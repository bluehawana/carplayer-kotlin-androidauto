package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.core.graphics.drawable.IconCompat
import com.carplayer.iptv.models.Channel

class PlayerScreen(carContext: CarContext, private val channel: Channel, private val allChannels: List<Channel> = emptyList()) : Screen(carContext) {
    
    private val mediaController = MediaController(carContext)
    private var playbackStatus = "Starting playback..."
    private var hasError = false
    private var isMuted = false
    private var isPaused = false
    private var currentChannelIndex = allChannels.indexOf(channel)
    
    override fun onGetTemplate(): Template {
        val statusIcon = when {
            isMuted -> "🔇"
            isPaused -> "⏸️"
            else -> "🎵"
        }
        
        val itemListBuilder = ItemList.Builder()
        
        // Add control rows
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("⏮️ Previous Channel")
                .addText(if (currentChannelIndex > 0) "Go to previous channel" else "At first channel")
                .setOnClickListener { previousChannel() }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_previous_channel)
                    ).build()
                )
                .build()
        )
        
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle(if (isPaused) "▶️ Play" else "⏸️ Pause")
                .addText(if (isPaused) "Resume playback" else "Pause playback")
                .setOnClickListener { togglePlayPause() }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, 
                            if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_play_pause)
                    ).build()
                )
                .build()
        )
        
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("⏭️ Next Channel")
                .addText(if (currentChannelIndex < allChannels.size - 1) "Go to next channel" else "At last channel")
                .setOnClickListener { nextChannel() }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_skip_next)
                    ).build()
                )
                .build()
        )
        
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle(if (isMuted) "🔊 Unmute" else "🔇 Mute")
                .addText(if (isMuted) "Restore audio" else "Mute audio")
                .setOnClickListener { toggleMute() }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext,
                            if (isMuted) R.drawable.ic_volume_mute else R.drawable.ic_volume)
                    ).build()
                )
                .build()
        )
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle("$statusIcon ${channel.name}")
            .setHeaderAction(Action.BACK)
            .build()
    }
    
    init {
        android.util.Log.d("PlayerScreen", "Initializing player for channel: ${channel.name}")
        android.util.Log.d("PlayerScreen", "Stream URL: ${channel.streamUrl}")
        
        mediaController.setOnErrorCallback { error ->
            android.util.Log.e("PlayerScreen", "Playback error: $error")
            hasError = true
            playbackStatus = error
            invalidate()
        }
        
        mediaController.setOnStateChangedCallback { isReady ->
            android.util.Log.d("PlayerScreen", "State changed - isReady: $isReady")
            playbackStatus = if (isReady) "🎵 Neural stream >> car matrix" else "⏸️ Ice stream frozen"
            hasError = false
            invalidate()
        }
        
        // Validate stream URL before attempting playback
        if (channel.streamUrl.isBlank()) {
            hasError = true
            playbackStatus = "❄️ No neural matrix path found"
            android.util.Log.e("PlayerScreen", "Empty stream URL for channel: ${channel.name}")
        } else {
            playbackStatus = "⚡ Hacking ice stream matrix..."
            mediaController.startPlayback(channel.streamUrl)
        }
    }
    
    private fun previousChannel() {
        if (allChannels.isNotEmpty() && currentChannelIndex > 0) {
            currentChannelIndex--
            val prevChannel = allChannels[currentChannelIndex]
            playbackStatus = "⚡ Ice matrix << rewinding to ${prevChannel.name}..."
            hasError = false
            invalidate()
            
            // Switch to previous channel
            screenManager.push(PlayerScreen(carContext, prevChannel, allChannels))
        } else {
            playbackStatus = "❄️ Ice terminal start reached"
            invalidate()
        }
    }
    
    private fun nextChannel() {
        if (allChannels.isNotEmpty() && currentChannelIndex < allChannels.size - 1) {
            currentChannelIndex++
            val nextChannel = allChannels[currentChannelIndex]
            playbackStatus = "⚡ Ice matrix >> advancing to ${nextChannel.name}..."
            hasError = false
            invalidate()
            
            // Switch to next channel
            screenManager.push(PlayerScreen(carContext, nextChannel, allChannels))
        } else {
            playbackStatus = "❄️ Ice terminal end reached"
            invalidate()
        }
    }
    
    private fun togglePlayPause() {
        isPaused = !isPaused
        mediaController.togglePlayPause()
        playbackStatus = if (isPaused) "⏸️ Ice stream frozen" else "⚡ Neural stream active"
        invalidate()
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        val currentVolume = mediaController.getVolume()
        if (isMuted) {
            mediaController.setVolume(0.0f)
            playbackStatus = "🔇 Neural audio severed"
        } else {
            mediaController.setVolume(if (currentVolume == 0.0f) 1.0f else currentVolume)
            playbackStatus = "🔊 Ice audio matrix restored"
        }
        invalidate()
    }
}