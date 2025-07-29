package com.carplayer.iptv

import android.content.Context
import android.util.Log
import android.widget.FrameLayout

class HybridMediaController(private val context: Context) {

    private var exoPlayerController: ExoPlayerController? = null
    private var vlcMediaController: VlcMediaController? = null
    private var currentEngine = PlayerEngine.EXOPLAYER
    private var currentUrl = ""
    
    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    enum class PlayerEngine {
        EXOPLAYER, VLC
    }

    companion object {
        private const val TAG = "HybridMediaController"
    }

    init {
        Log.d(TAG, "Initializing Hybrid Media Controller (ExoPlayer + VLC fallback)")
        initializeExoPlayer()
    }

    private fun initializeExoPlayer() {
        try {
            exoPlayerController = ExoPlayerController(context)
            
            exoPlayerController?.setOnErrorCallback { error ->
                Log.w(TAG, "ExoPlayer error: $error - trying VLC fallback")
                switchToVLC()
            }
            
            exoPlayerController?.setOnStateChangedCallback { isPlaying ->
                stateChangedCallback?.invoke(isPlaying)
            }
            
            currentEngine = PlayerEngine.EXOPLAYER
            Log.d(TAG, "ExoPlayer initialized as primary engine")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer, switching to VLC", e)
            switchToVLC()
        }
    }

    private fun switchToVLC() {
        Log.d(TAG, "Switching to VLC player engine")
        try {
            if (vlcMediaController == null) {
                vlcMediaController = VlcMediaController(context)
                
                vlcMediaController?.setOnErrorCallback { error ->
                    Log.e(TAG, "VLC error: $error")
                    errorCallback?.invoke("❄️ Both players failed: $error")
                }
                
                vlcMediaController?.setOnStateChangedCallback { isPlaying ->
                    stateChangedCallback?.invoke(isPlaying)
                }
            }
            
            currentEngine = PlayerEngine.VLC
            
            // If we have a current URL, restart playback with VLC
            if (currentUrl.isNotEmpty()) {
                Log.d(TAG, "Restarting playback with VLC for: $currentUrl")
                vlcMediaController?.startPlayback(currentUrl)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC fallback", e)
            errorCallback?.invoke("❄️ All players failed: ${e.message}")
        }
    }

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun createVideoSurface(container: FrameLayout) {
        Log.d(TAG, "Creating video surface for $currentEngine")
        when (currentEngine) {
            PlayerEngine.EXOPLAYER -> exoPlayerController?.createVideoSurface(container)
            PlayerEngine.VLC -> vlcMediaController?.createVideoSurface(container)
        }
    }

    @androidx.media3.common.util.UnstableApi
    fun startPlayback(url: String) {
        Log.d(TAG, "Starting playback with $currentEngine for: $url")
        currentUrl = url
        
        when (currentEngine) {
            PlayerEngine.EXOPLAYER -> {
                exoPlayerController?.startPlayback(url)
            }
            PlayerEngine.VLC -> {
                vlcMediaController?.startPlayback(url)
            }
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping playback on $currentEngine")
        when (currentEngine) {
            PlayerEngine.EXOPLAYER -> exoPlayerController?.stopPlayback()
            PlayerEngine.VLC -> vlcMediaController?.stopPlayback()
        }
    }

    fun togglePlayPause() {
        when (currentEngine) {
            PlayerEngine.EXOPLAYER -> exoPlayerController?.togglePlayPause()
            PlayerEngine.VLC -> vlcMediaController?.togglePlayPause()
        }
    }

    fun isPlaying(): Boolean {
        return when (currentEngine) {
            PlayerEngine.EXOPLAYER -> exoPlayerController?.isPlaying() ?: false
            PlayerEngine.VLC -> vlcMediaController?.isPlaying() ?: false
        }
    }

    fun getCurrentEngine(): PlayerEngine {
        return currentEngine
    }

    fun forceVLC() {
        Log.d(TAG, "Forcing VLC engine")
        if (currentEngine == PlayerEngine.EXOPLAYER) {
            exoPlayerController?.stopPlayback()
        }
        switchToVLC()
    }
    
    fun clearVideoSurface(container: FrameLayout) {
        Log.d(TAG, "Clearing video surface for engine switch")
        try {
            container.removeAllViews()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing video surface", e)
        }
    }

    fun release() {
        Log.d(TAG, "Releasing all media controllers")
        try {
            exoPlayerController?.release()
            vlcMediaController?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
}