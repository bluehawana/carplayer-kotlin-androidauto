package com.carplayer.iptv

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import java.util.Locale
import java.util.TimeZone

object CarSettings {
    private const val PREF_NAME = "car_settings"
    private const val KEY_METRIC = "use_metric"

    fun applyEUSettings(carContext: CarContext) {
        try {
            // Set locale to Swedish
            val swedishLocale = Locale("sv", "SE")
            Locale.setDefault(swedishLocale)

            // Set timezone to Stockholm
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"))

            // Store settings
            carContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_METRIC, true)
                .apply()

            // Show confirmation
            CarToast.makeText(carContext, "🇸🇪 Using EU settings", CarToast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            CarToast.makeText(carContext, "⚠️ Could not set EU settings", CarToast.LENGTH_SHORT).show()
        }
    }

    fun convertToCelsius(tempF: Float): Float {
        return (tempF - 32) * 5/9
    }

    fun getTemperatureUnit(): String {
        return "°C"  // Always use Celsius
    }

    fun formatTemperature(tempF: Float): String {
        val tempC = convertToCelsius(tempF)
        return "${tempC.toInt()}${getTemperatureUnit()}"
    }
}