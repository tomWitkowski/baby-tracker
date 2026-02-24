package com.babytracker.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.sync.SyncState
import com.babytracker.ui.components.EditEventSheet
import com.babytracker.ui.i18n.AppStrings
import com.babytracker.ui.i18n.LocalStrings
import com.babytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

sealed class BottomSheetState {
    object Hidden : BottomSheetState()
    object FeedingOptions : BottomSheetState()
    object BreastSideOptions : BottomSheetState()
    object PumpSideOptions : BottomSheetState()
    object DiaperOptions : BottomSheetState()
    object BottleMlInput : BottomSheetState()
    object PumpMlInput : BottomSheetState()
    data class Editing(val event: BabyEvent) : BottomSheetState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val recentEvents by viewModel.recentEvents.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val babyName by viewModel.babyName.collectAsState()
    var bottomSheetState by remember { mutableStateOf<BottomSheetState>(BottomSheetState.Hidden) }
    var mlInput by remember { mutableStateOf("") }
    var pumpMlInput by remember { mutableStateOf("") }
    var pumpSelectedSubType by remember { mutableStateOf(FeedingSubType.PUMP_LEFT) }
    var showSuccessBadge by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(syncState) {
        when (val s = syncState) {
            is SyncState.Success -> showSuccessBadge =
                if (s.added > 0) "${strings.syncSuccessPrefix}${s.added}" else strings.syncAlreadySynced
            is SyncState.NoDeviceFound -> showSuccessBadge = strings.syncNoDevice
            is SyncState.Error -> showSuccessBadge = strings.syncError
            else -> {}
        }
    }

    LaunchedEffect(showSuccessBadge) {
        if (showSuccessBadge != null) {
            kotlinx.coroutines.delay(1500)
            showSuccessBadge = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = babyName.ifEmpty { strings.defaultBabyTitle },
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getTodayString(strings.locale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sync button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .clickable {
                                if (syncState is SyncState.Idle || syncState is SyncState.NoDeviceFound || syncState is SyncState.Error) {
                                    viewModel.syncNow()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (syncState) {
                            is SyncState.Searching, is SyncState.Syncing ->
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = TextSecondary
                                )
                            is SyncState.NoDeviceFound, is SyncState.Error ->
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = strings.syncButton,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            else ->
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = strings.syncButton,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                        }
                    }
                    // Settings button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // Dashboard button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .clickable { onNavigateToDashboard() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = strings.dashboard,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ActionDot(
                    modifier = Modifier.weight(1f),
                    color = FeedingColor,
                    lightColor = FeedingColorLight,
                    emoji = "\uD83C\uDF7C",
                    label = strings.feeding,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        bottomSheetState = BottomSheetState.FeedingOptions
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        mlInput = ""
                        bottomSheetState = BottomSheetState.BottleMlInput
                    }
                )
                ActionDot(
                    modifier = Modifier.weight(1f),
                    color = DiaperColor,
                    lightColor = DiaperColorLight,
                    emoji = "\uD83D\uDCCD",
                    label = strings.diaper,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        bottomSheetState = BottomSheetState.DiaperOptions
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spit-up quick button
            Surface(
                onClick = {
                    viewModel.logSpitUp()
                    showSuccessBadge = strings.spitUpSaved
                },
                shape = RoundedCornerShape(20.dp),
                color = SpitUpColor.copy(alpha = 0.10f),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("\u21A9\uFE0F", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        strings.spitUp,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SpitUpColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hint text
            Text(
                text = strings.holdForMl,
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Recent events
            if (recentEvents.isNotEmpty()) {
                Text(
                    text = strings.recentActivity,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentEvents, key = { it.id }) { event ->
                        EventRow(
                            event = event,
                            strings = strings,
                            onDelete = { viewModel.deleteEvent(event) },
                            onEdit = { bottomSheetState = BottomSheetState.Editing(event) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = strings.noEventsHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }

        // Success toast badge
        AnimatedVisibility(
            visible = showSuccessBadge != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                color = Color(0xFF2D2D2D),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Text(
                    text = showSuccessBadge ?: "",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        // Dimmed overlay
        AnimatedVisibility(
            visible = bottomSheetState != BottomSheetState.Hidden,
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
                    ) { bottomSheetState = BottomSheetState.Hidden }
            )
        }

        // Bottom sheet
        AnimatedVisibility(
            visible = bottomSheetState != BottomSheetState.Hidden,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SurfaceColor,
                tonalElevation = 4.dp
            ) {
                when (val state = bottomSheetState) {
                    BottomSheetState.FeedingOptions -> FeedingSheet(
                        onBottle = {
                            viewModel.logBottleFeeding(null)
                            showSuccessBadge = strings.bottleSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onBottleWithMl = {
                            mlInput = ""
                            bottomSheetState = BottomSheetState.BottleMlInput
                        },
                        onBreast = { bottomSheetState = BottomSheetState.BreastSideOptions },
                        onPump = { bottomSheetState = BottomSheetState.PumpSideOptions },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.BreastSideOptions -> BreastSideSheet(
                        onSide = { subType ->
                            viewModel.logBreastFeeding(subType)
                            showSuccessBadge = strings.breastSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.PumpSideOptions -> PumpSideSheet(
                        onSide = { subType ->
                            pumpSelectedSubType = subType
                            pumpMlInput = ""
                            bottomSheetState = BottomSheetState.PumpMlInput
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.PumpMlInput -> PumpMlSheet(
                        mlInput = pumpMlInput,
                        onMlChange = { pumpMlInput = it },
                        onConfirm = {
                            val ml = pumpMlInput.toIntOrNull()
                            viewModel.logPump(pumpSelectedSubType, ml)
                            showSuccessBadge = if (ml != null)
                                String.format(strings.pumpMlSavedFmt, ml)
                            else strings.pumpSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.DiaperOptions -> DiaperSheet(
                        onPee = {
                            viewModel.logDiaper(DiaperSubType.PEE)
                            showSuccessBadge = strings.peeSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onPoop = {
                            viewModel.logDiaper(DiaperSubType.POOP)
                            showSuccessBadge = strings.poopSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onMixed = {
                            viewModel.logDiaper(DiaperSubType.MIXED)
                            showSuccessBadge = strings.mixedSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.BottleMlInput -> BottleMlSheet(
                        mlInput = mlInput,
                        onMlChange = { mlInput = it },
                        onConfirm = {
                            val ml = mlInput.toIntOrNull()
                            viewModel.logBottleFeeding(ml)
                            showSuccessBadge = if (ml != null)
                                String.format(strings.bottleMlSavedFmt, ml)
                            else strings.bottleSaved
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    is BottomSheetState.Editing -> EditEventSheet(
                        event = state.event,
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden },
                        onSave = { updatedEvent ->
                            viewModel.updateEvent(updatedEvent)
                            bottomSheetState = BottomSheetState.Hidden
                        }
                    )
                    else -> {}
                }
            }
        }
    }
}

// ── Action dot ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionDot(
    modifier: Modifier = Modifier,
    color: Color,
    lightColor: Color,
    emoji: String,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .scale(scale)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = color.copy(alpha = 0.3f),
                    spotColor = color.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(brush = Brush.radialGradient(colors = listOf(lightColor, color)))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 52.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Feeding sheet ─────────────────────────────────────────────────────────────

@Composable
fun FeedingSheet(
    onBottle: () -> Unit,
    onBottleWithMl: () -> Unit,
    onBreast: () -> Unit,
    onPump: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            strings.feeding,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83C\uDF7C",
                title = strings.bottle,
                subtitle = strings.quickSave,
                color = BottleColor,
                onClick = onBottle
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83C\uDF7C",
                title = strings.bottleWithMl,
                subtitle = strings.chooseSide,
                color = BottleColor,
                onClick = onBottleWithMl
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83E\uDD31",
                title = strings.breastFeeding,
                subtitle = strings.chooseSide,
                color = NaturalColor,
                onClick = onBreast
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83E\uDED7",
                title = strings.pump,
                subtitle = strings.chooseSide,
                color = PumpColor,
                onClick = onPump
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Breast side sheet ─────────────────────────────────────────────────────────

@Composable
fun BreastSideSheet(
    onSide: (FeedingSubType) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "\uD83E\uDD31 ${strings.breastFeeding}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2B05\uFE0F",
                title = strings.breastLeft,
                subtitle = "",
                color = NaturalColor,
                onClick = { onSide(FeedingSubType.BREAST_LEFT) }
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u27A1\uFE0F",
                title = strings.breastRight,
                subtitle = "",
                color = NaturalColor,
                onClick = { onSide(FeedingSubType.BREAST_RIGHT) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2194\uFE0F",
                title = strings.breastBothLR,
                subtitle = strings.startedLeft,
                color = NaturalColor,
                onClick = { onSide(FeedingSubType.BREAST_BOTH_LR) }
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2194\uFE0F",
                title = strings.breastBothRL,
                subtitle = strings.startedRight,
                color = NaturalColor,
                onClick = { onSide(FeedingSubType.BREAST_BOTH_RL) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Pump side sheet ───────────────────────────────────────────────────────────

@Composable
fun PumpSideSheet(
    onSide: (FeedingSubType) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "\uD83E\uDED7 ${strings.pumpSideTitle}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2B05\uFE0F",
                title = strings.breastLeft,
                subtitle = "",
                color = PumpColor,
                onClick = { onSide(FeedingSubType.PUMP_LEFT) }
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u27A1\uFE0F",
                title = strings.breastRight,
                subtitle = "",
                color = PumpColor,
                onClick = { onSide(FeedingSubType.PUMP_RIGHT) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2194\uFE0F",
                title = strings.breastBothLR,
                subtitle = strings.startedLeft,
                color = PumpColor,
                onClick = { onSide(FeedingSubType.PUMP_BOTH_LR) }
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\u2194\uFE0F",
                title = strings.breastBothRL,
                subtitle = strings.startedRight,
                color = PumpColor,
                onClick = { onSide(FeedingSubType.PUMP_BOTH_RL) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Pump ml sheet ─────────────────────────────────────────────────────────────

@Composable
fun PumpMlSheet(
    mlInput: String,
    onMlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "\uD83E\uDED7 ${strings.pump}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(strings.howManyMl, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = mlInput,
            onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) onMlChange(v) },
            label = { Text(strings.amountLabel) },
            placeholder = { Text(strings.exampleMlSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PumpColor,
                focusedLabelColor = PumpColor
            ),
            suffix = { Text("ml", color = TextSecondary) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PumpColor)
        ) {
            Text(
                if (mlInput.isEmpty()) strings.saveWithoutAmount
                else String.format(strings.saveMlFormat, mlInput.toInt()),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Diaper sheet ──────────────────────────────────────────────────────────────

@Composable
fun DiaperSheet(
    onPee: () -> Unit,
    onPoop: () -> Unit,
    onMixed: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            strings.diaper,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83D\uDFE1",
                title = strings.pee,
                subtitle = strings.wetDiaper,
                color = PeeColor,
                onClick = onPee
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83D\uDFE4",
                title = strings.poop,
                subtitle = "",
                color = PoopColor,
                onClick = onPoop
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OptionCard(
            modifier = Modifier.fillMaxWidth(),
            emoji = "\uD83D\uDFE0",
            title = strings.mixed,
            subtitle = strings.mixedDesc,
            color = MixedColor,
            onClick = onMixed
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Bottle ml sheet ───────────────────────────────────────────────────────────

@Composable
fun BottleMlSheet(
    mlInput: String,
    onMlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "\uD83C\uDF7C ${strings.bottle}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(strings.howManyMl, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = mlInput,
            onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) onMlChange(v) },
            label = { Text(strings.amountLabel) },
            placeholder = { Text(strings.exampleMlLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BottleColor,
                focusedLabelColor = BottleColor
            ),
            suffix = { Text("ml", color = TextSecondary) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BottleColor)
        ) {
            Text(
                if (mlInput.isEmpty()) strings.saveWithoutAmount
                else String.format(strings.saveMlFormat, mlInput.toInt()),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Option card ───────────────────────────────────────────────────────────────

@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        onClick = onClick,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ── Event row ─────────────────────────────────────────────────────────────────

@Composable
fun EventRow(
    event: BabyEvent,
    strings: AppStrings,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("HH:mm", strings.locale)
    val time = timeFormat.format(Date(event.timestamp))
    val (emoji, label, color) = eventDisplayInfo(event, strings)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
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
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Event display info ────────────────────────────────────────────────────────

fun eventDisplayInfo(event: BabyEvent, strings: AppStrings): Triple<String, String, Color> {
    val mlSuffix = event.milliliters?.let { " · ${it}ml" } ?: ""
    return when {
        event.eventType == EventType.SPIT_UP.name ->
            Triple("\u21A9\uFE0F", strings.spitUp, SpitUpColor)
        event.eventType == EventType.FEEDING.name -> when (event.subType) {
            FeedingSubType.BOTTLE.name ->
                Triple("\uD83C\uDF7C", "${strings.bottle}$mlSuffix", BottleColor)
            FeedingSubType.BREAST_LEFT.name ->
                Triple("\uD83E\uDD31", strings.breastLeft, NaturalColor)
            FeedingSubType.BREAST_RIGHT.name ->
                Triple("\uD83E\uDD31", strings.breastRight, NaturalColor)
            FeedingSubType.BREAST_BOTH_LR.name ->
                Triple("\uD83E\uDD31", "${strings.breastLeft}\u2192${strings.breastRight}", NaturalColor)
            FeedingSubType.BREAST_BOTH_RL.name ->
                Triple("\uD83E\uDD31", "${strings.breastRight}\u2192${strings.breastLeft}", NaturalColor)
            FeedingSubType.PUMP.name ->
                Triple("\uD83E\uDED7", "${strings.pump}$mlSuffix", PumpColor)
            FeedingSubType.PUMP_LEFT.name ->
                Triple("\uD83E\uDED7", "${strings.pump} · ${strings.breastLeft}$mlSuffix", PumpColor)
            FeedingSubType.PUMP_RIGHT.name ->
                Triple("\uD83E\uDED7", "${strings.pump} · ${strings.breastRight}$mlSuffix", PumpColor)
            FeedingSubType.PUMP_BOTH_LR.name ->
                Triple("\uD83E\uDED7", "${strings.pump} · ${strings.breastLeft}\u2192${strings.breastRight}$mlSuffix", PumpColor)
            FeedingSubType.PUMP_BOTH_RL.name ->
                Triple("\uD83E\uDED7", "${strings.pump} · ${strings.breastRight}\u2192${strings.breastLeft}$mlSuffix", PumpColor)
            else -> Triple("\uD83E\uDD31", strings.breastFeeding, NaturalColor)
        }
        event.subType == DiaperSubType.PEE.name -> Triple("\uD83D\uDFE1", strings.pee, PeeColor)
        event.subType == DiaperSubType.POOP.name -> Triple("\uD83D\uDFE4", strings.poop, PoopColor)
        else -> Triple("\uD83D\uDFE0", strings.mixed, MixedColor)
    }
}

// ── Sheet handle ──────────────────────────────────────────────────────────────

@Composable
fun SheetHandle() {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Outline)
    )
}

// ── Date helper ───────────────────────────────────────────────────────────────

fun getTodayString(locale: java.util.Locale): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM", locale)
    return sdf.format(Date()).replaceFirstChar { it.uppercase() }
}
