package com.ecarplay.iptv

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MediaController(private val context: Context) {
    
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    
    fun startPlayback(streamUrl: String) {
        stopPlayback()
        
        exoPlayer = ExoPlayer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(streamUrl)
        
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        
        isPlaying = true
    }
    
    fun stopPlayback() {
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
        isPlaying = false
    }
    
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
            } else {
                player.play()
                isPlaying = true
            }
        }
    }
    
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    fun getVolume(): Float {
        return exoPlayer?.volume ?: 0f
    }
    
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
    
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }
    
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }
    
    fun addListener(listener: Player.Listener) {
        exoPlayer?.addListener(listener)
    }
    
    fun removeListener(listener: Player.Listener) {
        exoPlayer?.removeListener(listener)
    }
}