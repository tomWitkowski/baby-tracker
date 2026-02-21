package com.babytracker.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
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
import com.babytracker.ui.components.EditEventSheet
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
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedWeekStart by viewModel.selectedWeekStart.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportInProgress by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<BabyEvent?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row: Dzień / Tydzień
            TabRow(
                selectedTabIndex = if (viewMode == DashboardViewMode.DAY) 0 else 1,
                containerColor = BackgroundColor,
                contentColor = TextPrimary
            ) {
                Tab(
                    selected = viewMode == DashboardViewMode.DAY,
                    onClick = { viewModel.setViewMode(DashboardViewMode.DAY) },
                    text = {
                        Text(
                            "Dzień",
                            fontWeight = if (viewMode == DashboardViewMode.DAY) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = viewMode == DashboardViewMode.WEEK,
                    onClick = { viewModel.setViewMode(DashboardViewMode.WEEK) },
                    text = {
                        Text(
                            "Tydzień",
                            fontWeight = if (viewMode == DashboardViewMode.WEEK) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }

            // Content
            if (viewMode == DashboardViewMode.DAY) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

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
                                onDelete = { viewModel.deleteEvent(event) },
                                onEdit = { editingEvent = event }
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
            } else {
                WeeklyView(
                    weeklyStats = weeklyStats,
                    selectedWeekStart = selectedWeekStart,
                    isCurrentWeek = viewModel.isCurrentWeek(),
                    onPrevious = { viewModel.goToPreviousWeek() },
                    onNext = { viewModel.goToNextWeek() }
                )
            }
        }
    }

    // Edit sheet overlay
    AnimatedVisibility(
        visible = editingEvent != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { editingEvent = null }
        )
    }
    AnimatedVisibility(
        visible = editingEvent != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        editingEvent?.let { event ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SurfaceColor,
                tonalElevation = 4.dp
            ) {
                EditEventSheet(
                    event = event,
                    onDismiss = { editingEvent = null },
                    onSave = { updatedEvent ->
                        viewModel.updateEvent(updatedEvent)
                        editingEvent = null
                    }
                )
            }
        }
    }
    } // end Box
}

// ── Weekly view ───────────────────────────────────────────────────────────────

@Composable
fun WeeklyView(
    weeklyStats: List<DayStats>?,
    selectedWeekStart: Long,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (weeklyStats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FeedingColor)
        }
        return
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val totalFeedings = weeklyStats.sumOf { it.totalFeedings }
    val totalDiapers = weeklyStats.sumOf { it.totalDiapers }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            WeekNavigator(
                weekStart = selectedWeekStart,
                isCurrentWeek = isCurrentWeek,
                onPrevious = onPrevious,
                onNext = onNext
            )
        }

        item {
            WeeklyTotalsCard(totalFeedings = totalFeedings, totalDiapers = totalDiapers)
        }

        item {
            Text(
                "Dzień po dniu",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(weeklyStats) { dayStats ->
            WeekDayRow(
                dayStats = dayStats,
                isToday = dayStats.date == todayMidnight
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun WeekNavigator(
    weekStart: Long,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val weekEnd = weekStart + 6 * 86_400_000L
    val sdf = SimpleDateFormat("d MMM", Locale("pl"))
    val weekLabel = "${sdf.format(Date(weekStart))} – ${sdf.format(Date(weekEnd))}"

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
                Icon(Icons.Default.ChevronLeft, contentDescription = "Poprzedni tydzień", tint = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    weekLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                if (isCurrentWeek) {
                    Text(
                        "Ten tydzień",
                        style = MaterialTheme.typography.labelSmall,
                        color = FeedingColor
                    )
                }
            }
            IconButton(onClick = onNext, enabled = !isCurrentWeek) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Następny tydzień",
                    tint = if (isCurrentWeek) TextHint else TextPrimary
                )
            }
        }
    }
}

@Composable
fun WeeklyTotalsCard(totalFeedings: Int, totalDiapers: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍼", fontSize = 26.sp)
                Text(
                    totalFeedings.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FeedingColor
                )
                Text("karmień", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(TextHint.copy(alpha = 0.25f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📌", fontSize = 26.sp)
                Text(
                    totalDiapers.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DiaperColor
                )
                Text("pieluszek", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun WeekDayRow(dayStats: DayStats, isToday: Boolean) {
    val cal = Calendar.getInstance().apply { timeInMillis = dayStats.date }
    val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Pon"
        Calendar.TUESDAY -> "Wt"
        Calendar.WEDNESDAY -> "Śr"
        Calendar.THURSDAY -> "Czw"
        Calendar.FRIDAY -> "Pt"
        Calendar.SATURDAY -> "Sob"
        else -> "Ndz"
    }
    val dayDate = SimpleDateFormat("d MMM", Locale("pl")).format(Date(dayStats.date))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isToday) FeedingColor.copy(alpha = 0.09f) else SurfaceColor,
        tonalElevation = if (isToday) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.width(40.dp)) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) FeedingColor else TextPrimary
                    )
                    Text(
                        dayDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = FeedingColor
                    ) {
                        Text(
                            "Dziś",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🍼", fontSize = 14.sp)
                    Text(
                        dayStats.totalFeedings.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayStats.totalFeedings > 0) FeedingColor else TextHint
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📌", fontSize = 14.sp)
                    Text(
                        dayStats.totalDiapers.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayStats.totalDiapers > 0) DiaperColor else TextHint
                    )
                }
            }
        }
    }
}

// ── Day view composables ──────────────────────────────────────────────────────

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
                if (stats.breastFeedings > 0) add(
                    StatRow("Pierś", stats.breastFeedings.toString(), NaturalColor, null)
                )
                if (stats.pumpFeedings > 0) add(
                    StatRow("Laktator", stats.pumpFeedings.toString(), PumpColor,
                        if (stats.totalPumpMl > 0) "· ${stats.totalPumpMl}ml" else null)
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
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFormat.format(Date(event.timestamp))

    val (emoji, label, color) = com.babytracker.ui.main.eventDisplayInfo(event)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceColor,
        tonalElevation = 1.dp,
        onClick = onEdit
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
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edytuj",
                    tint = TextHint,
                    modifier = Modifier.size(16.dp)
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
