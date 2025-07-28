package com.carplayer.iptv

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

class ExoPlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    
    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "ExoPlayerController"
    }

    init {
        Log.d(TAG, "Initializing ExoPlayer for IPTV")
        try {
            exoPlayer = ExoPlayer.Builder(context)
                .build()
                
            // Set up player listener for events
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            Log.d(TAG, "ExoPlayer: Ready to play")
                            stateChangedCallback?.invoke(true)
                        }
                        Player.STATE_BUFFERING -> {
                            Log.d(TAG, "ExoPlayer: Buffering")
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "ExoPlayer: Playback ended")
                            stateChangedCallback?.invoke(false)
                        }
                        Player.STATE_IDLE -> {
                            Log.d(TAG, "ExoPlayer: Idle")
                            stateChangedCallback?.invoke(false)
                        }
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "ExoPlayer error: ${error.message}")
                    errorCallback?.invoke("Stream error: ${error.message}")
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    stateChangedCallback?.invoke(isPlaying)
                }
            })
            
            Log.d(TAG, "ExoPlayer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer", e)
            errorCallback?.invoke("Player initialization failed: ${e.message}")
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
                useController = false // Hide default controls
                player = exoPlayer
            }
            
            container.addView(playerView)
            Log.d(TAG, "ExoPlayer video surface created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video surface", e)
            errorCallback?.invoke("Failed to create video surface: ${e.message}")
        }
    }

    fun startPlayback(url: String) {
        Log.d(TAG, "Starting ExoPlayer playback for: $url")
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            val mediaSource = createMediaSource(url, mediaItem)
            
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            
            // Special handling for Sky Sports F1 audio issues
            if (url.contains("35244") || url.toLowerCase().contains("sky") && url.toLowerCase().contains("f1")) {
                Log.d(TAG, "Sky Sports F1 detected - applying audio fixes")
                
                // Set audio attributes for sports content
                val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                exoPlayer?.setAudioAttributes(audioAttributes, true)
                
                // Force audio track selection after preparation
                exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        Log.d(TAG, "Sky F1: Tracks changed, checking audio tracks")
                        
                        for (trackGroup in tracks.groups) {
                            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                                Log.d(TAG, "Sky F1: Found audio track group with ${trackGroup.length} tracks")
                                
                                // Try to select the first available audio track
                                for (i in 0 until trackGroup.length) {
                                    if (trackGroup.isTrackSupported(i)) {
                                        Log.d(TAG, "Sky F1: Selecting audio track $i")
                                        val trackSelectionParameters = exoPlayer?.trackSelectionParameters
                                            ?.buildUpon()
                                            ?.setOverrideForType(
                                                androidx.media3.common.TrackSelectionOverride(
                                                    trackGroup.mediaTrackGroup,
                                                    i
                                                )
                                            )
                                            ?.build()
                                        
                                        trackSelectionParameters?.let { params ->
                                            exoPlayer?.trackSelectionParameters = params
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                })
            }
            
            exoPlayer?.play()
            Log.d(TAG, "ExoPlayer playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            errorCallback?.invoke("Playback failed: ${e.message}")
        }
    }
    
    private fun createMediaSource(url: String, mediaItem: MediaItem): MediaSource {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
            .setUserAgent("ExoPlayer-IPTV/1.0")
        
        return when {
            url.contains(".m3u8") -> {
                // HLS streams (most IPTV)
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                // Progressive streams
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping ExoPlayer playback")
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }

    fun togglePlayPause() {
        try {
            if (exoPlayer?.isPlaying == true) {
                Log.d(TAG, "ExoPlayer: Pausing playback")
                exoPlayer?.pause()
            } else {
                Log.d(TAG, "ExoPlayer: Resuming playback")
                exoPlayer?.play()
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