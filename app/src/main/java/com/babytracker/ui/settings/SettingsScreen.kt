package com.babytracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.ui.i18n.LocalStrings
import com.babytracker.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: AppPreferences
) : ViewModel() {
    val babyName = prefs.babyName
    val language = prefs.language
    val themeMode = prefs.themeMode
    val reminderEnabled = prefs.reminderEnabled

    fun saveBabyName(name: String) { prefs.setBabyName(name) }
    fun setLanguage(lang: String) { prefs.setLanguage(lang) }
    fun setThemeMode(mode: String) { prefs.setThemeMode(mode) }
    fun setReminderEnabled(enabled: Boolean) { prefs.setReminderEnabled(enabled) }
    fun getReminderHours() = prefs.reminderDelayHours
    fun setReminderHours(h: Int) { prefs.reminderDelayHours = h }
    fun getReminderMinutes() = prefs.reminderDelayMinutes
    fun setReminderMinutes(m: Int) { prefs.reminderDelayMinutes = m }
    fun getShowBottle() = prefs.showBottle
    fun setShowBottle(v: Boolean) { prefs.showBottle = v }
    fun getShowBreast() = prefs.showBreast
    fun setShowBreast(v: Boolean) { prefs.showBreast = v }
    fun getShowPump() = prefs.showPump
    fun setShowPump(v: Boolean) { prefs.showPump = v }
    fun getShowSpitUp() = prefs.showSpitUp
    fun setShowSpitUp(v: Boolean) { prefs.showSpitUp = v }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val savedBabyName by viewModel.babyName.collectAsState()
    val savedLanguage by viewModel.language.collectAsState()
    val savedTheme by viewModel.themeMode.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()

    var nameInput by remember(savedBabyName) { mutableStateOf(savedBabyName) }
    var showSaved by remember { mutableStateOf(false) }
    var reminderHours by remember { mutableIntStateOf(viewModel.getReminderHours()) }
    var reminderMinutes by remember { mutableIntStateOf(viewModel.getReminderMinutes()) }
    var showBottle by remember { mutableStateOf(viewModel.getShowBottle()) }
    var showBreast by remember { mutableStateOf(viewModel.getShowBreast()) }
    var showPump by remember { mutableStateOf(viewModel.getShowPump()) }
    var showSpitUp by remember { mutableStateOf(viewModel.getShowSpitUp()) }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            kotlinx.coroutines.delay(2000)
            showSaved = false
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back, tint = TextPrimary)
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Baby name
            Text(strings.babyNameLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = { Text(strings.babyNameHint, color = TextHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FeedingColor,
                    focusedLabelColor = FeedingColor
                )
            )

            // Language
            Text(strings.languageLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            // Row 1: PL, EN, ES
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDF5\uD83C\uDDF1", strings.polish, savedLanguage == "pl") { viewModel.setLanguage("pl") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEC\uD83C\uDDE7", strings.english, savedLanguage == "en") { viewModel.setLanguage("en") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEA\uD83C\uDDF8", strings.spanish, savedLanguage == "es") { viewModel.setLanguage("es") }
            }
            // Row 2: FR, IT, UK, ZH
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEB\uD83C\uDDF7", strings.french, savedLanguage == "fr") { viewModel.setLanguage("fr") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEE\uD83C\uDDF9", strings.italian, savedLanguage == "it") { viewModel.setLanguage("it") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDFA\uD83C\uDDE6", strings.ukrainian, savedLanguage == "uk") { viewModel.setLanguage("uk") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDE8\uD83C\uDDF3", strings.chinese, savedLanguage == "zh") { viewModel.setLanguage("zh") }
            }

            // Theme
            Text(strings.themeLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to strings.themeSystem, "light" to strings.themeLight, "dark" to strings.themeDark).forEach { (mode, label) ->
                    val selected = savedTheme == mode
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(
                            2.dp,
                            if (selected) FeedingColor else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ).clickable { viewModel.setThemeMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) FeedingColor.copy(alpha = 0.08f) else SurfaceColor,
                        tonalElevation = 1.dp
                    ) {
                        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) FeedingColor else TextPrimary
                            )
                        }
                    }
                }
            }

            // Feeding reminder
            Text(strings.reminderLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.reminderEnabledLabel, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { viewModel.setReminderEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = SurfaceColor, checkedTrackColor = FeedingColor)
                )
            }
            if (reminderEnabled) {
                Text(strings.reminderDelayLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.reminderHoursUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = reminderHours.toFloat(),
                            onValueChange = { reminderHours = it.toInt(); viewModel.setReminderHours(it.toInt()) },
                            valueRange = 1f..12f,
                            steps = 10,
                            colors = SliderDefaults.colors(activeTrackColor = FeedingColor, thumbColor = FeedingColor)
                        )
                        Text("$reminderHours ${strings.reminderHoursUnit}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.reminderMinutesUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = reminderMinutes.toFloat(),
                            onValueChange = { reminderMinutes = it.toInt(); viewModel.setReminderMinutes(it.toInt()) },
                            valueRange = 0f..55f,
                            steps = 10,
                            colors = SliderDefaults.colors(activeTrackColor = FeedingColor, thumbColor = FeedingColor)
                        )
                        Text("$reminderMinutes ${strings.reminderMinutesUnit}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }

            // Feeding options filter
            Text(strings.feedingOptionsLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            listOf(
                strings.showBottleOption to Triple(showBottle, { v: Boolean -> showBottle = v; viewModel.setShowBottle(v) }, FeedingColor),
                strings.showBreastOption to Triple(showBreast, { v: Boolean -> showBreast = v; viewModel.setShowBreast(v) }, NaturalColor),
                strings.showPumpOption to Triple(showPump, { v: Boolean -> showPump = v; viewModel.setShowPump(v) }, PumpColor),
                strings.showSpitUpOption to Triple(showSpitUp, { v: Boolean -> showSpitUp = v; viewModel.setShowSpitUp(v) }, SpitUpColor),
            ).forEach { (label, triple) ->
                val (checked, onChanged, color) = triple
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = checked,
                        onCheckedChange = { onChanged(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SurfaceColor, checkedTrackColor = color)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    viewModel.saveBabyName(nameInput)
                    showSaved = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeedingColor)
            ) {
                if (showSaved) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.savedConfirmation, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(strings.saveButton, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageOption(
    modifier: Modifier = Modifier,
    flag: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) FeedingColor else Color.Transparent
    val bgColor = if (isSelected) FeedingColor.copy(alpha = 0.08f) else SurfaceColor
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(flag, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) FeedingColor else TextPrimary
            )
        }
    }
}
