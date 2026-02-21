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
import com.babytracker.ui.components.EditEventSheet
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
import com.babytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

sealed class BottomSheetState {
    object Hidden : BottomSheetState()
    object FeedingOptions : BottomSheetState()
    object DiaperOptions : BottomSheetState()
    object BottleMlInput : BottomSheetState()
    data class Editing(val event: BabyEvent) : BottomSheetState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val recentEvents by viewModel.recentEvents.collectAsState()
    var bottomSheetState by remember { mutableStateOf<BottomSheetState>(BottomSheetState.Hidden) }
    var mlInput by remember { mutableStateOf("") }
    var showSuccessBadge by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

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
                        text = "Dziecko",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getTodayString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
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
                        contentDescription = "Dashboard",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
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
                // Feeding button
                ActionDot(
                    modifier = Modifier.weight(1f),
                    color = FeedingColor,
                    lightColor = FeedingColorLight,
                    emoji = "\uD83C\uDF7C",
                    label = "Karmienie",
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

                // Diaper button
                ActionDot(
                    modifier = Modifier.weight(1f),
                    color = DiaperColor,
                    lightColor = DiaperColorLight,
                    emoji = "\uD83D\uDCCD",
                    label = "Pieluszka",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        bottomSheetState = BottomSheetState.DiaperOptions
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hint text
            Text(
                text = "Przytrzymaj butelkę aby wpisać ml",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Recent events
            if (recentEvents.isNotEmpty()) {
                Text(
                    text = "Ostatnie zdarzenia",
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
                            onDelete = { viewModel.deleteEvent(event) },
                            onEdit = { bottomSheetState = BottomSheetState.Editing(event) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Dotknij przycisku,\naby zarejestrować zdarzenie",
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

        // Dimmed overlay + bottom sheet
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
                when (bottomSheetState) {
                    BottomSheetState.FeedingOptions -> FeedingSheet(
                        onBottle = {
                            viewModel.logBottleFeeding(null)
                            showSuccessBadge = "Butelka zapisana ✓"
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onBottleWithMl = {
                            mlInput = ""
                            bottomSheetState = BottomSheetState.BottleMlInput
                        },
                        onNatural = {
                            viewModel.logNaturalFeeding()
                            showSuccessBadge = "Karmienie zapisane ✓"
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    BottomSheetState.DiaperOptions -> DiaperSheet(
                        onPee = {
                            viewModel.logDiaper(DiaperSubType.PEE)
                            showSuccessBadge = "Siku zapisane ✓"
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onPoop = {
                            viewModel.logDiaper(DiaperSubType.POOP)
                            showSuccessBadge = "Kupka zapisana ✓"
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onMixed = {
                            viewModel.logDiaper(DiaperSubType.MIXED)
                            showSuccessBadge = "Mieszane zapisane ✓"
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
                            val msg = if (ml != null) "Butelka ${ml}ml zapisana ✓" else "Butelka zapisana ✓"
                            showSuccessBadge = msg
                            bottomSheetState = BottomSheetState.Hidden
                        },
                        onDismiss = { bottomSheetState = BottomSheetState.Hidden }
                    )
                    is BottomSheetState.Editing -> EditEventSheet(
                        event = (bottomSheetState as BottomSheetState.Editing).event,
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
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(lightColor, color)
                    )
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 52.sp,
                textAlign = TextAlign.Center
            )
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

@Composable
fun FeedingSheet(
    onBottle: () -> Unit,
    onBottleWithMl: () -> Unit,
    onNatural: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Karmienie",
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
                title = "Butelka",
                subtitle = "Szybki zapis",
                color = BottleColor,
                onClick = onBottle
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83C\uDF7C",
                title = "Butelka + ml",
                subtitle = "Podaj ilość",
                color = BottleColor,
                onClick = onBottleWithMl
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OptionCard(
            modifier = Modifier.fillMaxWidth(),
            emoji = "\uD83E\uDD31",
            title = "Karmienie naturalne",
            subtitle = "Pierś",
            color = NaturalColor,
            onClick = onNatural
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onDismiss) {
            Text("Anuluj", color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun DiaperSheet(
    onPee: () -> Unit,
    onPoop: () -> Unit,
    onMixed: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Pieluszka",
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
                title = "Siku",
                subtitle = "Mokra pieluszka",
                color = PeeColor,
                onClick = onPee
            )
            OptionCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83D\uDFE4",
                title = "Kupka",
                subtitle = "",
                color = PoopColor,
                onClick = onPoop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OptionCard(
            modifier = Modifier.fillMaxWidth(),
            emoji = "\uD83D\uDFE0",
            title = "Mieszane",
            subtitle = "Siku i kupka",
            color = MixedColor,
            onClick = onMixed
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onDismiss) {
            Text("Anuluj", color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun BottleMlSheet(
    mlInput: String,
    onMlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "\uD83C\uDF7C Butelka",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ile mililitrów? (opcjonalne)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = mlInput,
            onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) onMlChange(v) },
            label = { Text("Ilość") },
            placeholder = { Text("np. 120") },
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BottleColor)
        ) {
            Text(
                if (mlInput.isEmpty()) "Zapisz bez ilości" else "Zapisz ${mlInput}ml",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onDismiss) {
            Text("Anuluj", color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

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

@Composable
fun EventRow(
    event: BabyEvent,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
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
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edytuj",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

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

fun getTodayString(): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("pl"))
    return sdf.format(Date()).replaceFirstChar { it.uppercase() }
}
