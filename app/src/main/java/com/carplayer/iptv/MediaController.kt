package com.carplayer.iptv

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class MediaController(private val context: Context) {
    
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var isPlaying = false
    private var currentStreamUrl: String? = null
    private var retryCount = 0
    private val maxRetries = 3
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
        currentStreamUrl = streamUrl
        retryCount = 0
        startPlaybackInternal(streamUrl)
    }
    
    private fun startPlaybackInternal(streamUrl: String) {
        Log.d(TAG, "Starting playback internal for URL: $streamUrl (attempt ${retryCount + 1})")
        stopPlayback()
        
        try {
            // Create data source factory for HTTP streams with better IPTV compatibility
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("AppleCoreMedia/1.0.0.20H71 (iPhone; U; CPU OS 16_7_1 like Mac OS X; en_us)")
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
            
            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Configure better buffering for IPTV
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5000,   // Min buffer before playback starts
                    25000,  // Max buffer
                    1500,   // Buffer for playback
                    3000    // Buffer for playback after rebuffer
                )
                .build()
            
            // Configure track selector
            trackSelector = DefaultTrackSelector(context)
            
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector!!)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                .build()
            
            val mediaItem = MediaItem.fromUri(streamUrl)
            
            exoPlayer?.apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        
                        // Check if this is a retryable error
                        val isRetryableError = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> true
                            else -> false
                        }
                        
                        if (isRetryableError && retryCount < maxRetries && currentStreamUrl != null) {
                            retryCount++
                            Log.w(TAG, "Retrying playback (attempt $retryCount/$maxRetries) due to: ${error.message}")
                            
                            // Wait before retrying
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                startPlaybackInternal(currentStreamUrl!!)
                            }, (2000 * retryCount).toLong())
                        } else {
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
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Playback state changed: $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player ready - stream is playing")
                                retryCount = 0  // Reset retry count on successful playback
                                logAudioInfo()
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
    
    private fun logAudioInfo() {
        exoPlayer?.let { player ->
            try {
                val audioFormat = player.audioFormat
                if (audioFormat != null) {
                    Log.d(TAG, "Audio format - Sample rate: ${audioFormat.sampleRate}Hz, " +
                            "Channels: ${audioFormat.channelCount}, " +
                            "Encoding: ${audioFormat.pcmEncoding}")
                }
                
                // Log current tracks
                val currentTracks = player.currentTracks
                for (trackGroup in currentTracks.groups) {
                    if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until trackGroup.length) {
                            val format = trackGroup.getTrackFormat(i)
                            Log.d(TAG, "Available audio track $i: ${format.sampleMimeType}, " +
                                    "channels: ${format.channelCount}, " +
                                    "sample rate: ${format.sampleRate}Hz, " +
                                    "language: ${format.language ?: "Unknown"}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log audio info: ${e.message}")
            }
        }
    }
    
    // Function to manually select audio track if needed
    fun selectAudioTrack(trackIndex: Int) {
        trackSelector?.let { selector ->
            try {
                val parametersBuilder = selector.buildUponParameters()
                    .setPreferredAudioLanguage("en")
                    .setMaxAudioBitrate(Int.MAX_VALUE)
                selector.parameters = parametersBuilder.build()
                Log.d(TAG, "Updated track selection parameters")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update track selection: ${e.message}")
            }
        }
    }
}