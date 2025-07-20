package com.ecarplay.iptv

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MediaController(private val context: Context) {
    
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onStateChangedCallback: ((Boolean) -> Unit)? = null
    
    companion object {
        private const val TAG = "MediaController"
    }
    
    fun setOnErrorCallback(callback: (String) -> Unit) {
        onErrorCallback = callback
    }
    
    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        onStateChangedCallback = callback
    }
    
    fun startPlayback(streamUrl: String) {
        Log.d(TAG, "Starting playback for URL: $streamUrl")
        stopPlayback()
        
        try {
            exoPlayer = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(streamUrl)
            
            exoPlayer?.apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        val errorMessage = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed"
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection timeout"
                            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "Invalid stream format"
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Malformed stream"
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Stream not found"
                            else -> "Playback failed: ${error.message}"
                        }
                        onErrorCallback?.invoke(errorMessage)
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Playback state changed: $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player ready - stream is playing")
                                onStateChangedCallback?.invoke(true)
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Playback ended")
                                onStateChangedCallback?.invoke(false)
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Buffering stream...")
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "Player idle")
                            }
                        }
                    }
                    
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        this@MediaController.isPlaying = isPlaying
                        Log.d(TAG, "Is playing changed: $isPlaying")
                    }
                })
                
                setMediaItem(mediaItem)
                prepare()
                play()
            }
            
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            onErrorCallback?.invoke("Failed to start playback: ${e.message}")
        }
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
    
    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }
}