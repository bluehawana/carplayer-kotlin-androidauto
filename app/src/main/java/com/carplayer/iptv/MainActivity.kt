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
        
        // Import M3U files from project folder
        val m3uImporter = M3UImporter(this)
        lifecycleScope.launch {
            m3uImporter.importProjectM3UFiles()
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
        // Get screen dimensions for responsive design
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // Calculate responsive values based on screen size
        val basePadding = (16 * density).toInt()
        val titleSize = if (screenWidth > 1200) 24f else if (screenWidth > 800) 20f else 18f
        val subtitleSize = if (screenWidth > 1200) 16f else if (screenWidth > 800) 14f else 12f
        val buttonTextSize = if (screenWidth > 1200) 18f else if (screenWidth > 800) 16f else 14f
        
        // Create Nordic-themed layout programmatically - FULLY RESPONSIVE
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(basePadding, basePadding/2, basePadding, basePadding/2)
            setBackgroundColor(0xFF145da0.toInt()) // Midnight Blue background
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
        val subtitleView = TextView(this).apply {
            text = "🌨️ Nordic Ice Age Theme\n🎬 20 Premium Channels"
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
        
        // Start Watching button - FULLY RESPONSIVE Ice Age Style
        val watchButton = Button(this).apply {
            text = "🎬 Start Watching"
            textSize = buttonTextSize
            setTextColor(0xFF00FF41.toInt()) // Ice green
            setBackgroundResource(android.R.drawable.btn_default)
            background.setColorFilter(0xFF004D5C.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) // Deep ice
            setPadding((20 * density).toInt(), (12 * density).toInt(), (20 * density).toInt(), (12 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * density).toInt() // Fixed height based on density
            ).apply {
                setMargins((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
            }
            setOnClickListener {
                launchCarInterface()
            }
        }
        layout.addView(watchButton)
        
        // Import Settings button - FULLY RESPONSIVE Ice Age Style
        val settingsButton = Button(this).apply {
            text = "🗂️ Import Channels"
            textSize = buttonTextSize - 2f
            setTextColor(0xFFE0F7FA.toInt()) // Ice white
            setBackgroundResource(android.R.drawable.btn_default)
            background.setColorFilter(0xFF00BCD4.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) // Ice accent
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt() // Fixed height based on density
            ).apply {
                setMargins((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            }
            setOnClickListener {
                val intent = Intent(this@MainActivity, ImportActivity::class.java)
                startActivity(intent)
            }
        }
        layout.addView(settingsButton)
        
        // Network test button - FULLY RESPONSIVE
        val networkButton = Button(this).apply {
            text = "🌐 Test Network"
            textSize = buttonTextSize - 2f
            setTextColor(0xFF145da0.toInt()) // Midnight Blue text
            setBackgroundColor(0xFFb1d4e0.toInt()) // Baby Blue background
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt() // Fixed height based on density
            ).apply {
                setMargins((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            }
            setOnClickListener {
                checkNetworkStatus()
            }
        }
        layout.addView(networkButton)
        
        // Version info - FULLY RESPONSIVE
        val versionView = TextView(this).apply {
            text = "❄️ Nordic Edition v1.0 | 🎯 20 Channels"
            textSize = if (screenWidth > 800) 12f else 10f
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding(0, (6 * density).toInt(), 0, 0)
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
}