package com.carplayer.iptv

import android.content.Context
import android.os.LocaleList
import java.util.Locale
import java.util.TimeZone
import java.text.NumberFormat
import java.text.DateFormat

object EUSettings {
    // Lindholmen, Gothenburg, Sweden
    private const val LINDHOLMEN_LAT = 57.7072
    private const val LINDHOLMEN_LONG = 11.9378
    
    // Swedish/EU Standards
    private const val COUNTRY_CODE = "SE"
    private const val LANGUAGE_CODE = "sv"
    private const val TIMEZONE = "Europe/Stockholm"
    private const val CURRENCY = "EUR"
    
    fun applyEUStandards(context: Context) {
        // Set Swedish locale
        val swedishLocale = Locale(LANGUAGE_CODE, COUNTRY_CODE)
        Locale.setDefault(swedishLocale)
        
        // Set EU locale list (Swedish primary, English secondary)
        val localeList = LocaleList(
            swedishLocale,
            Locale("en", "GB")  // British English as fallback
        )
        LocaleList.setDefault(localeList)
        
        // Apply to configuration
        val config = context.resources.configuration.apply {
            setLocales(localeList)
            setLayoutDirection(swedishLocale)
        }
        context.createConfigurationContext(config)
        
        // Set timezone to Stockholm
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE))
    }
    
    fun getLocation(): Pair<Double, Double> {
        return Pair(LINDHOLMEN_LAT, LINDHOLMEN_LONG)
    }
    
    // Temperature (Celsius)
    fun getTemperatureUnit(): String = "°C"
    
    fun convertToEUTemperature(fahrenheit: Float): Float {
        return (fahrenheit - 32) * 5/9  // F to C
    }
    
    // Speed (km/h)
    fun convertToEUSpeed(mph: Float): Float {
        return mph * 1.60934f  // MPH to KPH
    }
    
    fun getSpeedUnit(): String = "km/h"
    
    // Distance (meters/kilometers)
    fun convertToEUDistance(miles: Float): Float {
        return miles * 1.60934f  // Miles to Kilometers
    }
    
    fun getDistanceUnit(distance: Float): String {
        return if (distance < 1) "m" else "km"
    }
    
    // Date and Time (24-hour format)
    fun formatDateTime(context: Context, timeMillis: Long): String {
        val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            Locale(LANGUAGE_CODE, COUNTRY_CODE)
        )
        return dateFormat.format(timeMillis)
    }
    
    // Numbers (using comma as decimal separator)
    fun formatNumber(number: Double): String {
        return NumberFormat.getInstance(Locale(LANGUAGE_CODE, COUNTRY_CODE))
            .format(number)
    }
    
    // Currency (EUR)
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale(LANGUAGE_CODE, COUNTRY_CODE))
            .format(amount)
    }
    
    // Measurement system
    fun isMetric(): Boolean = true
    
    // Paper size (A4)
    fun getPaperSize(): String = "A4"
    
    // Phone number format
    fun formatPhoneNumber(number: String): String {
        // Swedish format: +46 XX XXX XX XX
        return number.replace(Regex("(\\d{2})(\\d{3})(\\d{2})(\\d{2})"), "+46 $1 $2 $3 $4")
    }
    
    // Postal code format
    fun formatPostalCode(code: String): String {
        // Swedish format: XXX XX
        return code.replace(Regex("(\\d{3})(\\d{2})"), "$1 $2")
    }
}