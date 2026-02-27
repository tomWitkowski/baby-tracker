package com.babytracker.data.sync

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.babytracker.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that owns the SyncManager lifecycle.
 *
 * Key responsibilities:
 * 1. Keep the TCP server + NSD + UDP discovery alive when the app is not open.
 * 2. Hold a WifiManager.WifiLock to prevent WiFi from sleeping and dropping
 *    the server socket / UDP listener.
 * 3. Hold a WifiManager.MulticastLock so the WiFi driver forwards broadcast UDP
 *    packets to the app — without this, UDP discovery silently receives nothing.
 * 4. Reschedule itself via AlarmManager if the user swipes the app away, so
 *    sync stays available even after task removal (START_STICKY alone is not
 *    reliable on all OEM ROMs).
 */
@AndroidEntryPoint
class SyncForegroundService : Service() {

    @Inject lateinit var syncManager: SyncManager

    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        acquireWifiLocks()
        syncManager.start()
        Log.d(TAG, "SyncForegroundService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: Android restarts the service after killing it to reclaim memory.
        // We call syncManager.start() here too so that when Android restarts the service
        // after a kill (not a user-swipe), sockets are re-initialized.
        syncManager.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // User swiped the app away. On many Android OEM ROMs, START_STICKY is ignored
        // after task removal. Schedule an alarm 3 seconds from now to restart the service.
        val restartIntent = Intent(applicationContext, SyncForegroundService::class.java)
        val pi = PendingIntent.getService(
            applicationContext,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(AlarmManager::class.java)
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 3_000L,
            pi
        )
        Log.d(TAG, "Task removed — scheduled restart in 3 s")
    }

    override fun onDestroy() {
        releaseWifiLocks()
        super.onDestroy()
        Log.d(TAG, "SyncForegroundService destroyed")
    }

    // ── WiFi locks ────────────────────────────────────────────────────────────

    private fun acquireWifiLocks() {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // WifiLock: prevents the WiFi chipset from entering low-power mode which
        // would drop the server socket and UDP listener while the screen is off.
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BabyTrackerWifi")
            .apply {
                setReferenceCounted(false)
                if (!isHeld) acquire()
            }

        // MulticastLock: by default Android's WiFi driver filters out broadcast and
        // multicast packets to save power. Without this lock, the UDP discovery
        // listener receives NOTHING — this is the most common reason UDP sync fails.
        multicastLock = wifi.createMulticastLock("BabyTrackerMulticast")
            .apply {
                setReferenceCounted(false)
                if (!isHeld) acquire()
            }

        Log.d(TAG, "WiFi locks acquired (wifiLock=${wifiLock?.isHeld}, multicastLock=${multicastLock?.isHeld})")
    }

    private fun releaseWifiLocks() {
        if (wifiLock?.isHeld == true) wifiLock?.release()
        if (multicastLock?.isHeld == true) multicastLock?.release()
        Log.d(TAG, "WiFi locks released")
    }

    // ── Notification ─────────────────────────────────────────────────────────

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

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "SyncForegroundService"
        private const val CHANNEL_ID = "sync_service_channel"
        private const val NOTIFICATION_ID = 9001
        private const val RESTART_REQUEST_CODE = 9002

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SyncForegroundService::class.java)
            )
        }
    }
}
