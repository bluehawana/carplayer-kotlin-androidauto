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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.ui.PlayerView

@UnstableApi
class ExoPlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val networkBalancer = NetworkBalancer(context)
    
    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "ExoPlayerController"
    }

    init {
        Log.d(TAG, "Initializing ExoPlayer for IPTV")
        setupExoPlayer()
    }

    private fun setupExoPlayer() {
        try {
            // Create track selector with enhanced audio track selection for sports streams
            trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    // AUDIO OPTIMIZATION FOR SKY SPORTS F1 AND SIMILAR STREAMS
                    .setPreferredAudioLanguage("en") // Prefer English audio tracks
                    .setMaxAudioBitrate(320000) // High-quality audio bitrate (320 kbps)
                    .setForceHighestSupportedBitrate(false) // Don't force highest to maintain stability
                    .setTunnelingEnabled(true) // Enable audio tunneling for better performance
                    .setAllowAudioMixedMimeTypeAdaptiveness(true) // Handle mixed audio formats
                    .setAllowAudioMixedSampleRateAdaptiveness(true) // Handle different sample rates
                    .setAllowAudioMixedChannelCountAdaptiveness(true) // Handle mono/stereo switching
                    .build()
            }
            
            // Create ExoPlayer with optimized settings for IPTV and enhanced track selector
            exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector!!)
                .build()
                .apply {
                    // Enhanced audio attributes specifically for sports commentary
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .setFlags(C.FLAG_AUDIBILITY_ENFORCED) // Force audio to be audible
                            .build(),
                        true // Handle audio focus
                    )
                    
                    // Set volume to maximum to ensure audio is heard
                    setVolume(1.0f)
                    
                    // Add player listener for events including track selection
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
                                    errorCallback?.invoke("") // Clear buffering message
                                    stateChangedCallback?.invoke(isPlaying)
                                    
                                    // Check and log audio tracks for debugging
                                    logAudioTrackInfo()
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
                        
                        override fun onTracksChanged(tracks: Tracks) {
                            Log.d(TAG, "ExoPlayer: Available tracks changed")
                            handleAudioTrackSelection(tracks)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "ExoPlayer error: ${error.message}", error)
                            
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
    
    private fun logAudioTrackInfo() {
        try {
            exoPlayer?.let { player ->
                val tracks = player.currentTracks
                Log.d(TAG, "Current audio tracks info:")
                
                for (trackGroup in tracks.groups) {
                    if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                        val format = trackGroup.getTrackFormat(0)
                        Log.d(TAG, "Audio track: ${format.sampleMimeType}, " +
                                "channels: ${format.channelCount}, " +
                                "sample rate: ${format.sampleRate}, " +
                                "bitrate: ${format.bitrate}, " +
                                "language: ${format.language}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging audio track info", e)
        }
    }
    
    private fun handleAudioTrackSelection(tracks: Tracks) {
        try {
            Log.d(TAG, "Analyzing available audio tracks for optimal selection")
            
            // Find the best audio track for Sky Sports F1 and similar streams
            for (trackGroup in tracks.groups) {
                if (trackGroup.type == C.TRACK_TYPE_AUDIO && trackGroup.length > 0) {
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getTrackFormat(i)
                        
                        Log.d(TAG, "Available audio track $i: " +
                                "mime: ${format.sampleMimeType}, " +
                                "channels: ${format.channelCount}, " +
                                "sample rate: ${format.sampleRate}, " +
                                "bitrate: ${format.bitrate}, " +
                                "language: ${format.language}")
                        
                        // Sky Sports F1 specific: prefer AAC/MP3 tracks with good bitrates
                        if (shouldSelectAudioTrack(format)) {
                            Log.d(TAG, "Selecting optimal audio track: $i for sports stream")
                            
                            trackSelector?.let { selector ->
                                val override = TrackSelectionOverride(
                                    trackGroup.mediaTrackGroup, 
                                    listOf(i)
                                )
                                
                                selector.parameters = selector.buildUponParameters()
                                    .addOverride(override)
                                    .build()
                                
                                Log.d(TAG, "Audio track override applied successfully")
                            }
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling audio track selection", e)
        }
    }
    
    private fun shouldSelectAudioTrack(format: androidx.media3.common.Format): Boolean {
        // Prefer tracks with the following characteristics for Sky Sports F1:
        return when {
            // Prefer stereo tracks (2 channels) over mono
            format.channelCount >= 2 -> {
                Log.d(TAG, "Stereo/multi-channel track found - prioritizing")
                true
            }
            // Prefer AAC or MP3 formats which are common in sports streams
            format.sampleMimeType?.contains("aac", ignoreCase = true) == true ||
            format.sampleMimeType?.contains("mp3", ignoreCase = true) == true -> {
                Log.d(TAG, "AAC/MP3 audio track found - good for sports streams")
                true
            }
            // Prefer tracks with reasonable bitrates (not too low, not unnecessarily high)
            format.bitrate in 64000..320000 -> {
                Log.d(TAG, "Good bitrate audio track found: ${format.bitrate}")
                true
            }
            // Prefer English tracks or tracks without language specification
            format.language == null || 
            format.language == "en" || 
            format.language == "eng" -> {
                Log.d(TAG, "English/unspecified language track - suitable for Sky Sports")
                true
            }
            else -> false
        }
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
        try {
            // Special audio handling for Sky Sports F1 streams
            if (url.contains("354945") || (url.lowercase().contains("sky") && url.lowercase().contains("f1"))) {
                Log.d(TAG, "Sky Sports F1 stream detected - applying ExoPlayer audio fixes")
                
                // Force audio track selection parameters for sports streams
                trackSelector?.let { selector ->
                    selector.parameters = selector.buildUponParameters()
                        .setPreferredAudioLanguage("en") // English commentary
                        .setMaxAudioBitrate(320000) // High quality audio
                        .setForceHighestSupportedBitrate(false) // But don't force highest to maintain stability
                        .setTunnelingEnabled(true) // Enable tunneling for better sync
                        .build()
                }
                
                // Ensure maximum volume for Sky Sports streams (commentary is often quiet)
                exoPlayer?.setVolume(1.0f)
            }
            
            val mediaSource = createMediaSource(url)
            
            exoPlayer?.apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
            
            Log.d(TAG, "ExoPlayer playback started with optimized audio settings")
            
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

        // Enhanced handling for Sky Sports F1 and other sports streams with audio issues
        if (url.contains("354945") || (url.lowercase().contains("sky") && url.lowercase().contains("f1")) ||
            url.contains("skysports") || url.contains("eurosport")) {
            Log.d(TAG, "Sports stream detected - applying ExoPlayer audio/network optimizations")
            
            // Use specific user agent and headers that work better with sports streams
            httpDataSourceFactory.setUserAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
            
            // Add headers that help with sports stream audio delivery
            httpDataSourceFactory.setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Accept-Encoding" to "identity", // Disable compression for better audio stream handling
                "Accept-Language" to "en-US,en;q=0.9",
                "Cache-Control" to "no-cache",
                "Connection" to "keep-alive",
                "Pragma" to "no-cache"
            ))
            
            // Increase timeouts for sports streams which can be slower to establish audio
            httpDataSourceFactory
                .setConnectTimeoutMs(15000) // 15 second connect timeout
                .setReadTimeoutMs(30000) // 30 second read timeout
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