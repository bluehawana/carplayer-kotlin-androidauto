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
        // Create Nordic-themed layout programmatically - RESPONSIVE
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16) // Reduced padding for smaller screens
            setBackgroundColor(0xFF145da0.toInt()) // Midnight Blue background
        }
        
        // App title - RESPONSIVE
        val titleView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 22f // Smaller for different resolutions
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16) // Reduced padding
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        layout.addView(titleView)
        
        // Subtitle - RESPONSIVE
        val subtitleView = TextView(this).apply {
            text = "🌨️ Nordic Ice Age Theme\n🎬 104 Premium Channels"
            textSize = 14f // Smaller text
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 12) // Reduced padding
        }
        layout.addView(subtitleView)
        
        // Network status - RESPONSIVE
        networkStatusView = TextView(this).apply {
            text = "🔍 Checking network connectivity..."
            textSize = 12f // Smaller text
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding(12, 8, 12, 12) // Reduced padding
            setBackgroundColor(0xFF0c2d48.toInt()) // Dark Blue
        }
        layout.addView(networkStatusView)
        
        // Start Watching button - RESPONSIVE Ice Age Style
        val watchButton = Button(this).apply {
            text = "🎬 Start Watching"
            textSize = 16f // Smaller text
            setTextColor(0xFF00FF41.toInt()) // Ice green
            setBackgroundResource(android.R.drawable.btn_default)
            background.setColorFilter(0xFF004D5C.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) // Deep ice
            setPadding(24, 16, 24, 16) // Reduced padding
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8) // Add margins for better spacing
            }
            setOnClickListener {
                launchCarInterface()
            }
        }
        layout.addView(watchButton)
        
        // Import Settings button - RESPONSIVE Ice Age Style
        val settingsButton = Button(this).apply {
            text = "🗂️ Import Channels"
            textSize = 14f // Smaller text
            setTextColor(0xFFE0F7FA.toInt()) // Ice white
            setBackgroundResource(android.R.drawable.btn_default)
            background.setColorFilter(0xFF00BCD4.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) // Ice accent
            setPadding(24, 12, 24, 12) // Reduced padding
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 4, 16, 4) // Add margins
            }
            setOnClickListener {
                val intent = Intent(this@MainActivity, ImportActivity::class.java)
                startActivity(intent)
            }
        }
        layout.addView(settingsButton)
        
        // Network test button - RESPONSIVE
        val networkButton = Button(this).apply {
            text = "🌐 Test Network"
            textSize = 14f // Smaller text
            setTextColor(0xFF145da0.toInt()) // Midnight Blue text
            setBackgroundColor(0xFFb1d4e0.toInt()) // Baby Blue background
            setPadding(20, 12, 20, 12) // Reduced padding
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 4, 16, 4) // Add margins
            }
            setOnClickListener {
                checkNetworkStatus()
            }
        }
        layout.addView(networkButton)
        
        // Version info - RESPONSIVE
        val versionView = TextView(this).apply {
            text = "❄️ Nordic Edition v1.0 | 🎯 104 Channels"
            textSize = 10f // Smaller text
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 0) // Reduced padding
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