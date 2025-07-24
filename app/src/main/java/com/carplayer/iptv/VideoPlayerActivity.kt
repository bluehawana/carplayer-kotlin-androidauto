package com.carplayer.iptv

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var playerView: PlayerView
    private lateinit var statusTextView: TextView
    private lateinit var channelNameTextView: TextView
    private lateinit var controlsLayout: LinearLayout
    private lateinit var mediaController: MediaController
    
    private var channelName: String = ""
    private var streamUrl: String = ""
    private var isPlaying = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable fullscreen mode for automotive
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        
        // Hide action bar for automotive fullscreen
        supportActionBar?.hide()
        
        // Get channel info from intent
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "Unknown Channel"
        streamUrl = intent.getStringExtra("STREAM_URL") ?: ""
        
        setupMediaController()
        setupUI()
        startPlayback()
    }
    
    private fun setupMediaController() {
        mediaController = MediaController(this)
        
        mediaController.setOnErrorCallback { errorMessage ->
            runOnUiThread {
                statusTextView.text = "❌ $errorMessage\n\n❄️ Stream unavailable"
                Toast.makeText(this, "❄️ $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
        
        mediaController.setOnStateChangedCallback { playing ->
            runOnUiThread {
                isPlaying = playing
                statusTextView.text = if (playing) {
                    "✅ Playing: $channelName\n❄️ Nordic Ice Age - Cool Your Summer"
                } else {
                    "⏸️ Buffering: $channelName\n❄️ Loading stream..."
                }
                updatePlayPauseButton()
            }
        }
    }
    
    private fun setupUI() {
        // Main container - full screen
        val mainLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt()) // Pure black for automotive
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Video player view - full screen
        playerView = PlayerView(this).apply {
            useController = false // Custom controls only
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
        }
        mainLayout.addView(playerView)
        
        // Channel name in top-right corner (hidden initially)
        channelNameTextView = TextView(this).apply {
            text = channelName
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt()) // White text
            setShadowLayer(6f, 2f, 2f, 0xFF000000.toInt()) // Strong shadow
            setPadding(20, 16, 20, 16)
            setBackgroundColor(0x88000000.toInt()) // Semi-transparent black
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP or android.view.Gravity.END
            ).apply {
                setMargins(0, 40, 24, 0) // Top margin for automotive status bar
            }
            visibility = android.view.View.GONE // Hidden initially
        }
        mainLayout.addView(channelNameTextView)
        
        // Loading status (hidden after playback starts)
        statusTextView = TextView(this).apply {
            text = "Loading..."
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt()) // White text
            setShadowLayer(6f, 2f, 2f, 0xFF000000.toInt()) // Strong shadow
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0x88000000.toInt()) // Semi-transparent black
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER
            )
            setPadding(40, 20, 40, 20)
        }
        mainLayout.addView(statusTextView)
        
        // Control overlay (hidden initially, shown on touch)
        controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 20, 40, 40)
            setBackgroundColor(0xBB000000.toInt()) // Semi-transparent black
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM
            )
            visibility = android.view.View.GONE // Hidden initially
        }
        
        // Play/Pause button
        val playPauseButton = Button(this).apply {
            text = "⏸️"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt()) // White text
            setBackgroundColor(0x66FFFFFF.toInt()) // Semi-transparent white
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(20, 0, 20, 0)
            }
            setOnClickListener {
                mediaController.togglePlayPause()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(playPauseButton)
        
        // Back button
        val backButton = Button(this).apply {
            text = "⬅ Back"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt()) // White text
            setBackgroundColor(0x66FF4444.toInt()) // Semi-transparent red
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(20, 0, 20, 0)
            }
            setOnClickListener {
                finish()
            }
        }
        controlsLayout.addView(backButton)
        
        mainLayout.addView(controlsLayout)
        
        // Touch listener to show/hide controls
        mainLayout.setOnClickListener {
            toggleControls()
        }
        
        setContentView(mainLayout)
    }
    
    private fun toggleControls() {
        val isVisible = controlsLayout.visibility == android.view.View.VISIBLE
        
        if (isVisible) {
            // Hide controls and channel name
            controlsLayout.visibility = android.view.View.GONE
            channelNameTextView.visibility = android.view.View.GONE
        } else {
            // Show controls and channel name
            controlsLayout.visibility = android.view.View.VISIBLE
            channelNameTextView.visibility = android.view.View.VISIBLE
            hideControlsAfterDelay()
        }
    }
    
    private fun hideControlsAfterDelay() {
        // Auto-hide controls after 3 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (controlsLayout.visibility == android.view.View.VISIBLE) {
                controlsLayout.visibility = android.view.View.GONE
                channelNameTextView.visibility = android.view.View.GONE
            }
        }, 3000)
    }
    
    private fun updatePlayPauseButton() {
        val playPauseButton = controlsLayout.getChildAt(0) as? Button
        playPauseButton?.text = if (isPlaying) "⏸️" else "▶️"
    }
    
    private fun startPlayback() {
        if (streamUrl.isEmpty()) {
            statusTextView.text = "No stream URL provided"
            return
        }
        
        // Show loading message
        statusTextView.text = "Loading $channelName..."
        statusTextView.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Get IPv4 alternatives for better compatibility
                val streamTester = StreamTester()
                val alternatives = streamTester.replaceWithIPv4Alternatives(streamUrl)
                
                // Start playback immediately with the best URL
                val bestUrl = alternatives.firstOrNull() ?: streamUrl
                
                runOnUiThread {
                    // Start ExoPlayer immediately
                    mediaController.startPlayback(bestUrl)
                    
                    // Connect player to view immediately
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (::mediaController.isInitialized) {
                            val exoPlayer = mediaController.getExoPlayer()
                            if (exoPlayer != null) {
                                playerView.player = exoPlayer
                                
                                // Hide loading message once player is connected
                                statusTextView.visibility = android.view.View.GONE
                            }
                        }
                    }, 500)
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    statusTextView.text = "Cannot play channel\nTry different channel"
                    statusTextView.visibility = android.view.View.VISIBLE
                }
                android.util.Log.e("VideoPlayer", "Playback failed for $channelName", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::mediaController.isInitialized) {
            mediaController.stopPlayback()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::mediaController.isInitialized && isPlaying) {
            mediaController.togglePlayPause()
        }
    }
}