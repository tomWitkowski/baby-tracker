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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Settings
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
import com.babytracker.data.repository.DayStats
import com.babytracker.ui.components.EditEventSheet
import com.babytracker.ui.i18n.LocalStrings
import com.babytracker.ui.main.eventDisplayInfo
import com.babytracker.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
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
                            strings.dashboard,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = strings.back,
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = strings.settings,
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (!exportInProgress) {
                                    exportInProgress = true
                                    coroutineScope.launch {
                                        try {
                                            val csv = viewModel.exportToCsv()
                                            shareCsv(context, csv)
                                        } finally {
                                            exportInProgress = false
                                        }
                                    }
                                }
                            }
                        ) {
                            if (exportInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = strings.exportCsv,
                                    tint = TextPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(
                    selectedTabIndex = when (viewMode) {
                        DashboardViewMode.DAY -> 0; DashboardViewMode.WEEK -> 1; DashboardViewMode.TIMELINE -> 2
                    },
                    containerColor = BackgroundColor,
                    contentColor = TextPrimary
                ) {
                    Tab(
                        selected = viewMode == DashboardViewMode.DAY,
                        onClick = { viewModel.setViewMode(DashboardViewMode.DAY) },
                        text = {
                            Text(strings.day, fontWeight = if (viewMode == DashboardViewMode.DAY) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    )
                    Tab(
                        selected = viewMode == DashboardViewMode.WEEK,
                        onClick = { viewModel.setViewMode(DashboardViewMode.WEEK) },
                        text = {
                            Text(strings.week, fontWeight = if (viewMode == DashboardViewMode.WEEK) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    )
                    Tab(
                        selected = viewMode == DashboardViewMode.TIMELINE,
                        onClick = { viewModel.setViewMode(DashboardViewMode.TIMELINE) },
                        text = {
                            Text(strings.timeline, fontWeight = if (viewMode == DashboardViewMode.TIMELINE) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    )
                }

                if (viewMode == DashboardViewMode.DAY) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        item {
                            DateNavigator(
                                selectedDate = selectedDate,
                                isToday = viewModel.isToday(),
                                onPrevious = { viewModel.goToPreviousDay() },
                                onNext = { viewModel.goToNextDay() }
                            )
                        }
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
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = FeedingColor)
                                    }
                                }
                            }
                        }
                        if (dayEvents.isNotEmpty()) {
                            item {
                                Text(
                                    strings.eventsThisDay,
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
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        strings.noEventsThisDay,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextHint,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                } else if (viewMode == DashboardViewMode.WEEK) {
                    WeeklyView(
                        weeklyStats = weeklyStats,
                        selectedWeekStart = selectedWeekStart,
                        isCurrentWeek = viewModel.isCurrentWeek(),
                        onPrevious = { viewModel.goToPreviousWeek() },
                        onNext = { viewModel.goToNextWeek() }
                    )
                } else {
                    TimelineView(
                        dayEvents = dayEvents,
                        selectedDate = selectedDate,
                        isToday = viewModel.isToday(),
                        onPrevious = { viewModel.goToPreviousDay() },
                        onNext = { viewModel.goToNextDay() },
                        onEdit = { editingEvent = it }
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
    }
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
    val strings = LocalStrings.current
    if (weeklyStats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FeedingColor)
        }
        return
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val totalFeedings = weeklyStats.sumOf { it.totalFeedings }
    val totalDiapers = weeklyStats.sumOf { it.totalDiapers }
    val totalSpitUps = weeklyStats.sumOf { it.spitUpCount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
            WeeklyTotalsCard(
                totalFeedings = totalFeedings,
                totalDiapers = totalDiapers,
                totalSpitUps = totalSpitUps
            )
        }
        item {
            Text(
                strings.dayByDay,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(weeklyStats) { dayStats ->
            WeekDayRow(dayStats = dayStats, isToday = dayStats.date == todayMidnight)
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
    val strings = LocalStrings.current
    val weekEnd = weekStart + 6 * 86_400_000L
    val sdf = SimpleDateFormat("d MMM", strings.locale)
    val weekLabel = "${sdf.format(Date(weekStart))} – ${sdf.format(Date(weekEnd))}"

    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceColor, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextPrimary)
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
                    Text(strings.thisWeek, style = MaterialTheme.typography.labelSmall, color = FeedingColor)
                }
            }
            IconButton(onClick = onNext, enabled = !isCurrentWeek) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isCurrentWeek) TextHint else TextPrimary
                )
            }
        }
    }
}

@Composable
fun WeeklyTotalsCard(totalFeedings: Int, totalDiapers: Int, totalSpitUps: Int) {
    val strings = LocalStrings.current
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceColor, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDF7C", fontSize = 26.sp)
                Text(
                    totalFeedings.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FeedingColor
                )
                Text(strings.feedingsUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Box(modifier = Modifier.width(1.dp).height(60.dp).background(TextHint.copy(alpha = 0.25f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83E\uDE72", fontSize = 26.sp)
                Text(
                    totalDiapers.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DiaperColor
                )
                Text(strings.diapersUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (totalSpitUps > 0) {
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(TextHint.copy(alpha = 0.25f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u21A9\uFE0F", fontSize = 26.sp)
                    Text(
                        totalSpitUps.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpitUpColor
                    )
                    Text(strings.spitUpsUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun WeekDayRow(dayStats: DayStats, isToday: Boolean) {
    val strings = LocalStrings.current
    val cal = Calendar.getInstance().apply { timeInMillis = dayStats.date }
    val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> strings.mon
        Calendar.TUESDAY -> strings.tue
        Calendar.WEDNESDAY -> strings.wed
        Calendar.THURSDAY -> strings.thu
        Calendar.FRIDAY -> strings.fri
        Calendar.SATURDAY -> strings.sat
        else -> strings.sun
    }
    val dayDate = SimpleDateFormat("d MMM", strings.locale).format(Date(dayStats.date))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isToday) FeedingColor.copy(alpha = 0.09f) else SurfaceColor,
        tonalElevation = if (isToday) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.width(40.dp)) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) FeedingColor else TextPrimary
                    )
                    Text(dayDate, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                if (isToday) {
                    Surface(shape = RoundedCornerShape(4.dp), color = FeedingColor) {
                        Text(
                            strings.todayLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\uD83C\uDF7C", fontSize = 14.sp)
                    Text(
                        dayStats.totalFeedings.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayStats.totalFeedings > 0) FeedingColor else TextHint
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\uD83E\uDE72", fontSize = 14.sp)
                    Text(
                        dayStats.totalDiapers.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayStats.totalDiapers > 0) DiaperColor else TextHint
                    )
                }
                if (dayStats.spitUpCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("\u21A9\uFE0F", fontSize = 14.sp)
                        Text(
                            dayStats.spitUpCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SpitUpColor
                        )
                    }
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
    val strings = LocalStrings.current
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", strings.locale)
    val dateStr = sdf.format(Date(selectedDate)).replaceFirstChar { it.uppercase() }

    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceColor, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextPrimary)
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
                    Text(strings.todayLong, style = MaterialTheme.typography.labelSmall, color = FeedingColor)
                }
            }
            IconButton(onClick = onNext, enabled = !isToday) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isToday) TextHint else TextPrimary
                )
            }
        }
    }
}

@Composable
fun StatsSection(stats: DayStats) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = strings.feedingLabel,
            emoji = "\uD83C\uDF7C",
            color = FeedingColor,
            total = stats.totalFeedings,
            rows = buildList {
                if (stats.bottleFeedings > 0) add(
                    StatRow(strings.bottleLabel, stats.bottleFeedings.toString(), BottleColor,
                        if (stats.totalMl > 0) "· ${stats.totalMl}ml" else null)
                )
                if (stats.breastFeedings > 0) add(
                    StatRow(strings.breastLabel, stats.breastFeedings.toString(), NaturalColor, null)
                )
                if (stats.pumpFeedings > 0) add(
                    StatRow(strings.pumpLabel, stats.pumpFeedings.toString(), PumpColor,
                        if (stats.totalPumpMl > 0) "· ${stats.totalPumpMl}ml" else null)
                )
            }
        )
        StatCard(
            title = strings.diapersLabel,
            emoji = "\uD83E\uDE72",
            color = DiaperColor,
            total = stats.totalDiapers,
            rows = buildList {
                if (stats.peeDiapers > 0) add(StatRow(strings.peeLabel, stats.peeDiapers.toString(), PeeColor, null))
                if (stats.poopDiapers > 0) add(StatRow(strings.poopLabel, stats.poopDiapers.toString(), PoopColor, null))
                if (stats.mixedDiapers > 0) add(StatRow(strings.mixedLabel, stats.mixedDiapers.toString(), MixedColor, null))
            }
        )
        if (stats.spitUpCount > 0) {
            StatCard(
                title = strings.spitUpLabel,
                emoji = "\u21A9\uFE0F",
                color = SpitUpColor,
                total = stats.spitUpCount,
                rows = emptyList()
            )
        }
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
                Surface(shape = CircleShape, color = color) {
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(row.color))
                            Spacer(Modifier.width(8.dp))
                            Text(row.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                                Text(row.suffix, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            val strings = LocalStrings.current
            if (total == 0) {
                Spacer(Modifier.height(8.dp))
                Text(strings.noEvents, style = MaterialTheme.typography.bodySmall, color = TextHint)
            }
        }
    }
}

// ── Dashboard event row ───────────────────────────────────────────────────────

@Composable
fun DashboardEventRow(
    event: BabyEvent,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val timeFormat = SimpleDateFormat("HH:mm", strings.locale)
    val time = timeFormat.format(Date(event.timestamp))
    val (emoji, label, color) = eventDisplayInfo(event, strings)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceColor,
        tonalElevation = 1.dp,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(time, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = TextHint, modifier = Modifier.size(16.dp))
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = TextHint)
            ) {
                Text(strings.deleteButton, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ── Timeline vertical view ────────────────────────────────────────────────────

@Composable
fun TimelineView(
    dayEvents: List<com.babytracker.data.db.entity.BabyEvent>,
    selectedDate: Long,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEdit: (com.babytracker.data.db.entity.BabyEvent) -> Unit
) {
    val strings = LocalStrings.current
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", strings.locale)
    val dateStr = sdf.format(Date(selectedDate)).replaceFirstChar { it.uppercase() }
    val hourFmt = SimpleDateFormat("HH:mm", strings.locale)

    // Sort events chronologically and group by hour
    val sortedEvents = remember(dayEvents) { dayEvents.sortedBy { it.timestamp } }

    // Build list items: hour headers + events
    data class HourHeader(val hour: Int)
    data class EventItem(val event: com.babytracker.data.db.entity.BabyEvent)

    val listItems: List<Any> = remember(sortedEvents) {
        buildList {
            var lastHour = -1
            sortedEvents.forEach { event ->
                val cal = Calendar.getInstance().apply { timeInMillis = event.timestamp }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour != lastHour) {
                    add(HourHeader(hour))
                    lastHour = hour
                }
                add(EventItem(event))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(4.dp))

        // Date navigator
        Surface(
            shape = RoundedCornerShape(20.dp), color = SurfaceColor, tonalElevation = 2.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dateStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
                    if (isToday) Text(strings.todayLong, style = MaterialTheme.typography.labelSmall, color = FeedingColor)
                }
                IconButton(onClick = onNext, enabled = !isToday) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (isToday) TextHint else TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (sortedEvents.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                Text(strings.noEventsThisDay, style = MaterialTheme.typography.bodyLarge, color = TextHint)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(listItems) { item ->
                    when (item) {
                        is HourHeader -> {
                            // Hour separator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "%02d:00".format(item.hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextHint,
                                    modifier = Modifier.width(44.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = DividerColor,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                        is EventItem -> {
                            val event = item.event
                            val timeStr = hourFmt.format(Date(event.timestamp))
                            val (emoji, label, dotColor) = com.babytracker.ui.main.eventDisplayInfo(event, strings)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 44.dp, bottom = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(dotColor.copy(alpha = 0.07f))
                                    .clickable { onEdit(event) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Timeline dot
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(dotColor.copy(alpha = 0.15f))
                                        .border(1.5.dp, dotColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextHint, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── CSV export helper ─────────────────────────────────────────────────────────

fun shareCsv(context: Context, csvContent: String) {
    val file = File(context.cacheDir, "baby_tracker_export.csv")
    file.writeText(csvContent)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Baby Tracker - export")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV"))
}
