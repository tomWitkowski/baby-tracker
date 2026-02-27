package com.babytracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.babytracker.data.notifications.FeedingReminderReceiver
import com.babytracker.data.sync.SyncForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BabyTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        SyncForegroundService.start(this)
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            FeedingReminderReceiver.CHANNEL_ID,
            "Feeding Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you when it's time to feed the baby"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
