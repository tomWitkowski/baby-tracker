package com.babytracker.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.ui.i18n.AppStrings
import com.babytracker.ui.i18n.stringsForLang
import com.babytracker.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {
    fun onLanguageSelected(lang: String) {
        prefs.setLanguage(lang)
        prefs.isFirstLaunch = false
    }
}

@Composable
fun LanguageSelectionScreen(
    onDone: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    var selected by remember { mutableStateOf("pl") }
    val preview: AppStrings = stringsForLang(selected)

    val languages = listOf(
        Triple("pl", "\uD83C\uDDF5\uD83C\uDDF1", "Polski"),
        Triple("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
        Triple("es", "\uD83C\uDDEA\uD83C\uDDF8", "Español"),
        Triple("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Français"),
        Triple("it", "\uD83C\uDDEE\uD83C\uDDF9", "Italiano"),
        Triple("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "🍼",
                fontSize = 64.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Baby Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = preview.chooseLanguage,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: PL, EN, ES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                languages.take(3).forEach { (code, flag, label) ->
                    LanguageCard(
                        modifier = Modifier.weight(1f),
                        flag = flag,
                        label = label,
                        isSelected = selected == code,
                        onClick = { selected = code }
                    )
                }
            }
            // Row 2: FR, IT, DE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                languages.drop(3).forEach { (code, flag, label) ->
                    LanguageCard(
                        modifier = Modifier.weight(1f),
                        flag = flag,
                        label = label,
                        isSelected = selected == code,
                        onClick = { selected = code }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.onLanguageSelected(selected)
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeedingColor)
            ) {
                Text(
                    preview.continueButton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
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
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(flag, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) FeedingColor else TextPrimary
            )
        }
    }
}
