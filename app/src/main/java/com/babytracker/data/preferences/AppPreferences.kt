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
}
