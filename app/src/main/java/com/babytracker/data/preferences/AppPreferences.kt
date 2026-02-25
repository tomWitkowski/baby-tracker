package com.babytracker.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(prefs.getString("language", "pl") ?: "pl")
    val language: StateFlow<String> = _language

    private val _babyName = MutableStateFlow(prefs.getString("baby_name", "") ?: "")
    val babyName: StateFlow<String> = _babyName

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(value) { prefs.edit().putBoolean("first_launch", value).apply() }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setBabyName(name: String) {
        prefs.edit().putString("baby_name", name).apply()
        _babyName.value = name
    }

    // ── Theme: "system" | "light" | "dark" ───────────────────────────────────
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    // ── Feeding reminder ──────────────────────────────────────────────────────
    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
        _reminderEnabled.value = enabled
    }

    var reminderTotalMinutes: Int
        get() = prefs.getInt("reminder_total_minutes", 180)
        set(value) { prefs.edit().putInt("reminder_total_minutes", value).apply() }

    // ── Visible feeding/diaper options ────────────────────────────────────────
    var showBottle: Boolean
        get() = prefs.getBoolean("show_bottle", true)
        set(value) { prefs.edit().putBoolean("show_bottle", value).apply() }

    var showBreast: Boolean
        get() = prefs.getBoolean("show_breast", true)
        set(value) { prefs.edit().putBoolean("show_breast", value).apply() }

    var showPump: Boolean
        get() = prefs.getBoolean("show_pump", true)
        set(value) { prefs.edit().putBoolean("show_pump", value).apply() }

    var showSpitUp: Boolean
        get() = prefs.getBoolean("show_spit_up", true)
        set(value) { prefs.edit().putBoolean("show_spit_up", value).apply() }
}
