package com.carplayer.iptv

import android.content.Context

object SettingsStore {
    private const val PREF_NAME = "eu_settings"
    private const val KEY_UNIT_SYSTEM = "unit_system"
    private const val KEY_TEMP_UNIT = "temp_unit"
    private const val KEY_LOCALE = "locale"
    private const val KEY_TIMEZONE = "timezone"
    
    fun saveEUPreferences(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UNIT_SYSTEM, "metric")
            .putString(KEY_TEMP_UNIT, "celsius")
            .putString(KEY_LOCALE, "sv-SE")
            .putString(KEY_TIMEZONE, "Europe/Stockholm")
            .apply()
    }
    
    fun loadEUPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // If preferences don't exist, set EU defaults
        if (!prefs.contains(KEY_UNIT_SYSTEM)) {
            saveEUPreferences(context)
        }
        
        // Apply settings from preferences
        EUSettings.applyEUStandards(context)
    }
    
    fun ensureEUStandards(context: Context) {
        // Load and apply preferences
        loadEUPreferences(context)
        
        // Save current settings to ensure persistence
        saveEUPreferences(context)
    }
}