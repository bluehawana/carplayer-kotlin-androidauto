package com.carplayer.iptv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

@UnstableApi
class ExoPlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private val networkBalancer = NetworkBalancer(context)
    
    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null
    private var currentUrl: String = ""
    private var retryCount = 0
    private val maxRetries = 3

    companion object {
        private const val TAG = "ExoPlayerController"
    }

    init {
        Log.d(TAG, "Initializing ExoPlayer for IPTV")
        setupExoPlayer()
    }

    private fun setupExoPlayer() {
        try {
            // Create ExoPlayer with optimized settings for IPTV
            exoPlayer = ExoPlayer.Builder(context)
                .build()
                .apply {
                    // Set audio attributes for better audio handling
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        true // Handle audio focus
                    )
                    
                    // Add player listener for events
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            Log.d(TAG, "ExoPlayer state changed: $playbackState")
                            when (playbackState) {
                                Player.STATE_IDLE -> {
                                    Log.d(TAG, "ExoPlayer: IDLE")
                                }
                                Player.STATE_BUFFERING -> {
                                    Log.d(TAG, "ExoPlayer: BUFFERING")
                                    errorCallback?.invoke("❄️ Buffering...")
                                }
                                Player.STATE_READY -> {
                                    Log.d(TAG, "ExoPlayer: READY - playback can start")
                                    retryCount = 0 // Reset retries on success
                                    errorCallback?.invoke("") // Clear buffering message
                                    stateChangedCallback?.invoke(isPlaying)
                                }
                                Player.STATE_ENDED -> {
                                    Log.d(TAG, "ExoPlayer: ENDED")
                                    stateChangedCallback?.invoke(false)
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d(TAG, "ExoPlayer: Playing state changed to $isPlaying")
                            stateChangedCallback?.invoke(isPlaying)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "ExoPlayer error: ${error.message}", error)

                            // Auto-retry on network errors (common on emulator)
                            val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                                error.cause is java.net.UnknownHostException ||
                                error.cause?.cause is java.net.UnknownHostException

                            if (isNetworkError && retryCount < maxRetries && currentUrl.isNotEmpty()) {
                                retryCount++
                                Log.d(TAG, "Network error - auto-retry $retryCount/$maxRetries in 2s")
                                errorCallback?.invoke("❄️ Connecting... (attempt $retryCount/$maxRetries)")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    startPlayback(currentUrl)
                                }, 2000L)
                                return
                            }

                            // Provide specific error messages
                            val errorMessage = when (error.errorCode) {
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                    "❄️ Network connection failed - check internet"
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                    "❄️ Connection timeout - trying to reconnect..."
                                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                                    "❄️ Stream format issue - switching to VLC fallback..."
                                else -> "❄️ Playback error: ${error.message}"
                            }

                            retryCount = 0
                            errorCallback?.invoke(errorMessage)
                        }
                    })
                }
            
            Log.d(TAG, "ExoPlayer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer", e)
            errorCallback?.invoke("ExoPlayer initialization failed: ${e.message}")
        }
    }

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun createVideoSurface(container: FrameLayout) {
        Log.d(TAG, "Creating ExoPlayer video surface")
        try {
            playerView = PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                useController = false // Disable built-in controls for automotive
                player = exoPlayer
            }
            
            container.removeAllViews() // Clear any existing views
            container.addView(playerView)
            
            Log.d(TAG, "ExoPlayer video surface created and attached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video surface", e)
            errorCallback?.invoke("Failed to create video surface: ${e.message}")
        }
    }

    fun startPlayback(url: String) {
        Log.d(TAG, "Starting ExoPlayer playback for: $url")
        currentUrl = url
        try {
            val mediaSource = createMediaSource(url)

            exoPlayer?.apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }

            Log.d(TAG, "ExoPlayer playback started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            errorCallback?.invoke("ExoPlayer playback failed: ${e.message}")
        }
    }

    private fun createMediaSource(url: String): MediaSource {
        val uri = Uri.parse(url)
        
        // Get network-optimized settings
        val currentNetwork = networkBalancer.getCurrentNetworkType()
        val streamingProfile = networkBalancer.getOptimalStreamingProfile(currentNetwork)
        
        Log.d(TAG, "Using streaming profile: ${streamingProfile.description}")
        if (currentNetwork?.isHotspot == true) {
            errorCallback?.invoke("📱 Hotspot detected - optimizing for cellular")
        } else if (currentNetwork?.type == "Mobile Data") {
            errorCallback?.invoke("📱 Mobile data - using optimized settings")
        }
        
        // Create HTTP data source with network-adaptive settings
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ExoPlayer-IPTV/1.0")
            .setConnectTimeoutMs(streamingProfile.connectTimeoutMs)
            .setReadTimeoutMs(streamingProfile.readTimeoutMs)
            .setAllowCrossProtocolRedirects(true)

        // Special handling for Sky Sports F1 and other problematic streams
        if (url.contains("354945") || (url.lowercase().contains("sky") && url.lowercase().contains("f1"))) {
            Log.d(TAG, "Sky Sports F1 detected - applying ExoPlayer audio optimizations")
            
            // Use specific user agent that works better with Sky streams
            httpDataSourceFactory.setUserAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
        }
        
        // Additional headers for cellular/hotspot connections
        if (currentNetwork?.isHotspot == true || currentNetwork?.type == "Mobile Data") {
            // These headers help with some IPTV providers on cellular
            httpDataSourceFactory.setDefaultRequestProperties(mapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache",
                "Accept-Encoding" to "identity"
            ))
        }

        return when {
            url.contains(".m3u8") -> {
                // HLS stream - most IPTV streams are HLS
                Log.d(TAG, "Creating HLS media source for: $url")
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            else -> {
                // Progressive stream fallback
                Log.d(TAG, "Creating progressive media source for: $url")
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping ExoPlayer playback")
        try {
            exoPlayer?.apply {
                stop()
                clearMediaItems()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }

    fun togglePlayPause() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    Log.d(TAG, "ExoPlayer: Pausing playback")
                    player.pause()
                } else {
                    Log.d(TAG, "ExoPlayer: Resuming playback")
                    player.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling playback", e)
        }
    }

    fun isPlaying(): Boolean {
        return try {
            exoPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking playing state", e)
            false
        }
    }

    fun release() {
        Log.d(TAG, "Releasing ExoPlayer resources")
        try {
            exoPlayer?.release()
            playerView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ExoPlayer resources", e)
        }
    }
}