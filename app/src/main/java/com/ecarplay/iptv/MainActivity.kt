package com.ecarplay.iptv

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private val channelManager = ChannelManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
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