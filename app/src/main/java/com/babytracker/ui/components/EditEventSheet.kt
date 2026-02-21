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
import com.babytracker.ui.main.SheetHandle
import com.babytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditEventSheet(
    event: BabyEvent,
    onDismiss: () -> Unit,
    onSave: (BabyEvent) -> Unit
) {
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
            onDateSelected = { newTs ->
                editedTimestamp = newTs
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        TimePickerWrapper(
            timestamp = editedTimestamp,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newTs ->
                editedTimestamp = newTs
                showTimePicker = false
            }
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
            "Edytuj zdarzenie",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(24.dp))

        // Date + time row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dateStr = SimpleDateFormat("d MMM yyyy", Locale("pl")).format(Date(editedTimestamp))
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
                        Text("Data", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                        Text("Godzina", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(timeStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Event type selector
        Text("Typ zdarzenia", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EventType.entries.forEach { type ->
                val selected = editedEventType == type
                val color = if (type == EventType.FEEDING) FeedingColor else DiaperColor
                val emoji = if (type == EventType.FEEDING) "\uD83C\uDF7C" else "\uD83D\uDCCD"
                val label = if (type == EventType.FEEDING) "Karmienie" else "Pieluszka"
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) color.copy(alpha = 0.15f) else SurfaceColor,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                    onClick = {
                        editedEventType = type
                        editedSubType = if (type == EventType.FEEDING) FeedingSubType.BOTTLE.name else DiaperSubType.PEE.name
                        if (type == EventType.DIAPER) editedMilliliters = ""
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) color else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Subtype selector
        Text("Szczegóły", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        if (editedEventType == EventType.FEEDING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    FeedingSubType.BOTTLE to ("\uD83C\uDF7C" to "Butelka"),
                    FeedingSubType.NATURAL to ("\uD83E\uDD31" to "Naturalne")
                ).forEach { (subType, pair) ->
                    val (emoji, label) = pair
                    val selected = editedSubType == subType.name
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) BottleColor.copy(alpha = 0.15f) else SurfaceColor,
                        tonalElevation = if (selected) 0.dp else 1.dp,
                        onClick = { editedSubType = subType.name }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) BottleColor else TextSecondary
                            )
                        }
                    }
                }
            }
            if (editedSubType == FeedingSubType.BOTTLE.name) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editedMilliliters,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) editedMilliliters = v },
                    label = { Text("Ilość ml (opcjonalne)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BottleColor,
                        focusedLabelColor = BottleColor
                    ),
                    suffix = { Text("ml", color = TextSecondary) }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DiaperSubType.PEE to ("\uD83D\uDFE1" to "Siku"),
                    DiaperSubType.POOP to ("\uD83D\uDFE4" to "Kupka"),
                    DiaperSubType.MIXED to ("\uD83D\uDFE0" to "Mieszane")
                ).forEach { (subType, pair) ->
                    val (emoji, label) = pair
                    val selected = editedSubType == subType.name
                    val color = when (subType) {
                        DiaperSubType.PEE -> PeeColor
                        DiaperSubType.POOP -> PoopColor
                        DiaperSubType.MIXED -> MixedColor
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) color.copy(alpha = 0.15f) else SurfaceColor,
                        tonalElevation = if (selected) 0.dp else 1.dp,
                        onClick = { editedSubType = subType.name }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, fontSize = 20.sp)
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
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val ml = if (editedSubType == FeedingSubType.BOTTLE.name) editedMilliliters.toIntOrNull() else null
                onSave(
                    event.copy(
                        timestamp = editedTimestamp,
                        eventType = editedEventType.name,
                        subType = editedSubType,
                        milliliters = ml
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text("Zapisz zmiany", fontWeight = FontWeight.SemiBold, color = SurfaceColor)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDismiss) {
            Text("Anuluj", color = TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
    }
}

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
            true // 24h
        )
        dialog.setOnDismissListener { if (!handled) onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}
