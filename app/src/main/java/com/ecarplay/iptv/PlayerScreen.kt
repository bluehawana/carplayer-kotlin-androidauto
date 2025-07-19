package com.ecarplay.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.model.MessageTemplate
import androidx.core.graphics.drawable.IconCompat

class PlayerScreen(carContext: CarContext, private val channel: Channel) : Screen(carContext) {
    
    private val mediaController = MediaController(carContext)
    
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Now Playing: ${channel.name}")
            .setTitle("IPTV Player")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Play/Pause")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_play_pause)
                                ).build()
                            )
                            .setOnClickListener {
                                mediaController.togglePlayPause()
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Volume")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_volume)
                                ).build()
                            )
                            .setOnClickListener {
                                screenManager.push(VolumeControlScreen(carContext))
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Back")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_back)
                                ).build()
                            )
                            .setOnClickListener {
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    init {
        mediaController.startPlayback(channel.streamUrl)
    }
}