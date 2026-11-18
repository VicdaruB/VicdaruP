package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.HapticsHelper
import com.example.data.PreferencesRepository
import com.example.data.VicdaruRepository
import com.example.data.local.QuoteCacheRepository
import com.example.data.local.VicdaruLocalDatabase
import com.example.model.CuratedQuotesCatalog
import com.example.model.QuoteHistoryEntry
import com.example.network.AnimeImageFetcher
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object QuoteNotificationHelper {
    const val CHANNEL_MORNING = "vicdaru_morning_quotes"
    const val CHANNEL_DAYTIME = "vicdaru_daytime_quotes"

    private val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val morning = NotificationChannel(
                CHANNEL_MORNING,
                "Frase Inspiradora Matutina",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tu frase motivacional personalizada cada mañana con arte anime."
                enableVibration(true)
                vibrationPattern = HapticsHelper.MORNING_NOTIFICATION_TIMINGS
            }
            val daytime = NotificationChannel(
                CHANNEL_DAYTIME,
                "Recordatorios Productivos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Frases aleatorias antidepresivas durante el día productivo."
                enableVibration(true)
                vibrationPattern = HapticsHelper.DAYTIME_NOTIFICATION_TIMINGS
            }
            manager.createNotificationChannel(morning)
            manager.createNotificationChannel(daytime)
        }
    }

    suspend fun deliverQuoteNotification(context: Context, isMorning: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            ensureChannels(context)
            val prefsRepo = PreferencesRepository(context)
            val snapshot = prefsRepo.getSnapshot()

            val quote = if (isMorning) {
                CuratedQuotesCatalog.morningQuote()
            } else {
                CuratedQuotesCatalog.randomQuote(excludeText = snapshot.lastQuoteText)
            }

            val exclude = snapshot.usedImageUrls + setOfNotNull(snapshot.lastQuoteImageUrl.takeIf { it.isNotBlank() })
            val fetched = AnimeImageFetcher.fetchFreshImage(
                anime = quote.anime,
                excludeUrls = exclude,
                characterHint = quote.characterName
            )
            val imageUrl = fetched.imageUrl
            val bitmap = downloadBitmap(imageUrl)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.hero_cyber_zen_1794974224612)

            prefsRepo.saveLastDeliveredQuote(
                animeKey = quote.anime.key,
                quoteText = quote.text,
                characterName = quote.characterName,
                imageUrl = imageUrl
            )

            // Sync to Firestore history & local Room cache if signed in
            val uid = Firebase.auth.currentUser?.uid
            if (uid != null) {
                val sourceType = if (isMorning) "MORNING" else "RANDOM"
                val cacheRepo = QuoteCacheRepository(VicdaruLocalDatabase.getInstance(context).cachedQuoteDao())
                val remoteEntry = try {
                    val repo = VicdaruRepository(context)
                    repo.saveQuoteEntry(
                        quoteText = quote.text,
                        anime = quote.anime,
                        characterName = quote.characterName,
                        moodTag = quote.moodTag,
                        imageUrl = imageUrl,
                        imageSource = fetched.sourceLabel,
                        sourceType = sourceType
                    ).getOrNull()
                } catch (_: Exception) {
                    null
                }
                val now = Timestamp.now()
                if (remoteEntry != null) {
                    cacheRepo.cacheQuote(
                        userId = uid,
                        entry = remoteEntry.copy(createdAt = remoteEntry.createdAt ?: now, updatedAt = remoteEntry.updatedAt ?: now),
                        pendingUpload = false
                    )
                } else {
                    cacheRepo.cacheQuote(
                        userId = uid,
                        entry = QuoteHistoryEntry(
                            id = "local_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
                            userId = uid,
                            quoteText = quote.text,
                            animeKey = quote.anime.key,
                            characterName = quote.characterName,
                            moodTag = quote.moodTag,
                            imageUrl = imageUrl,
                            imageSource = fetched.sourceLabel,
                            favorite = false,
                            sourceType = sourceType,
                            createdAt = now,
                            updatedAt = now
                        ),
                        pendingUpload = true
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return@withContext false
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                if (isMorning) 101 else 202,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = if (isMorning) CHANNEL_MORNING else CHANNEL_DAYTIME
            val title = if (isMorning) {
                "Buenos días • ${quote.characterName} (${quote.anime.displayName})"
            } else {
                "${quote.characterName} • ${quote.anime.displayName}"
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(quote.text)
                .setSubText(quote.moodTag)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(
                    if (isMorning) HapticsHelper.MORNING_NOTIFICATION_TIMINGS
                    else HapticsHelper.DAYTIME_NOTIFICATION_TIMINGS
                )
                .setPriority(if (isMorning) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                        .setSummaryText(quote.text)
                )
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(quote.text))
            }

            val notificationId = if (isMorning) 4100 else (4200 + (System.currentTimeMillis() % 500).toInt())
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            val haptics = HapticsHelper(context)
            if (isMorning) {
                haptics.notificationMorning()
            } else {
                haptics.notificationDaytime()
            }
            true
        }

    suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val req = Request.Builder().url(url).build()
            imageHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                var sample = 1
                while (opts.outWidth / sample > 800 || opts.outHeight / sample > 800) {
                    sample *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }
}
