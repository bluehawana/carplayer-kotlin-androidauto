package com.ecarplay.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.model.MessageTemplate
import androidx.core.graphics.drawable.IconCompat
import com.ecarplay.iptv.models.Channel

class PlayerScreen(carContext: CarContext, private val channel: Channel) : Screen(carContext) {
    
    private val mediaController = MediaController(carContext)
    private var playbackStatus = "Starting playback..."
    private var hasError = false
    
    override fun onGetTemplate(): Template {
        val statusMessage = if (hasError) {
            "❌ Error: ${channel.name}\n$playbackStatus\n\n💡 Try:\n• Check internet connection\n• Use mobile data\n• Try different channel"
        } else {
            "📺 ${channel.name}\n$playbackStatus\n\n❄️ Nordic IPTV Player\n🚗 Audio playing through car speakers\n📱 Tap 'Video' to watch on phone"
        }
        
        return MessageTemplate.Builder(statusMessage)
            .setTitle("🎬 ECarTV Nordic")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Video")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_tv)
                                ).build()
                            )
                            .setOnClickListener {
                                // Launch video player on phone
                                val intent = android.content.Intent(carContext, VideoPlayerActivity::class.java).apply {
                                    putExtra("CHANNEL_NAME", channel.name)
                                    putExtra("STREAM_URL", channel.streamUrl)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                carContext.startActivity(intent)
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Audio")
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
                                // Simple volume control without separate screen
                                val currentVolume = mediaController.getVolume()
                                val newVolume = if (currentVolume > 0.5f) 0.3f else 1.0f
                                mediaController.setVolume(newVolume)
                                playbackStatus = "Volume: ${(newVolume * 100).toInt()}%"
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
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
            playbackStatus = if (isReady) "🎵 Audio streaming to car" else "⏸️ Audio paused"
            hasError = false
            invalidate()
        }
        
        // Validate stream URL before attempting playback
        if (channel.streamUrl.isBlank()) {
            hasError = true
            playbackStatus = "No stream URL available"
            android.util.Log.e("PlayerScreen", "Empty stream URL for channel: ${channel.name}")
        } else {
            playbackStatus = "Connecting..."
            mediaController.startPlayback(channel.streamUrl)
        }
    }
}