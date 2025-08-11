package com.carplayer.iptv

import android.content.Context
import android.os.LocaleList
import java.util.Locale

object LocaleSettings {
    private val FAHRENHEIT_COUNTRIES = setOf("US", "BS", "BZ", "KY", "PW")
    
    fun shouldUseCelsius(context: Context): Boolean {
        val locales = context.resources.configuration.locales
        val countryCode = when {
            !locales.isEmpty -> locales.get(0).country
            else -> Locale.getDefault().country
        }
        return !FAHRENHEIT_COUNTRIES.contains(countryCode.uppercase())
    }
    
    fun convertToPreferredUnit(context: Context, tempFahrenheit: Float): Float {
        return if (shouldUseCelsius(context)) {
            (tempFahrenheit - 32) * 5/9  // Convert F to C
        } else {
            tempFahrenheit
        }
    }
    
    fun getTemperatureUnitSymbol(context: Context): String {
        return if (shouldUseCelsius(context)) "°C" else "°F"
    }
}