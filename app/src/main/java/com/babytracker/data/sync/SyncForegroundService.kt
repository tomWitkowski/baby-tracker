package com.babytracker.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.babytracker.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the SyncManager TCP server and NSD registration
 * alive even when the app is not in the foreground.
 *
 * Start this service from BabyTrackerApplication.onCreate() so it begins
 * immediately on app launch and survives activity/ViewModel lifecycle.
 */
@AndroidEntryPoint
class SyncForegroundService : Service() {

    @Inject lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        syncManager.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart if the system kills the service
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background Sync",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps sync active in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baby Tracker")
            .setContentText("Sync ready")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    companion object {
        private const val CHANNEL_ID = "sync_service_channel"
        private const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SyncForegroundService::class.java)
            )
        }
    }
}
