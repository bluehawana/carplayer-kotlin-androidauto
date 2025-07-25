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
import androidx.media3.common.util.UnstableApi
import com.carplayer.iptv.models.Channel

@UnstableApi
class PlayerScreen(carContext: CarContext, private val channel: Channel, private val allChannels: List<Channel> = emptyList()) : Screen(carContext) {
    
    
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
        
        // Create action strip with Ice Age styled channel navigation
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("⏮️ Previous")
                    .setOnClickListener { previousChannel() }
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_previous_channel)
                        ).build()
                    )
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isPaused) "▶️ Play" else "⏸️ Pause")
                    .setOnClickListener { togglePlayPause() }
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, 
                                if (isPaused) R.drawable.ic_play_nordic else R.drawable.ic_pause_nordic)
                        ).build()
                    )
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("⏭️ Next")
                    .setOnClickListener { nextChannel() }
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_next_channel)
                        ).build()
                    )
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isMuted) "🔊 Unmute" else "🔇 Mute")
                    .setOnClickListener { toggleMute() }
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext,
                                if (isMuted) R.drawable.ic_volume_nordic else R.drawable.ic_mute_nordic)
                        ).build()
                    )
                    .build()
            )
            .build()

        // Create a message template instead of list template for better media control
        return MessageTemplate.Builder("$statusIcon ${channel.name}")
            .setTitle("❄️ $playbackStatus\n\n🎬 Use the controls below to navigate channels")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .addAction(
                Action.Builder()
                    .setTitle("⏮️ Previous Channel")
                    .setOnClickListener { previousChannel() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isPaused) "▶️ Play" else "⏸️ Pause")
                    .setOnClickListener { togglePlayPause() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("⏭️ Next Channel")
                    .setOnClickListener { nextChannel() }
                    .build()
            )
            .build()
    }
    
    init {
        android.util.Log.d("PlayerScreen", "Initializing player for channel: ${channel.name}")
        android.util.Log.d("PlayerScreen", "Stream URL: ${channel.streamUrl}")
        
        // BYPASS Android Auto's media session - launch VideoPlayerActivity directly
        playbackStatus = "❄️ Launching Ice Video Player..."
        
        try {
            val channelNames = allChannels.map { it.name }
            val channelUrls = allChannels.map { it.streamUrl }
            val currentIndex = allChannels.indexOf(channel)
            
            val intent = android.content.Intent(carContext, VideoPlayerActivity::class.java).apply {
                putExtra("CHANNEL_NAME", channel.name)
                putExtra("STREAM_URL", channel.streamUrl)
                putStringArrayListExtra("ALL_CHANNELS", ArrayList(channelNames))
                putStringArrayListExtra("ALL_CHANNEL_URLS", ArrayList(channelUrls))
                putExtra("CURRENT_INDEX", currentIndex)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            carContext.startActivity(intent)
            playbackStatus = "🎬 Ice Video Player launched"
            
        } catch (e: Exception) {
            android.util.Log.e("PlayerScreen", "Failed to launch VideoPlayerActivity", e)
            playbackStatus = "❄️ Error launching video player"
            hasError = true
        }
        
        invalidate()
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
        playbackStatus = if (isPaused) "⏸️ Ice stream frozen" else "⚡ Neural stream active"
        invalidate()
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        playbackStatus = if (isMuted) "🔇 Neural audio severed" else "🔊 Ice audio matrix restored"
        invalidate()
    }
}