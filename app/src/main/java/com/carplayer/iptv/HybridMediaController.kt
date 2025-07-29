package com.carplayer.iptv

import android.content.Context
import android.util.Log
import android.widget.FrameLayout

/**
 * Hybrid Media Controller - Uses ExoPlayer as primary, VLC as fallback
 * This is the approach used by TiviMate, IPTV Smarters, and other successful IPTV players
 */
class HybridMediaController(private val context: Context) {

    private var exoPlayerController: ExoPlayerController? = null
    private var vlcMediaController: VlcMediaController? = null
    private var currentEngine = MediaEngine.EXOPLAYER
    private var currentUrl = ""
    private var videoContainer: FrameLayout? = null
    
    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    enum class MediaEngine {
        EXOPLAYER,
        VLC
    }

    companion object {
        private const val TAG = "HybridMediaController"
    }

    init {
        Log.d(TAG, "Initializing Hybrid Media Controller (ExoPlayer + VLC fallback)")
        setupControllers()
    }

    private fun setupControllers() {
        try {
            // Initialize ExoPlayer as primary
            exoPlayerController = ExoPlayerController(context).apply {
                setOnErrorCallback { message ->
                    if (message.contains("Playback error") || message.contains("format issue")) {
                        Log.w(TAG, "ExoPlayer failed, switching to VLC: $message")
                        switchToVlcFallback()
                    } else {
                        errorCallback?.invoke(message)
                    }
                }
                setOnStateChangedCallback { isPlaying ->
                    stateChangedCallback?.invoke(isPlaying)
                }
            }

            // Initialize VLC as fallback
            vlcMediaController = VlcMediaController(context).apply {
                setOnErrorCallback { message ->
                    errorCallback?.invoke(message)
                }
                setOnStateChangedCallback { isPlaying ->
                    stateChangedCallback?.invoke(isPlaying)
                }
            }

            Log.d(TAG, "Both media controllers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize media controllers", e)
            errorCallback?.invoke("Media controller initialization failed: ${e.message}")
        }
    }

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun createVideoSurface(container: FrameLayout) {
        Log.d(TAG, "Creating video surface for current engine: $currentEngine")
        videoContainer = container
        
        when (currentEngine) {
            MediaEngine.EXOPLAYER -> exoPlayerController?.createVideoSurface(container)
            MediaEngine.VLC -> vlcMediaController?.createVideoSurface(container)
        }
    }

    fun startPlayback(url: String) {
        Log.d(TAG, "Starting playback with $currentEngine for: $url")
        currentUrl = url
        
        // Special handling for Sky Sports F1 - try ExoPlayer first with optimizations
        if (url.contains("354945") || (url.lowercase().contains("sky") && url.lowercase().contains("f1"))) {
            Log.d(TAG, "Sky Sports F1 detected - using ExoPlayer with audio optimizations")
            errorCallback?.invoke("🏎️ Optimizing Sky Sports F1 audio with ExoPlayer...")
            currentEngine = MediaEngine.EXOPLAYER
        }
        
        when (currentEngine) {
            MediaEngine.EXOPLAYER -> {
                exoPlayerController?.startPlayback(url)
            }
            MediaEngine.VLC -> {
                vlcMediaController?.startPlayback(url)
            }
        }
    }

    private fun switchToVlcFallback() {
        Log.d(TAG, "Switching from ExoPlayer to VLC fallback")
        
        try {
            // Stop ExoPlayer
            exoPlayerController?.stopPlayback()
            
            // Switch to VLC
            currentEngine = MediaEngine.VLC
            errorCallback?.invoke("❄️ Switching to VLC for better compatibility...")
            
            // Recreate video surface for VLC
            videoContainer?.let { container ->
                vlcMediaController?.createVideoSurface(container)
            }
            
            // Start playback with VLC
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                vlcMediaController?.startPlayback(currentUrl)
            }, 1000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to VLC fallback", e)
            errorCallback?.invoke("Failed to switch to VLC: ${e.message}")
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping playback on current engine: $currentEngine")
        when (currentEngine) {
            MediaEngine.EXOPLAYER -> exoPlayerController?.stopPlayback()
            MediaEngine.VLC -> vlcMediaController?.stopPlayback()
        }
    }

    fun togglePlayPause() {
        when (currentEngine) {
            MediaEngine.EXOPLAYER -> exoPlayerController?.togglePlayPause()
            MediaEngine.VLC -> vlcMediaController?.togglePlayPause()
        }
    }

    fun isPlaying(): Boolean {
        return when (currentEngine) {
            MediaEngine.EXOPLAYER -> exoPlayerController?.isPlaying() ?: false
            MediaEngine.VLC -> vlcMediaController?.isPlaying() ?: false
        }
    }

    fun getCurrentEngine(): MediaEngine {
        return currentEngine
    }

    fun forceVlcEngine() {
        Log.d(TAG, "Forcing VLC engine for current stream")
        if (currentEngine == MediaEngine.EXOPLAYER) {
            switchToVlcFallback()
        }
    }

    fun forceExoPlayerEngine() {
        Log.d(TAG, "Forcing ExoPlayer engine for current stream")
        if (currentEngine == MediaEngine.VLC) {
            try {
                // Stop VLC
                vlcMediaController?.stopPlayback()
                
                // Switch to ExoPlayer
                currentEngine = MediaEngine.EXOPLAYER
                errorCallback?.invoke("❄️ Switching to ExoPlayer...")
                
                // Recreate video surface for ExoPlayer
                videoContainer?.let { container ->
                    exoPlayerController?.createVideoSurface(container)
                }
                
                // Start playback with ExoPlayer
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    exoPlayerController?.startPlayback(currentUrl)
                }, 1000)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to ExoPlayer", e)
                errorCallback?.invoke("Failed to switch to ExoPlayer: ${e.message}")
            }
        }
    }

    fun release() {
        Log.d(TAG, "Releasing all media controller resources")
        try {
            exoPlayerController?.release()
            vlcMediaController?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
}