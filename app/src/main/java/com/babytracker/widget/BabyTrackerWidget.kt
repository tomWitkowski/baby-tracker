package com.babytracker.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.babytracker.MainActivity
import com.babytracker.data.db.BabyDatabase
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val WIDGET_ACTION_KEY = ActionParameters.Key<String>("action")

class BabyTrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
fun WidgetContent() {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFAF8F5)))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetActionButton(
                    modifier = GlanceModifier.defaultWeight(),
                    emoji = "\uD83C\uDF7C",
                    label = "Karmienie",
                    bgColor = Color(0x26FF7B7B),
                    action = actionRunCallback<WidgetFeedingAction>(
                        actionParametersOf(WIDGET_ACTION_KEY to "feeding")
                    )
                )
                Spacer(GlanceModifier.width(12.dp))
                WidgetActionButton(
                    modifier = GlanceModifier.defaultWeight(),
                    emoji = "\uD83E\uDDF7",
                    label = "Pieluszka",
                    bgColor = Color(0x264ECDC4),
                    action = actionRunCallback<WidgetDiaperAction>(
                        actionParametersOf(WIDGET_ACTION_KEY to "diaper")
                    )
                )
            }
        }
    }
}

@Composable
fun WidgetActionButton(
    modifier: GlanceModifier = GlanceModifier,
    emoji: String,
    label: String,
    bgColor: Color,
    action: androidx.glance.action.Action
) {
    Column(
        modifier = modifier
            .clickable(action)
            .background(ColorProvider(bgColor))
            .cornerRadius(20.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = TextStyle(fontSize = 32.sp)
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color(0xFF2C2C2C))
            )
        )
    }
}

class WidgetFeedingAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            val db = androidx.room.Room.databaseBuilder(
                context,
                BabyDatabase::class.java,
                "baby_tracker_db"
            ).build()
            db.babyEventDao().insertEvent(
                BabyEvent(
                    eventType = EventType.FEEDING.name,
                    subType = FeedingSubType.BOTTLE.name
                )
            )
            db.close()
        }
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("WIDGET_ACTION", "feeding_logged")
            }
        )
    }
}

class WidgetDiaperAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            val db = androidx.room.Room.databaseBuilder(
                context,
                BabyDatabase::class.java,
                "baby_tracker_db"
            ).build()
            db.babyEventDao().insertEvent(
                BabyEvent(
                    eventType = EventType.DIAPER.name,
                    subType = DiaperSubType.PEE.name
                )
            )
            db.close()
        }
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("WIDGET_ACTION", "diaper_logged")
            }
        )
    }
}

class BabyTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BabyTrackerWidget()
}

class BabyTrackerWidgetActionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
