package com.carplayer.iptv

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MediaController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun startPlayback(url: String) {
        exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer?.let {
            val mediaItem = MediaItem.fromUri(url)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
            it.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    stateChangedCallback?.invoke(isPlaying)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    errorCallback?.invoke(error.message ?: "Unknown error")
                }
            })
        }
    }

    fun stopPlayback() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun getVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return currentVolume.toFloat() / maxVolume.toFloat()
    }

    fun setVolume(volume: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (volume * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        Log.d("MediaController", "Setting volume to $volume")
    }

    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }
}
