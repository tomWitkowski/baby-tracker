package com.babytracker.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.babytracker.R
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.ui.i18n.stringsForLang
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FeedingReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val strings = stringsForLang(prefs.language.value)
        val hours = prefs.reminderDelayHours
        val minutes = prefs.reminderDelayMinutes
        val body = String.format(strings.reminderNotifBody, hours, minutes)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.reminderNotifTitle)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "feeding_reminder"
        const val NOTIF_ID = 1001
    }
}
