package com.babytracker.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.ui.i18n.LocalStrings
import com.babytracker.ui.main.SheetHandle
import com.babytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val ML_SUBTYPES = setOf(
    FeedingSubType.BOTTLE.name,
    FeedingSubType.BOTTLE_FORMULA.name,
    FeedingSubType.BOTTLE_EXPRESSED.name,
    FeedingSubType.PUMP.name,
    FeedingSubType.PUMP_LEFT.name,
    FeedingSubType.PUMP_RIGHT.name,
    FeedingSubType.PUMP_BOTH_LR.name,
    FeedingSubType.PUMP_BOTH_RL.name
)

@Composable
fun EditEventSheet(
    event: BabyEvent,
    onDismiss: () -> Unit,
    onSave: (BabyEvent) -> Unit
) {
    val strings = LocalStrings.current
    var editedTimestamp by remember { mutableStateOf(event.timestamp) }
    var editedEventType by remember { mutableStateOf(event.toEventType()) }
    var editedSubType by remember { mutableStateOf(event.subType) }
    var editedMilliliters by remember { mutableStateOf(event.milliliters?.toString() ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerWrapper(
            timestamp = editedTimestamp,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newTs -> editedTimestamp = newTs; showDatePicker = false }
        )
    }
    if (showTimePicker) {
        TimePickerWrapper(
            timestamp = editedTimestamp,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newTs -> editedTimestamp = newTs; showTimePicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandle()
        Spacer(Modifier.height(16.dp))
        Text(
            strings.editEvent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(24.dp))

        // Date + time row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val dateStr = SimpleDateFormat("d MMM yyyy", strings.locale).format(Date(editedTimestamp))
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceColor,
                tonalElevation = 2.dp,
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Column {
                        Text(strings.dateLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(editedTimestamp))
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceColor,
                tonalElevation = 2.dp,
                onClick = { showTimePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Column {
                        Text(strings.timeLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(timeStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Event type selector
        Text(strings.eventTypeLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventType.entries.forEach { type ->
                val selected = editedEventType == type
                val (color, emoji, label) = when (type) {
                    EventType.FEEDING -> Triple(FeedingColor, "\uD83C\uDF7C", strings.feeding)
                    EventType.DIAPER -> Triple(DiaperColor, "\uD83E\uDE72", strings.diaper)
                    EventType.SPIT_UP -> Triple(SpitUpColor, "\u21A9\uFE0F", strings.spitUp)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) color.copy(alpha = 0.15f) else SurfaceColor,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                    onClick = {
                        editedEventType = type
                        editedSubType = when (type) {
                            EventType.FEEDING -> FeedingSubType.BOTTLE.name
                            EventType.DIAPER -> DiaperSubType.PEE.name
                            EventType.SPIT_UP -> EventType.SPIT_UP.name
                        }
                        if (type != EventType.FEEDING) editedMilliliters = ""
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) color else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Subtype selector (hidden for SPIT_UP)
        if (editedEventType != EventType.SPIT_UP) {
            Text(strings.details, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            if (editedEventType == EventType.FEEDING) {
                // Row 1: Bottle types
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(FeedingSubType.BOTTLE.name, "\uD83C\uDF7C", strings.bottle, BottleColor),
                        SubTypeItem(FeedingSubType.BOTTLE_FORMULA.name, "\uD83C\uDF7C", strings.bottleFormula, BottleColor),
                        SubTypeItem(FeedingSubType.BOTTLE_EXPRESSED.name, "\uD83E\uDD3C", strings.bottleExpressed, NaturalColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
                Spacer(Modifier.height(8.dp))
                // Row 2: Breast left / right
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(FeedingSubType.BREAST_LEFT.name, "\u2B05\uFE0F", strings.breastLeft, NaturalColor),
                        SubTypeItem(FeedingSubType.BREAST_RIGHT.name, "\u27A1\uFE0F", strings.breastRight, NaturalColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
                Spacer(Modifier.height(8.dp))
                // Row 3: Breast both
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(FeedingSubType.BREAST_BOTH_LR.name, "\u2194\uFE0F", "${strings.breastLeft}\u2192${strings.breastRight}", NaturalColor),
                        SubTypeItem(FeedingSubType.BREAST_BOTH_RL.name, "\u2194\uFE0F", "${strings.breastRight}\u2192${strings.breastLeft}", NaturalColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
                Spacer(Modifier.height(8.dp))
                // Row 4: Pump left / right
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(FeedingSubType.PUMP_LEFT.name, "\u2B05\uFE0F", "${strings.pump} ${strings.breastLeft}", PumpColor),
                        SubTypeItem(FeedingSubType.PUMP_RIGHT.name, "\u27A1\uFE0F", "${strings.pump} ${strings.breastRight}", PumpColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
                Spacer(Modifier.height(8.dp))
                // Row 5: Pump both
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(FeedingSubType.PUMP_BOTH_LR.name, "\u2194\uFE0F", "${strings.pump} L\u2192P", PumpColor),
                        SubTypeItem(FeedingSubType.PUMP_BOTH_RL.name, "\u2194\uFE0F", "${strings.pump} P\u2192L", PumpColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
                // Legacy PUMP (shown only if event was created with it)
                if (editedSubType == FeedingSubType.PUMP.name) {
                    Spacer(Modifier.height(8.dp))
                    SubTypeRow(
                        items = listOf(
                            SubTypeItem(FeedingSubType.PUMP.name, "\uD83E\uDED7", strings.pump, PumpColor)
                        ),
                        selectedSubType = editedSubType,
                        onSelect = { editedSubType = it }
                    )
                }
                // ML input for bottle/pump subtypes
                if (editedSubType in ML_SUBTYPES) {
                    val mlColor = when (editedSubType) {
                        FeedingSubType.BOTTLE_EXPRESSED.name -> NaturalColor
                        FeedingSubType.PUMP.name, FeedingSubType.PUMP_LEFT.name,
                        FeedingSubType.PUMP_RIGHT.name, FeedingSubType.PUMP_BOTH_LR.name,
                        FeedingSubType.PUMP_BOTH_RL.name -> PumpColor
                        else -> BottleColor
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedMilliliters,
                        onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) editedMilliliters = v },
                        label = { Text(strings.mlOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = mlColor,
                            focusedLabelColor = mlColor
                        ),
                        suffix = { Text("ml", color = TextSecondary) }
                    )
                }
            } else {
                // Diaper subtypes
                SubTypeRow(
                    items = listOf(
                        SubTypeItem(DiaperSubType.PEE.name, "\uD83D\uDFE1", strings.pee, PeeColor),
                        SubTypeItem(DiaperSubType.POOP.name, "\uD83D\uDFE4", strings.poop, PoopColor),
                        SubTypeItem(DiaperSubType.MIXED.name, "\uD83D\uDFE0", strings.mixed, MixedColor)
                    ),
                    selectedSubType = editedSubType,
                    onSelect = { editedSubType = it }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val ml = if (editedSubType in ML_SUBTYPES) editedMilliliters.toIntOrNull() else null
                onSave(
                    event.copy(
                        timestamp = editedTimestamp,
                        eventType = editedEventType.name,
                        subType = editedSubType,
                        milliliters = ml
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text(strings.saveChanges, fontWeight = FontWeight.SemiBold, color = SurfaceColor)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Sub-type row helper ───────────────────────────────────────────────────────

private data class SubTypeItem(val subType: String, val emoji: String, val label: String, val color: androidx.compose.ui.graphics.Color)

@Composable
private fun SubTypeRow(
    items: List<SubTypeItem>,
    selectedSubType: String,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val selected = selectedSubType == item.subType
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) item.color.copy(alpha = 0.15f) else SurfaceColor,
                tonalElevation = if (selected) 0.dp else 1.dp,
                onClick = { onSelect(item.subType) }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.emoji, fontSize = 20.sp)
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) item.color else TextSecondary
                    )
                }
            }
        }
    }
}

// ── Date/time pickers ─────────────────────────────────────────────────────────

@Composable
private fun DatePickerWrapper(
    timestamp: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        var handled = false
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                handled = true
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                onDateSelected(newCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnDismissListener { if (!handled) onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}

@Composable
private fun TimePickerWrapper(
    timestamp: Long,
    onDismiss: () -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        var handled = false
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                handled = true
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onTimeSelected(newCal.timeInMillis)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )
        dialog.setOnDismissListener { if (!handled) onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}
