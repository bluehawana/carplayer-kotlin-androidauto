package com.carplayer.iptv

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppSettings {
    private const val PREF_NAME = "app_settings"
    private const val KEY_FORCE_CELSIUS = "force_celsius"
    private const val KEY_LANGUAGE = "language"

    fun setForceCelsius(context: Context, forceCelsius: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FORCE_CELSIUS, forceCelsius)
            .apply()
    }

    fun shouldForceCelsius(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_CELSIUS, true)  // Default to true for Sweden
    }

    fun convertToPreferredUnit(context: Context, tempFahrenheit: Float): Float {
        return if (shouldForceCelsius(context)) {
            (tempFahrenheit - 32) * 5/9  // Convert F to C
        } else {
            tempFahrenheit
        }
    }

    fun getTemperatureUnitSymbol(context: Context): String {
        return if (shouldForceCelsius(context)) "°C" else "°F"
    }

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun applyLocale(context: Context) {
        val languageCode = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "sv") ?: "sv"  // Default to Swedish
            
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
}