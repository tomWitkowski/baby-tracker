package com.babytracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    private val prefs: AppPreferences
) : ViewModel() {
    val babyName = prefs.babyName
    val language = prefs.language

    fun saveBabyName(name: String) { prefs.setBabyName(name) }
    fun setLanguage(lang: String) { prefs.setLanguage(lang) }
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

    var nameInput by remember(savedBabyName) { mutableStateOf(savedBabyName) }
    var showSaved by remember { mutableStateOf(false) }

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
                title = {
                    Text(
                        strings.settingsTitle,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Baby name section
            Text(
                strings.babyNameLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
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

            // Language section
            Text(
                strings.languageLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LanguageOption(
                    modifier = Modifier.weight(1f),
                    flag = "\uD83C\uDDF5\uD83C\uDDF1",
                    label = strings.polish,
                    isSelected = savedLanguage == "pl",
                    onClick = { viewModel.setLanguage("pl") }
                )
                LanguageOption(
                    modifier = Modifier.weight(1f),
                    flag = "\uD83C\uDDEC\uD83C\uDDE7",
                    label = strings.english,
                    isSelected = savedLanguage == "en",
                    onClick = { viewModel.setLanguage("en") }
                )
            }

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    viewModel.saveBabyName(nameInput)
                    showSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(flag, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) FeedingColor else TextPrimary
            )
        }
    }
}
