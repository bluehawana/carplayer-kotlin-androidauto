package com.ecarplay.iptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
        
        // Load saved subscriptions
        channelManager.loadSubscriptions(this)
        
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
        // Create Nordic-themed layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF145da0.toInt()) // Midnight Blue background
        }
        
        // App title
        val titleView = TextView(this).apply {
            text = "❄️ ECarTV Nordic IPTV"
            textSize = 28f
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        layout.addView(titleView)
        
        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "🌨️ Nordic Ice-Age IPTV Player\n🎬 Premium Streaming Experience"
            textSize = 16f
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(subtitleView)
        
        // Network status
        networkStatusView = TextView(this).apply {
            text = "🔍 Checking network connectivity..."
            textSize = 14f
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 24)
            setBackgroundColor(0xFF0c2d48.toInt()) // Dark Blue
        }
        layout.addView(networkStatusView)
        
        // Start Watching button
        val watchButton = Button(this).apply {
            text = "🎬 Start Watching"
            textSize = 18f
            setTextColor(0xFF145da0.toInt()) // Midnight Blue text
            setBackgroundColor(0xFFF5F5F5.toInt()) // Smoky white background
            setPadding(32, 24, 32, 24)
            setOnClickListener {
                launchCarInterface()
            }
        }
        layout.addView(watchButton)
        
        // Import Settings button
        val settingsButton = Button(this).apply {
            text = "⚙️ Import & Settings"
            textSize = 16f
            setTextColor(0xFFF5F5F5.toInt()) // Smoky white text
            setBackgroundColor(0xFF2e8bc0.toInt()) // Blue background
            setPadding(32, 20, 32, 20)
            setOnClickListener {
                val intent = Intent(this@MainActivity, ImportActivity::class.java)
                startActivity(intent)
            }
        }
        layout.addView(settingsButton)
        
        // Network test button
        val networkButton = Button(this).apply {
            text = "🌐 Test Network"
            textSize = 16f
            setTextColor(0xFF145da0.toInt()) // Midnight Blue text
            setBackgroundColor(0xFFb1d4e0.toInt()) // Baby Blue background
            setPadding(32, 20, 32, 20)
            setOnClickListener {
                checkNetworkStatus()
            }
        }
        layout.addView(networkButton)
        
        // Version info
        val versionView = TextView(this).apply {
            text = "\n❄️ Nordic Edition v1.0\n🎯 Optimized for IPv4/IPv6"
            textSize = 12f
            setTextColor(0xFFb1d4e0.toInt()) // Baby Blue
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        layout.addView(versionView)
        
        setContentView(layout)
        
        // Set action bar color
        supportActionBar?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(0xFF0c2d48.toInt())
        )
        supportActionBar?.title = "ECarTV Nordic"
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
}