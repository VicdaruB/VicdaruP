package com.example.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.AnimeSeries
import com.example.model.QuoteHistoryEntry
import com.example.notifications.QuoteNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object QuoteShareHelper {
    suspend fun shareQuoteCard(context: Context, quote: QuoteHistoryEntry) {
        val shareFile = withContext(Dispatchers.IO) {
            val anime = AnimeSeries.fromKey(quote.animeKey)
            val artBitmap = QuoteNotificationHelper.downloadBitmap(quote.imageUrl)
            val cardBitmap = renderShareBitmap(quote, anime, artBitmap)

            val dir = File(context.cacheDir, "shared_quotes").apply { mkdirs() }
            val file = File(dir, "vicdaru_quote_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            cardBitmap.recycle()
            artBitmap?.recycle()
            file
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        val shareText = "“${quote.quoteText}” — ${quote.characterName} (${AnimeSeries.fromKey(quote.animeKey).displayName})\n#VicdaruProject"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartir frase en redes")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun downloadQuoteImage(context: Context, quote: QuoteHistoryEntry): String {
        val url = quote.imageUrl.ifBlank { AnimeSeries.fromKey(quote.animeKey).fallbackImageUrl }
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            val uri = android.net.Uri.parse(url)
            val ext = when {
                url.contains(".png", ignoreCase = true) -> "png"
                url.contains(".webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val safeAnime = quote.animeKey.lowercase()
            val fileName = "vicdaru_${safeAnime}_${System.currentTimeMillis()}.$ext"
            if (dm != null) {
                val request = android.app.DownloadManager.Request(uri)
                    .setTitle("Vicdaru • ${AnimeSeries.fromKey(quote.animeKey).displayName}")
                    .setDescription("Descargando ilustración de ${quote.characterName}")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                } else {
                    request.setDestinationInExternalFilesDir(context, android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                dm.enqueue(request)
                "Descarga iniciada: $fileName"
            } else {
                openImageLink(context, url)
                "Abriendo enlace de descarga..."
            }
        } catch (_: Exception) {
            openImageLink(context, url)
            "Abriendo enlace en el navegador..."
        }
    }

    fun openImageLink(context: Context, rawUrl: String) {
        if (rawUrl.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(rawUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun renderShareBitmap(
        quote: QuoteHistoryEntry,
        anime: AnimeSeries,
        art: Bitmap?,
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF0F172A.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Sky-blue to soft-pink glow border
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                0xFF7DD3FC.toInt(), 0xFFF9A8D4.toInt(), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 44f, 44f, glowPaint)

        // Anime artwork frame
        val artRect = RectF(84f, 96f, width - 84f, 690f)
        if (art != null) {
            val save = canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(artRect, 32f, 32f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val srcRect = Rect(0, 0, art.width, art.height)
            canvas.drawBitmap(art, srcRect, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(save)
        } else {
            val phPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    artRect.left, artRect.top, artRect.right, artRect.bottom,
                    0xFF7DD3FC.toInt(), 0xFFF9A8D4.toInt(), Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(artRect, 32f, 32f, phPaint)
        }

        // Tag pill
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7DD3FC.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${anime.displayName.uppercase()} • ${quote.moodTag.uppercase()}", 84f, 770f, tagPaint)

        // Quote text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val layout = StaticLayout.Builder.obtain(
            "“${quote.quoteText}”",
            0,
            quote.quoteText.length + 2,
            textPaint,
            width - 168
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1.1f)
            .build()
        canvas.save()
        canvas.translate(84f, 820f)
        layout.draw(canvas)
        canvas.restore()

        // Footer attribution
        val charPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF9A8D4.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("— ${quote.characterName}", 84f, height - 150f, charPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 28f
        }
        canvas.drawText("Vicdaru Project • Bienestar Anime", 84f, height - 92f, brandPaint)

        return bmp
    }
}
