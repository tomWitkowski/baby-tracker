package com.babytracker.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
    fun getReminderTotalMinutes() = prefs.reminderTotalMinutes
    fun setReminderTotalMinutes(m: Int) { prefs.reminderTotalMinutes = m }
    fun getShowBottle() = prefs.showBottle
    fun setShowBottle(v: Boolean) { prefs.showBottle = v }
    fun getShowBreast() = prefs.showBreast
    fun setShowBreast(v: Boolean) { prefs.showBreast = v }
    fun getShowPump() = prefs.showPump
    fun setShowPump(v: Boolean) { prefs.showPump = v }
    fun getShowSpitUp() = prefs.showSpitUp
    fun setShowSpitUp(v: Boolean) { prefs.showSpitUp = v }
    fun getShowBottleFormula() = prefs.showBottleFormula
    fun setShowBottleFormula(v: Boolean) { prefs.showBottleFormula = v }
    fun getShowBottleExpressed() = prefs.showBottleExpressed
    fun setShowBottleExpressed(v: Boolean) { prefs.showBottleExpressed = v }

    val isPro get() = prefs.isPro
    fun isProOrTrial() = prefs.isProOrTrial()
    fun startTrial() { prefs.startTrial() }
    fun trialDaysRemaining() = prefs.trialDaysRemaining()
    fun hasTrialStarted() = prefs.hasTrialStarted()
    /** Debug only: expire trial immediately so expiry behaviour can be tested. */
    fun debugExpireTrial() { if (prefs.hasTrialStarted()) prefs.proTrialExpiryMs = System.currentTimeMillis() - 1000L }
    /** Debug only: activate Pro without purchase (for testing post-purchase flow). */
    fun debugActivatePro() { prefs.isPro = true }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val savedBabyName by viewModel.babyName.collectAsState()
    val savedLanguage by viewModel.language.collectAsState()
    val savedTheme by viewModel.themeMode.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()

    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted or denied — system handles the rationale */ }

    var nameInput by remember(savedBabyName) { mutableStateOf(savedBabyName) }
    var showSaved by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var showUpgradeInfoDialog by remember { mutableStateOf(false) }
    var proIsActive by remember { mutableStateOf(viewModel.isProOrTrial()) }
    var reminderTotalMinutes by remember { mutableIntStateOf(viewModel.getReminderTotalMinutes()) }
    var showBottle by remember { mutableStateOf(viewModel.getShowBottle()) }
    var showBottleFormula by remember { mutableStateOf(viewModel.getShowBottleFormula()) }
    var showBottleExpressed by remember { mutableStateOf(viewModel.getShowBottleExpressed()) }
    var showBreast by remember { mutableStateOf(viewModel.getShowBreast()) }
    var showPump by remember { mutableStateOf(viewModel.getShowPump()) }
    var showSpitUp by remember { mutableStateOf(viewModel.getShowSpitUp()) }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            kotlinx.coroutines.delay(2000)
            showSaved = false
        }
    }

    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            title = { Text(strings.proRequiredTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.proRequiredBody) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startTrial()
                        proIsActive = true
                        showProDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FeedingColor)
                ) { Text(strings.proRequiredStart, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showProDialog = false }) {
                    Text(strings.proRequiredCancel, color = TextHint)
                }
            }
        )
    }

    if (showUpgradeInfoDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeInfoDialog = false },
            title = { Text(strings.proRequiredTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.proTrialExpiredBody) },
            confirmButton = {
                Button(
                    onClick = { showUpgradeInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FeedingColor)
                ) { Text(strings.proRequiredCancel, fontWeight = FontWeight.SemiBold) }
            }
        )
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

            // ── Maluszek Pro card ─────────────────────────────────────────────
            val proStatusText = when {
                viewModel.isPro -> strings.proStatusPro
                viewModel.hasTrialStarted() && proIsActive -> String.format(strings.proStatusTrialActiveFmt, viewModel.trialDaysRemaining())
                viewModel.hasTrialStarted() && !proIsActive -> strings.proStatusTrialExpired
                else -> strings.proStatusFree
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FeedingColor.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.proSectionTitle,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    viewModel.debugActivatePro()
                                    proIsActive = viewModel.isProOrTrial()
                                }
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (proIsActive) FeedingColor else TextHint.copy(alpha = 0.2f),
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    viewModel.debugExpireTrial()
                                    proIsActive = viewModel.isProOrTrial()
                                }
                            )
                        ) {
                            Text(
                                proStatusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (proIsActive) Color.White else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(strings.proFeatureDesc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(strings.proPrice, style = MaterialTheme.typography.bodySmall, color = TextHint)
                    if (!proIsActive) {
                        Spacer(Modifier.height(2.dp))
                        Button(
                            onClick = {
                                if (!viewModel.hasTrialStarted()) {
                                    viewModel.startTrial()
                                    proIsActive = true
                                } else {
                                    showUpgradeInfoDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FeedingColor)
                        ) {
                            Text(
                                if (!viewModel.hasTrialStarted()) strings.proStartTrialCta else strings.proUpgradeCta,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Language
            Text(strings.languageLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            // Row 1: PL, EN, ES
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDF5\uD83C\uDDF1", strings.polish, savedLanguage == "pl") { viewModel.setLanguage("pl") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEC\uD83C\uDDE7", strings.english, savedLanguage == "en") { viewModel.setLanguage("en") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEA\uD83C\uDDF8", strings.spanish, savedLanguage == "es") { viewModel.setLanguage("es") }
            }
            // Row 2: FR, IT, DE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEB\uD83C\uDDF7", strings.french, savedLanguage == "fr") { viewModel.setLanguage("fr") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDEE\uD83C\uDDF9", strings.italian, savedLanguage == "it") { viewModel.setLanguage("it") }
                LanguageOption(Modifier.weight(1f), "\uD83C\uDDE9\uD83C\uDDEA", strings.german, savedLanguage == "de") { viewModel.setLanguage("de") }
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
                    onCheckedChange = { enabled ->
                        if (enabled && !viewModel.isProOrTrial()) {
                            showProDialog = true
                        } else {
                            viewModel.setReminderEnabled(enabled)
                            // On Android 13+ request POST_NOTIFICATIONS if not yet granted
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = SurfaceColor, checkedTrackColor = FeedingColor)
                )
            }
            if (reminderEnabled) {
                val displayHours = reminderTotalMinutes / 60
                val displayMins = reminderTotalMinutes % 60
                val delayText = if (displayHours > 0 && displayMins > 0)
                    "$displayHours ${strings.reminderHoursUnit} $displayMins ${strings.reminderMinutesUnit}"
                else if (displayHours > 0)
                    "$displayHours ${strings.reminderHoursUnit}"
                else
                    "$displayMins ${strings.reminderMinutesUnit}"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.reminderDelayLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(delayText, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
                Slider(
                    value = reminderTotalMinutes.toFloat(),
                    onValueChange = {
                        val snapped = maxOf(5, (it / 5).toInt() * 5)
                        reminderTotalMinutes = snapped
                        viewModel.setReminderTotalMinutes(snapped)
                    },
                    valueRange = 5f..480f,
                    steps = 94,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(activeTrackColor = FeedingColor, thumbColor = FeedingColor)
                )
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
                if (label == strings.showBottleOption && showBottle) {
                    listOf(
                        strings.showBottleFormulaOption to Pair(showBottleFormula, { v: Boolean -> showBottleFormula = v; viewModel.setShowBottleFormula(v) }),
                        strings.showBottleExpressedOption to Pair(showBottleExpressed, { v: Boolean -> showBottleExpressed = v; viewModel.setShowBottleExpressed(v) }),
                    ).forEach { (subLabel, subPair) ->
                        val (subChecked, subOnChanged) = subPair
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Switch(
                                checked = subChecked,
                                onCheckedChange = { subOnChanged(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = SurfaceColor, checkedTrackColor = FeedingColor)
                            )
                        }
                    }
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
