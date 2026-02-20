package com.babytracker.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.repository.DayStats
import com.babytracker.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayStats by viewModel.dayStats.collectAsState()
    val dayEvents by viewModel.dayEvents.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportInProgress by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Wróć",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!exportInProgress) {
                                exportInProgress = true
                                coroutineScope.launch {
                                    try {
                                        val csv = viewModel.exportToCsv(context)
                                        shareCsv(context, csv)
                                    } finally {
                                        exportInProgress = false
                                    }
                                }
                            }
                        }
                    ) {
                        if (exportInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Eksport CSV",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date navigator
            item {
                DateNavigator(
                    selectedDate = selectedDate,
                    isToday = viewModel.isToday(),
                    onPrevious = { viewModel.goToPreviousDay() },
                    onNext = { viewModel.goToNextDay() }
                )
            }

            // Stats cards
            item {
                AnimatedContent(
                    targetState = dayStats,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "stats"
                ) { stats ->
                    if (stats != null) {
                        StatsSection(stats = stats)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FeedingColor)
                        }
                    }
                }
            }

            // Events header
            if (dayEvents.isNotEmpty()) {
                item {
                    Text(
                        "Zdarzenia tego dnia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(dayEvents, key = { it.id }) { event ->
                    DashboardEventRow(
                        event = event,
                        onDelete = { viewModel.deleteEvent(event) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Brak zdarzeń w tym dniu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DateNavigator(
    selectedDate: Long,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("pl"))
    val dateStr = sdf.format(Date(selectedDate)).replaceFirstChar { it.uppercase() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Poprzedni dzień", tint = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                if (isToday) {
                    Text(
                        "Dzisiaj",
                        style = MaterialTheme.typography.labelSmall,
                        color = FeedingColor
                    )
                }
            }
            IconButton(onClick = onNext, enabled = !isToday) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Następny dzień",
                    tint = if (isToday) TextHint else TextPrimary
                )
            }
        }
    }
}

@Composable
fun StatsSection(stats: DayStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Feeding card
        StatCard(
            title = "Karmienie",
            emoji = "\uD83C\uDF7C",
            color = FeedingColor,
            total = stats.totalFeedings,
            rows = buildList {
                if (stats.bottleFeedings > 0) add(
                    StatRow("Butelka", stats.bottleFeedings.toString(), BottleColor,
                        if (stats.totalMl > 0) "· ${stats.totalMl}ml" else null)
                )
                if (stats.naturalFeedings > 0) add(
                    StatRow("Naturalne", stats.naturalFeedings.toString(), NaturalColor, null)
                )
            }
        )

        // Diaper card
        StatCard(
            title = "Pieluszki",
            emoji = "\uD83D\uDCCD",
            color = DiaperColor,
            total = stats.totalDiapers,
            rows = buildList {
                if (stats.peeDiapers > 0) add(StatRow("Siku", stats.peeDiapers.toString(), PeeColor, null))
                if (stats.poopDiapers > 0) add(StatRow("Kupka", stats.poopDiapers.toString(), PoopColor, null))
                if (stats.mixedDiapers > 0) add(StatRow("Mieszane", stats.mixedDiapers.toString(), MixedColor, null))
            }
        )
    }
}

data class StatRow(val label: String, val value: String, val color: Color, val suffix: String?)

@Composable
fun StatCard(
    title: String,
    emoji: String,
    color: Color,
    total: Int,
    rows: List<StatRow>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = color
                ) {
                    Text(
                        total.toString(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (rows.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = color.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(row.color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                row.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (row.suffix != null) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    row.suffix,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (total == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Brak zdarzeń",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
        }
    }
}

@Composable
fun DashboardEventRow(
    event: BabyEvent,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFormat.format(Date(event.timestamp))

    val (emoji, label, color) = when {
        event.eventType == EventType.FEEDING.name && event.subType == FeedingSubType.BOTTLE.name ->
            Triple("\uD83C\uDF7C", "Butelka${event.milliliters?.let { " · ${it}ml" } ?: ""}", BottleColor)
        event.eventType == EventType.FEEDING.name ->
            Triple("\uD83E\uDD31", "Karmienie naturalne", NaturalColor)
        event.subType == DiaperSubType.PEE.name ->
            Triple("\uD83D\uDFE1", "Siku", PeeColor)
        event.subType == DiaperSubType.POOP.name ->
            Triple("\uD83D\uDFE4", "Kupka", PoopColor)
        else ->
            Triple("\uD83D\uDFE0", "Mieszane", MixedColor)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    time,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = TextHint)
            ) {
                Text("Usuń", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun shareCsv(context: Context, csvContent: String) {
    val file = File(context.cacheDir, "baby_tracker_export.csv")
    file.writeText(csvContent)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Baby Tracker - eksport danych")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Eksportuj dane"))
}
