
package com.carplayer.iptv

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcMediaController(private val context: Context) {

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: VLCVideoLayout? = null

    private var errorCallback: ((String) -> Unit)? = null
    private var stateChangedCallback: ((Boolean) -> Unit)? = null

    init {
        libVLC = LibVLC(context, ArrayList<String>().apply {
            add("--no-stats")
            add("--no-audio")
            add("--no-sub-autodetect-file")
            add("--no-snapshot-preview")
            add("--no-spu")
            add("--network-caching=1500")
            add("--live-caching=1500")
            add("--sout-mux-caching=1500")
        })
        mediaPlayer = MediaPlayer(libVLC)
    }

    fun setOnErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setOnStateChangedCallback(callback: (Boolean) -> Unit) {
        stateChangedCallback = callback
    }

    fun createVideoSurface(container: FrameLayout) {
        videoLayout = VLCVideoLayout(context)
        videoLayout?.let {
            container.addView(it)
            mediaPlayer?.attachViews(it, null, false, false)
        }
    }

    fun startPlayback(url: String) {
        val media = org.videolan.libvlc.Media(libVLC, Uri.parse(url))
        mediaPlayer?.media = media
        media.release()
        mediaPlayer?.play()
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
    }

    fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        } else {
            mediaPlayer?.play()
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun release() {
        mediaPlayer?.release()
        libVLC?.release()
    }
}
