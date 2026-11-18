package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.PreferencesRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MorningQuoteWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext).getSnapshot()
        if (prefs.morningQuoteEnabled) {
            QuoteNotificationHelper.deliverQuoteNotification(applicationContext, isMorning = true)
            QuoteScheduler.scheduleNextMorning(applicationContext, prefs.morningHour, prefs.morningMinute)
        }
        return Result.success()
    }
}

class RandomDaytimeQuoteWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext).getSnapshot()
        if (prefs.randomQuotesEnabled) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour in 9..21) {
                QuoteNotificationHelper.deliverQuoteNotification(applicationContext, isMorning = false)
            }
            QuoteScheduler.scheduleNextRandomDaytime(applicationContext)
        }
        return Result.success()
    }
}

object QuoteScheduler {
    private const val WORK_MORNING = "vicdaru_morning_quote_work"
    private const val WORK_RANDOM = "vicdaru_random_quote_work"
    private const val WORK_FALLBACK_PERIODIC = "vicdaru_periodic_safety_work"

    fun syncSchedules(
        context: Context,
        morningEnabled: Boolean,
        morningHour: Int,
        morningMinute: Int,
        randomEnabled: Boolean,
    ) {
        QuoteNotificationHelper.ensureChannels(context)
        val wm = WorkManager.getInstance(context)
        if (morningEnabled) {
            scheduleNextMorning(context, morningHour, morningMinute)
        } else {
            wm.cancelUniqueWork(WORK_MORNING)
        }
        if (randomEnabled) {
            scheduleNextRandomDaytime(context)
            val periodic = PeriodicWorkRequestBuilder<RandomDaytimeQuoteWorker>(4, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(WORK_FALLBACK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)
        } else {
            wm.cancelUniqueWork(WORK_RANDOM)
            wm.cancelUniqueWork(WORK_FALLBACK_PERIODIC)
        }
    }

    fun scheduleNextMorning(context: Context, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val delayMs = (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
        val request = OneTimeWorkRequestBuilder<MorningQuoteWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_MORNING,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNextRandomDaytime(context: Context) {
        val delayMinutes = Random.nextLong(75L, 195L)
        val request = OneTimeWorkRequestBuilder<RandomDaytimeQuoteWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_RANDOM,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
