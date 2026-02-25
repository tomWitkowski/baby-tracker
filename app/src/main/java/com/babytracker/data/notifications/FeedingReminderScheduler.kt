package com.babytracker.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.babytracker.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedingReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleIfEnabled() {
        if (!prefs.reminderEnabled.value) {
            cancel()
            return
        }
        val delayMs = (prefs.reminderDelayHours * 3600L + prefs.reminderDelayMinutes * 60L) * 1000L
        if (delayMs <= 0) return

        val pendingIntent = buildPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.cancel(pendingIntent)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMs,
            15 * 60 * 1000L,   // 15-minute delivery window — no special permission needed
            pendingIntent
        )
    }

    fun cancel() {
        val pendingIntent = buildPendingIntent(PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun buildPendingIntent(flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, FeedingReminderReceiver::class.java),
            flags
        )

    companion object {
        const val REQUEST_CODE = 1001
    }
}
