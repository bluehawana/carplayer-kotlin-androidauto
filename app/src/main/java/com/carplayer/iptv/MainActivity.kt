package com.carplayer.iptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private val channelManager = ChannelManager()
    private lateinit var networkBalancer: NetworkBalancer
    private lateinit var networkStatusView: TextView
    private lateinit var subtitleView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        networkBalancer = NetworkBalancer(this)
        
        setupNordicUI()
        
        // FORCE CLEAR CACHE and reload channels from updated M3U file
        clearChannelCache()
        channelManager.reloadFromAssets(this)
        
        // FORCE CLEAR CACHE and reload channels from updated M3U file
        clearChannelCache()
        channelManager.reloadFromAssets(this)
        
        // Load sample M3U files on first run
        val preloadedFiles = PreloadedM3UFiles(this)
        preloadedFiles.loadSampleM3UFiles()
        
        // Import M3U files from project folder and update channel count
        val m3uImporter = M3UImporter(this)
        lifecycleScope.launch {
            val importedFiles = m3uImporter.importProjectM3UFiles()
            updateChannelCount()
        }
        
        // Check network status
        checkNetworkStatus()
        
        // Check if connected to Android Auto
        val carConnection = CarConnection(this)
        carConnection.type.observe(this) { connectionType ->
            when (connectionType) {
                CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> {
                    // Show phone UI
                    showPhoneInterface()
                }
                CarConnection.CONNECTION_TYPE_NATIVE -> {
                    // Connected to Android Auto
                    showCarInterface()
                }
            }
        }
    }
    
    private fun setupNordicUI() {
        // Get screen dimensions for Volvo EX90 responsive design
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // Volvo EX90 specific values - 11.2" display optimization
        val basePadding = (24 * density).toInt() // Increased for car use
        val titleSize = if (screenWidth > 1400) 28f else if (screenWidth > 1000) 24f else 20f
        val subtitleSize = if (screenWidth > 1400) 18f else if (screenWidth > 1000) 16f else 14f
        val buttonTextSize = if (screenWidth > 1400) 20f else if (screenWidth > 1000) 18f else 16f
        
        // Create Volvo EX90 optimized layout - Dark theme
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(basePadding, basePadding/2, basePadding, basePadding/2)
            setBackgroundColor(0xFF1a1a1a.toInt()) // Volvo dark background
        }
        
        // App title - FULLY RESPONSIVE
        val titleView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = titleSize
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, (12 * density).toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        layout.addView(titleView)
        
        // Subtitle - FULLY RESPONSIVE
        subtitleView = TextView(this).apply {
            text = "🌨️ Nordic Ice Age Theme\n🎬 Loading channels..."
            textSize = subtitleSize
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, (10 * density).toInt())
        }
        layout.addView(subtitleView)
        
        // Network status - FULLY RESPONSIVE
        networkStatusView = TextView(this).apply {
            text = "🔍 Checking network connectivity..."
            textSize = subtitleSize - 2f
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            setBackgroundColor(0xFF0c2d48.toInt()) // Dark Blue
        }
        layout.addView(networkStatusView)
        
        // Start Watching button - Volvo EX90 optimized touch target
        val watchButton = Button(this).apply {
            text = "🎬 Start Watching"
            textSize = buttonTextSize
            setTextColor(0xFFFFFFFF.toInt()) // White text for Volvo theme
            setBackgroundColor(0xFF2196F3.toInt()) // Volvo blue
            setPadding((24 * density).toInt(), (16 * density).toInt(), (24 * density).toInt(), (16 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (64 * density).toInt() // Larger touch target for car use
            ).apply {
                setMargins((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            }
            // Add haptic feedback for car touch
            isHapticFeedbackEnabled = true
            setOnClickListener {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                launchCarInterface()
            }
        }
        layout.addView(watchButton)
        
        // Import Settings button - Volvo EX90 car optimized
        val settingsButton = Button(this).apply {
            text = "🗂️ Import Channels"
            textSize = buttonTextSize - 2f
            setTextColor(0xFFFFFFFF.toInt()) // White text
            setBackgroundColor(0xFF424242.toInt()) // Volvo dark gray
            setPadding((20 * density).toInt(), (14 * density).toInt(), (20 * density).toInt(), (14 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * density).toInt() // Car-friendly touch target
            ).apply {
                setMargins((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            }
            isHapticFeedbackEnabled = true
            setOnClickListener {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                val intent = Intent(this@MainActivity, ImportActivity::class.java)
                startActivity(intent)
            }
        }
        layout.addView(settingsButton)
        
        // Network test button - Volvo EX90 touch optimized
        val networkButton = Button(this).apply {
            text = "🌐 Test Network"
            textSize = buttonTextSize - 2f
            setTextColor(0xFF1a1a1a.toInt()) // Dark text
            setBackgroundColor(0xFF9E9E9E.toInt()) // Light gray Volvo theme
            setPadding((20 * density).toInt(), (14 * density).toInt(), (20 * density).toInt(), (14 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * density).toInt() // Car-friendly touch target
            ).apply {
                setMargins((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            }
            isHapticFeedbackEnabled = true
            setOnClickListener {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                checkNetworkStatus()
            }
        }
        layout.addView(networkButton)
        
        // Version info - Volvo EX90 Edition
        val versionView = TextView(this).apply {
            text = "🚗 Volvo EX90 Edition v2.0 | 🎯 92 Channels | 📱 Touch Optimized"
            textSize = if (screenWidth > 1000) 14f else 12f
            setTextColor(0xFF9E9E9E.toInt()) // Light gray
            gravity = android.view.Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        layout.addView(versionView)
        
        setContentView(layout)
        
        // Set action bar color
        supportActionBar?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(0xFF0c2d48.toInt())
        )
        supportActionBar?.title = "❄️ Car TV Player"
    }
    
    private fun checkNetworkStatus() {
        lifecycleScope.launch {
            networkStatusView.text = "🔍 Testing network connectivity..."
            
            try {
                val networks = networkBalancer.getAvailableNetworks()
                val connectedNetworks = networks.filter { it.isConnected && it.hasInternet }
                
                val statusText = if (connectedNetworks.isNotEmpty()) {
                    val networkInfo = connectedNetworks.joinToString("\n") { network ->
                        val ipSupport = mutableListOf<String>()
                        if (network.supportsIPv4) ipSupport.add("IPv4")
                        if (network.supportsIPv6) ipSupport.add("IPv6")
                        
                        "✅ ${network.type} (${ipSupport.joinToString(", ")})"
                    }
                    "🌐 Network Status:\n$networkInfo\n\n❄️ Ready for streaming!"
                } else {
                    "❌ No internet connection\n🌨️ Check your network settings"
                }
                
                networkStatusView.text = statusText
                
            } catch (e: Exception) {
                networkStatusView.text = "⚠️ Network test error:\n${e.message}\n\n❄️ Try again later"
            }
        }
    }
    
    private fun launchCarInterface() {
        // Create a test activity that simulates the car interface
        val intent = Intent(this, CarTestActivity::class.java)
        startActivity(intent)
    }
    
    private fun showPhoneInterface() {
        // Show phone-specific UI for managing subscriptions
        // This allows users to import subscriptions when not in car
    }
    
    private fun showCarInterface() {
        // Launch car app service
        val intent = Intent(this, CarAppService::class.java)
        startService(intent)
    }
    
    private fun clearChannelCache() {
        // Clear all cached channel data to force fresh reload
        val prefs = getSharedPreferences("iptv_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        android.util.Log.d("MainActivity", "Channel cache cleared - will reload fresh M3U data")
    }
    
    private fun updateChannelCount() {
        lifecycleScope.launch {
            try {
                val m3uFileManager = com.carplayer.iptv.storage.M3UFileManager(this@MainActivity)
                
                // Load directly from assets iptv.m3u to get accurate count
                val channelCount = try {
                    val inputStream = assets.open("iptv.m3u")
                    val content = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()
                    
                    // Parse content to count channels
                    val lines = content.split("\n")
                    var count = 0
                    for (line in lines) {
                        if (line.trim().startsWith("http://") || line.trim().startsWith("https://")) {
                            count++
                        }
                    }
                    count
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error reading assets iptv.m3u", e)
                    
                    // Fallback to M3U file manager
                    val m3uFiles = m3uFileManager.getAllM3UFiles()
                    if (m3uFiles.isNotEmpty()) {
                        val channels = m3uFileManager.loadM3UFile(m3uFiles.first().fileName)
                        channels?.size ?: 0
                    } else {
                        0
                    }
                }
                
                runOnUiThread {
                    subtitleView.text = "🌨️ Nordic Ice Age Theme\n🎬 $channelCount Premium Channels"
                }
                
                android.util.Log.d("MainActivity", "Updated channel count: $channelCount")
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to update channel count", e)
                runOnUiThread {
                    subtitleView.text = "🌨️ Nordic Ice Age Theme\n🎬 Premium Channels"
                }
            }
        }
    }
}