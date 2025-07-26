
package com.carplayer.iptv

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcMediaController(private val context: Context) {

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: VLCVideoLayout? = null

    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "VlcMediaController"
    }

    init {
        Log.d(TAG, "Initializing VLC LibVLC")
        try {
            libVLC = LibVLC(context, ArrayList<String>().apply {
                add("--no-stats")
                add("--no-snapshot-preview")
                add("--live-caching=1500")
                add("--network-caching=1500")
                add("--no-drop-late-frames")
                add("--no-skip-frames")
                add("--avcodec-hw=any")
                add("--http-reconnect")
                add("--sout-keep")
                add("--sout-mux-caching=1500")
                add("-vvv") 
            })
            
            mediaPlayer = MediaPlayer(libVLC)
            
            // Set up event listeners
            mediaPlayer?.setEventListener { event ->
                Log.d(TAG, "VLC Event: ${event.type}")
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        Log.d(TAG, "VLC: Started playing")
                        stateChangedCallback?.invoke(true)
                    }
                    MediaPlayer.Event.Paused -> {
                        Log.d(TAG, "VLC: Paused")
                        stateChangedCallback?.invoke(false)
                    }
                    MediaPlayer.Event.Stopped -> {
                        Log.d(TAG, "VLC: Stopped")
                        stateChangedCallback?.invoke(false)
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e(TAG, "VLC: Encountered error")
                        errorCallback?.invoke("VLC playback error")
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d(TAG, "VLC: End reached")
                        stateChangedCallback?.invoke(false)
                    }
                    MediaPlayer.Event.Buffering -> {
                        Log.d(TAG, "VLC: Buffering ${event.buffering}%")
                    }
                    MediaPlayer.Event.Opening -> {
                        Log.d(TAG, "VLC: Opening stream")
                    }
                }
            }
            
            Log.d(TAG, "VLC LibVLC initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC", e)
            errorCallback?.invoke("VLC initialization failed: ${e.message}")
        }
    }

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun createVideoSurface(container: FrameLayout) {
        Log.d(TAG, "Creating VLC video surface")
        try {
            videoLayout = VLCVideoLayout(context)
            videoLayout?.let { layout ->
                layout.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                container.addView(layout)
                
                // Attach VLC player to the video layout
                mediaPlayer?.attachViews(layout, null, false, false)
                Log.d(TAG, "VLC video surface created and attached")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video surface", e)
            errorCallback?.invoke("Failed to create video surface: ${e.message}")
        }
    }

    fun startPlayback(url: String) {
        Log.d(TAG, "Starting VLC playback for: $url")
        try {
            val media = Media(libVLC, Uri.parse(url))
            
            // Add media options for IPTV streams
            media.addOption(":network-caching=1500")
            media.addOption(":live-caching=1500")
            media.addOption(":http-reconnect=true")
            media.addOption(":http-continuous=true")
            media.addOption(":sout-keep")
            media.addOption(":sout-mux-caching=1500")
            media.addOption(":http-user-agent=VLC/3.0.0 LibVLC/3.0.0")
            
            mediaPlayer?.media = media
            media.release()
            
            mediaPlayer?.play()
            Log.d(TAG, "VLC playback started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            errorCallback?.invoke("VLC playback failed: ${e.message}")
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping VLC playback")
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }

    fun togglePlayPause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                Log.d(TAG, "VLC: Pausing playback")
                mediaPlayer?.pause()
                stateChangedCallback?.invoke(false)
            } else {
                Log.d(TAG, "VLC: Resuming playback")
                mediaPlayer?.play()
                stateChangedCallback?.invoke(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling playback", e)
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking playing state", e)
            false
        }
    }

    fun release() {
        Log.d(TAG, "Releasing VLC resources")
        try {
            mediaPlayer?.release()
            libVLC?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VLC resources", e)
        }
    }
}
