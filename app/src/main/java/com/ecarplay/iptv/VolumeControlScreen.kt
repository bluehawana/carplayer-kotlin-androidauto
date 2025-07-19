package com.ecarplay.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat

class VolumeControlScreen(carContext: CarContext) : Screen(carContext) {
    
    private val mediaController = MediaController(carContext)
    private var currentVolume = mediaController.getVolume()
    
    private fun showToast(message: String) {
        androidx.car.app.CarToast.makeText(carContext, message, androidx.car.app.CarToast.LENGTH_SHORT).show()
    }
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        // Volume control options
        val volumeOptions = listOf(
            "Volume Up" to 0.1f,
            "Volume Down" to -0.1f,
            "Mute" to 0f,
            "Max Volume" to 1f
        )
        
        volumeOptions.forEach { (title, volumeChange) ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(if (title == "Mute") "Set volume to 0%" else 
                            if (title == "Max Volume") "Set volume to 100%" else
                            "Current: ${(currentVolume * 100).toInt()}%")
                    .setOnClickListener {
                        adjustVolume(volumeChange, title == "Mute" || title == "Max Volume")
                    }
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext, 
                                when (title) {
                                    "Volume Up" -> R.drawable.ic_volume_up
                                    "Volume Down" -> R.drawable.ic_volume_down
                                    "Mute" -> R.drawable.ic_volume_mute
                                    else -> R.drawable.ic_volume_max
                                }
                            )
                        ).build()
                    )
                    .build()
            )
        }
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle("Volume Control")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Back")
                            .setOnClickListener {
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun adjustVolume(volumeChange: Float, isAbsolute: Boolean) {
        currentVolume = if (isAbsolute) {
            volumeChange
        } else {
            (currentVolume + volumeChange).coerceIn(0f, 1f)
        }
        
        mediaController.setVolume(currentVolume)
        
        // Show toast with current volume
        showToast("Volume: ${(currentVolume * 100).toInt()}%")
        
        // Refresh the screen to update the display
        invalidate()
    }
}