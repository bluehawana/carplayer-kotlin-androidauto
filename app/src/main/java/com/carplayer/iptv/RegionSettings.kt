package com.carplayer.iptv

import android.content.Context
import android.os.LocaleList
import java.util.Locale
import java.util.TimeZone

object RegionSettings {
    // Lindholmen, Gothenburg coordinates
    private const val LINDHOLMEN_LAT = 57.7072
    private const val LINDHOLMEN_LONG = 11.9378
    private const val TIMEZONE_STOCKHOLM = "Europe/Stockholm"
    
    fun applySwedishSettings(context: Context) {
        // Set Swedish locale
        val swedishLocale = Locale("sv", "SE")
        Locale.setDefault(swedishLocale)
        
        // Set timezone to Stockholm
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_STOCKHOLM))
        
        // Force Swedish locale list
        val localeList = LocaleList(swedishLocale)
        LocaleList.setDefault(localeList)
        
        // Apply to app context
        val config = context.resources.configuration
        config.setLocales(localeList)
        context.createConfigurationContext(config)
    }
    
    fun getLindholmenLocation(): Pair<Double, Double> {
        return Pair(LINDHOLMEN_LAT, LINDHOLMEN_LONG)
    }
    
    fun getSwedishLocale(): Locale {
        return Locale("sv", "SE")
    }
    
    fun getTemperatureUnit(): String {
        return "°C"  // Sweden always uses Celsius
    }
    
    fun convertToMetric(tempF: Float): Float {
        return (tempF - 32) * 5/9  // Convert F to C
    }
}