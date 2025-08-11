package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class TemperatureScreen(carContext: CarContext, private val tempF: Float) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val tempC = CarSettings.convertToCelsius(tempF)
        
        return MessageTemplate.Builder(
            "🌡️ Temperature\n" +
            "${tempF.toInt()}°F = ${tempC.toInt()}°C\n" +
            "\n" +
            "🇸🇪 Using European settings"
        )
        .setTitle("Temperature Conversion")
        .addAction(
            Action.Builder()
                .setTitle("Close")
                .setOnClickListener { finish() }
                .build()
        )
        .build()
    }
}