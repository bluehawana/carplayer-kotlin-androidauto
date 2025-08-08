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
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var videoContainer: FrameLayout
    private lateinit var statusTextView: TextView
    private lateinit var channelNameTextView: TextView
    private lateinit var controlsLayout: LinearLayout
    private lateinit var mediaController: HybridMediaController
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var channelPreloader: ChannelPreloader
    
    private var channelName: String = ""
    private var streamUrl: String = ""
    private var isPlaying = false
    private var allChannels: List<String> = emptyList()
    private var allChannelUrls: List<String> = emptyList()
    private var currentChannelIndex: Int = 0
    
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
        
        // Get channel list for navigation
        allChannels = intent.getStringArrayListExtra("ALL_CHANNELS") ?: emptyList()
        allChannelUrls = intent.getStringArrayListExtra("ALL_CHANNEL_URLS") ?: emptyList()
        currentChannelIndex = intent.getIntExtra("CURRENT_INDEX", 0)
        
        setupMediaController()
        setupNetworkMonitoring()
        setupUI()
        startPlayback()
    }
    
    private fun setupMediaController() {
        mediaController = HybridMediaController(this)
        setupMediaControllerCallbacks()
    }
    
    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this)
        channelPreloader = ChannelPreloader(this)
        
        // Monitor network changes and adjust streaming
        networkMonitor.setOnNetworkChangedCallback { networkInfo ->
            android.util.Log.d("NetworkMonitor", "Network changed: ${networkInfo?.type} (Hotspot: ${networkInfo?.isHotspot})")
            
            // Update status with network info
            val networkDesc = when {
                networkInfo?.isHotspot == true -> "📱 Using hotspot connection"
                networkInfo?.type == "Mobile Data" -> "📱 Using mobile data"
                networkInfo?.type == "WiFi" -> "📶 Using WiFi connection"
                else -> "🔌 Network connection"
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (statusTextView.visibility == android.view.View.GONE) {
                    statusTextView.text = networkDesc
                    statusTextView.visibility = android.view.View.VISIBLE
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        statusTextView.visibility = android.view.View.GONE
                    }, 2000)
                }
            }
        }
        
        networkMonitor.setOnStreamingProfileChangedCallback { profile ->
            android.util.Log.d("NetworkMonitor", "Streaming profile changed: ${profile.description}")
            
            // Start preloading if recommended
            if (profile.preloadChannels && allChannelUrls.isNotEmpty()) {
                channelPreloader.preloadChannels(allChannelUrls, streamUrl)
            }
        }
        
        // Start network monitoring
        networkMonitor.startMonitoring()
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

        // Video container for VLC-like player - full screen
        videoContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
        }
        mainLayout.addView(videoContainer)

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

        // Control overlay (positioned ABOVE Android Auto's system controls) - Ice Age Style
        controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(0xCC004D5C.toInt()) // More opaque deep ice background
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM
            ).apply {
                setMargins(0, 0, 0, 120) // Push up 120px to avoid Android Auto buttons
            }
            visibility = android.view.View.GONE // Hidden initially
            elevation = 10f // Ensure it appears above other elements
        }

        // Previous Channel button - Ice Age Style
        val previousButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "⏮️ PREV"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
            }
            setOnClickListener {
                previousChannel()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(previousButton)

        // Play/Pause button - Ice Age Style
        val playPauseButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "⏸️ PAUSE"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
            }
            setOnClickListener {
                mediaController.togglePlayPause()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(playPauseButton)

        // Engine Switch button for Sky F1 audio issues - Ice Age Style
        val engineButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "🎵 VLC"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
            }
            setOnClickListener {
                switchMediaEngine()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(engineButton)

        // Restart button - Ice Age Style
        val restartButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "🔄 RESTART"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
            }
            setOnClickListener {
                restartPlayback()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(restartButton)



        // Next Channel button - Ice Age Style
        val nextButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "NEXT ⏭️"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
            }
            setOnClickListener {
                nextChannel()
                hideControlsAfterDelay()
            }
        }
        controlsLayout.addView(nextButton)

        // Back button - Ice Age Style
        val backButton = Button(this, null, 0, R.style.PlayerControlButton).apply {
            text = "⬅ BACK"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(12, 0, 12, 0)
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
        // Auto-hide controls after 5 seconds (longer for easier channel switching)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (controlsLayout.visibility == android.view.View.VISIBLE) {
                controlsLayout.visibility = android.view.View.GONE
                channelNameTextView.visibility = android.view.View.GONE
            }
        }, 5000)
    }
    
    private fun updatePlayPauseButton() {
        val playPauseButton = controlsLayout.getChildAt(1) as? Button // Second button is play/pause
        playPauseButton?.text = if (isPlaying) "⏸️ PAUSE" else "▶️ PLAY"
        
        // Update engine button text
        val engineButton = controlsLayout.getChildAt(2) as? Button // Third button is engine switch
        val currentEngine = if (::mediaController.isInitialized) {
            mediaController.getCurrentEngine()
        } else {
            HybridMediaController.MediaEngine.EXOPLAYER
        }
        engineButton?.text = when (currentEngine) {
            HybridMediaController.MediaEngine.EXOPLAYER -> "🎵 VLC"
            HybridMediaController.MediaEngine.VLC -> "🎵 EXO"
        }
    }
    
    private fun switchMediaEngine() {
        if (!::mediaController.isInitialized) return
        
        val currentEngine = mediaController.getCurrentEngine()
        statusTextView.text = when (currentEngine) {
            HybridMediaController.MediaEngine.EXOPLAYER -> "🎵 Switching to VLC for better audio..."
            HybridMediaController.MediaEngine.VLC -> "🎵 Switching to ExoPlayer for better sync..."
        }
        statusTextView.visibility = android.view.View.VISIBLE
        
        when (currentEngine) {
            HybridMediaController.MediaEngine.EXOPLAYER -> {
                mediaController.forceVlcEngine()
            }
            HybridMediaController.MediaEngine.VLC -> {
                mediaController.forceExoPlayerEngine()
            }
        }
        
        updatePlayPauseButton()
        
        // Hide message after delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            statusTextView.visibility = android.view.View.GONE
        }, 3000)
    }
    

    
    private fun restartPlayback() {
        statusTextView.text = "❄️ Restarting playback..."
        statusTextView.visibility = android.view.View.VISIBLE
        
        if (::mediaController.isInitialized) {
            val currentEngine = mediaController.getCurrentEngine()
            
            // Stop current playback
            mediaController.stopPlayback()
            
            // Restart with current engine
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                mediaController.startPlayback(streamUrl)
                statusTextView.text = "❄️ Playback restarted with ${currentEngine.name}"
                
                // Hide message after delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    statusTextView.visibility = android.view.View.GONE
                }, 3000)
            }, 500)
        }
    }
    
    private fun setupMediaControllerCallbacks() {
        mediaController.setOnErrorCallback { message ->
            if (message.isEmpty()) {
                statusTextView.visibility = android.view.View.GONE
            } else {
                statusTextView.text = message
                statusTextView.visibility = android.view.View.VISIBLE
                
                if (message.contains("Buffering 100%") || message.contains("Started playing")) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        statusTextView.visibility = android.view.View.GONE
                    }, 1000)
                }
            }
        }
        
        mediaController.setOnStateChangedCallback {
            isPlaying = it
            updatePlayPauseButton()
        }
    }
    

    
    private fun previousChannel() {
        if (allChannels.isNotEmpty() && currentChannelIndex > 0) {
            currentChannelIndex--
            switchToChannel(currentChannelIndex)
        } else {
            Toast.makeText(this, "❄️ At first channel", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun nextChannel() {
        if (allChannels.isNotEmpty() && currentChannelIndex < allChannels.size - 1) {
            currentChannelIndex++
            switchToChannel(currentChannelIndex)
        } else {
            Toast.makeText(this, "❄️ At last channel", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun switchToChannel(index: Int) {
        if (index < 0 || index >= allChannels.size || index >= allChannelUrls.size) return
        
        channelName = allChannels[index]
        streamUrl = allChannelUrls[index]
        
        // Update channel name display
        channelNameTextView.text = channelName
        
        // Check if channel is preloaded
        val preloadInfo = if (::channelPreloader.isInitialized) {
            channelPreloader.getChannelRecommendation(streamUrl)
        } else null
        
        // Show loading status with preload info
        val baseMessage = "❄️ Switching to $channelName..."
        val statusMessage = if (preloadInfo != null) {
            "$baseMessage\n$preloadInfo"
        } else {
            baseMessage
        }
        
        statusTextView.text = statusMessage
        statusTextView.visibility = android.view.View.VISIBLE
        
        // Stop current playback for channel switching
        if (::mediaController.isInitialized) {
            mediaController.stopPlayback()
        }
        
        // Start preloading next channels after switching
        if (::channelPreloader.isInitialized && allChannelUrls.isNotEmpty()) {
            channelPreloader.preloadChannels(allChannelUrls, streamUrl)
        }
        
        // Longer delay for proper cleanup and buffer reset
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startPlayback()
        }, 1000) // Increased delay for better stability
    }
    
    private fun startPlayback() {
        if (streamUrl.isEmpty()) {
            statusTextView.text = "No stream URL provided"
            return
        }
        
        // Show loading message with buffering info
        statusTextView.text = "❄️ Loading $channelName...\n(Building 4-second sync buffer)"
        statusTextView.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                runOnUiThread {
                    // Only create surface if not already created
                    if (videoContainer.childCount == 0) {
                        mediaController.createVideoSurface(videoContainer)
                    }
                    
                    mediaController.startPlayback(streamUrl)
                    isPlaying = true
                    updatePlayPauseButton()
                    
                    // Hide loading message after sync buffer is built
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (statusTextView.text.toString().contains("Loading")) {
                            statusTextView.visibility = android.view.View.GONE
                        }
                    }, 6000) // Wait for 4s buffer + 2s extra
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusTextView.text = "❄️ Stream error: ${e.message}\n\nTap screen and try NEXT channel"
                    statusTextView.visibility = android.view.View.VISIBLE
                    
                    // Auto-retry with next channel after 3 seconds
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (currentChannelIndex < allChannels.size - 1) {
                            nextChannel()
                        }
                    }, 3000)
                }
                android.util.Log.e("VideoPlayer", "Playback failed for $channelName: $streamUrl", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::mediaController.isInitialized) {
            mediaController.release()
        }
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
        if (::channelPreloader.isInitialized) {
            channelPreloader.clearCache()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::mediaController.isInitialized && mediaController.isPlaying()) {
            mediaController.togglePlayPause()
        }
    }
}