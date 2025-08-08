package com.carplayer.iptv

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.util.UnstableApi
import com.carplayer.iptv.models.Channel
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class CarTestActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter
    private val m3uFileManager = M3UFileManager(this)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create Volvo EX90 optimized grid layout - 2 columns for better touch targets
        recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@CarTestActivity, 2) // 2 columns for Volvo EX90 touch
            setPadding(48, 48, 48, 48) // Increased padding for car use
            setBackgroundColor(0xFF1a1a1a.toInt()) // Volvo dark theme
            // Add haptic feedback for touch
            isHapticFeedbackEnabled = true
        }
        
        setContentView(recyclerView)
        
        // Set title
        
        
        // Load channels
        loadChannels()
    }
    
    private fun loadChannels() {
        coroutineScope.launch {
            try {
                android.util.Log.d("CarTestActivity", "Starting to load channels...")
                
                val importer = M3UImporter(this@CarTestActivity)
                val importedFiles = importer.importProjectM3UFiles()
                
                android.util.Log.d("CarTestActivity", "Imported files: ${importedFiles.size}")
                
                // FORCE CLEAR CACHE and reload from updated assets first
                clearChannelCache()
                val channelManager = ChannelManager()
                channelManager.reloadFromAssets(this@CarTestActivity)
                
                // Try to load from assets directly first
                val channels = try {
                    android.util.Log.d("CarTestActivity", "Loading UPDATED M3U directly from assets...")
                    val content = assets.open("iptv.m3u").bufferedReader().use { it.readText() }
                    android.util.Log.d("CarTestActivity", "Updated M3U content length: ${content.length}")
                    android.util.Log.d("CarTestActivity", "First 200 chars: ${content.take(200)}")
                    
                    val parsedChannels = parseM3UContent(content)
                    android.util.Log.d("CarTestActivity", "Parsed ${parsedChannels.size} channels from UPDATED assets")
                    
                    // Log first few channel names to verify
                    parsedChannels.take(5).forEach { channel ->
                        android.util.Log.d("CarTestActivity", "Channel: ${channel.name}")
                    }
                    
                    parsedChannels
                } catch (e: Exception) {
                    android.util.Log.e("CarTestActivity", "Failed to load from assets", e)
                    
                    // Fallback to M3UFileManager
                    val channels = if (importedFiles.isNotEmpty()) {
                        android.util.Log.d("CarTestActivity", "Loading from imported file: ${importedFiles.first().fileName}")
                        m3uFileManager.loadM3UFile(importedFiles.first().fileName) ?: emptyList()
                    } else {
                        android.util.Log.d("CarTestActivity", "No imported files, checking existing...")
                        val existingFiles = m3uFileManager.getAllM3UFiles()
                        android.util.Log.d("CarTestActivity", "Existing files: ${existingFiles.size}")
                        
                        if (existingFiles.isNotEmpty()) {
                            android.util.Log.d("CarTestActivity", "Loading from existing file: ${existingFiles.first().fileName}")
                            m3uFileManager.loadM3UFile(existingFiles.first().fileName) ?: emptyList()
                        } else {
                            android.util.Log.w("CarTestActivity", "No M3U files found")
                            emptyList()
                        }
                    }
                    channels
                }
                
                android.util.Log.d("CarTestActivity", "Final channel count: ${channels.size}")
                
                if (channels.isNotEmpty()) {
                    // Show ALL channels, not just 16!
                    (application as CarPlayerApplication).channels.clear()
                    (application as CarPlayerApplication).channels.addAll(channels)
                    channelAdapter = ChannelAdapter(channels) // Show all channels
                    recyclerView.adapter = channelAdapter
                    
                    // Update title to show channel count
                    runOnUiThread {
                        title = "Car TV Player - ${channels.size} Channels (FRESH)"
                    }
                } else {
                    android.util.Log.w("CarTestActivity", "No channels loaded, using sample data")
                    // Create sample channels if loading fails
                    val sampleChannels = createSampleChannels()
                    (application as CarPlayerApplication).channels.clear()
                    (application as CarPlayerApplication).channels.addAll(sampleChannels)
                    channelAdapter = ChannelAdapter(sampleChannels)
                    recyclerView.adapter = channelAdapter
                    
                    runOnUiThread {
                        title = "❄️ Car TV Player - Sample Data (No M3U)"
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CarTestActivity", "Failed to load channels", e)
                // Create sample channels if loading fails
                val sampleChannels = createSampleChannels()
                channelAdapter = ChannelAdapter(sampleChannels)
                recyclerView.adapter = channelAdapter
                
                runOnUiThread {
                    title = "❄️ Car TV Player - Error Loading"
                }
            }
        }
    }
    
    private fun parseM3UContent(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n")
        
        android.util.Log.d("CarTestActivity", "Parsing M3U content with ${lines.size} lines")
        
        var channelInfo = ""
        
        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    channelInfo = line.substringAfter("#EXTINF:")
                    android.util.Log.d("CarTestActivity", "Found EXTINF: $channelInfo")
                }
                line.startsWith("http") -> {
                    val name = extractChannelName(channelInfo)
                    val logoUrl = extractLogoUrl(channelInfo)
                    val category = extractCategory(channelInfo)
                    
                    android.util.Log.d("CarTestActivity", "Creating channel: $name, URL: ${line.trim()}")
                    
                    val channel = Channel(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        description = category,
                        streamUrl = line.trim(),
                        logoUrl = logoUrl,
                        category = category
                    )
                    channels.add(channel)
                }
            }
        }
        
        android.util.Log.d("CarTestActivity", "Parsed ${channels.size} channels total")
        return channels
    }
    
    private fun extractChannelName(info: String): String {
        return info.substringAfterLast(",").trim()
    }
    
    private fun extractLogoUrl(info: String): String? {
        val logoPattern = """tvg-logo="([^"]+)"""".toRegex()
        return logoPattern.find(info)?.groupValues?.get(1)
    }
    
    private fun extractCategory(info: String): String {
        val categoryPattern = """group-title="([^"]+)"""".toRegex()
        return categoryPattern.find(info)?.groupValues?.get(1) ?: "General"
    }

    private fun createSampleChannels(): List<Channel> {
        return listOf(
            Channel("1", "CNN", "News Channel", "http://example.com/cnn", category = "News"),
            Channel("2", "BBC", "British Broadcasting", "http://example.com/bbc", category = "News"),
            Channel("3", "Fox News", "News Channel", "http://example.com/fox", category = "News"),
            Channel("4", "ESPN", "Sports Network", "http://example.com/espn", category = "Sports"),
            Channel("5", "HBO", "Premium Movies", "http://example.com/hbo", category = "Movies"),
            Channel("6", "Discovery", "Documentary", "http://example.com/discovery", category = "Documentary"),
            Channel("7", "NBC Sports", "Sports Network", "http://example.com/nbc", category = "Sports"),
            Channel("8", "Al Jazeera", "News Network", "http://example.com/aljazeera", category = "News"),
            Channel("9", "National Geographic", "Documentary", "http://example.com/natgeo", category = "Documentary"),
            Channel("10", "AMC", "Movies & TV", "http://example.com/amc", category = "Movies"),
            Channel("11", "Sky Sports", "Sports Channel", "http://example.com/sky", category = "Sports"),
            Channel("12", "CBSN", "News Network", "http://example.com/cbsn", category = "News"),
            Channel("13", "History", "History Channel", "http://example.com/history", category = "Documentary"),
            Channel("14", "Fox Sports", "Sports Network", "http://example.com/foxsports", category = "Sports"),
            Channel("15", "Showtime", "Premium TV", "http://example.com/showtime", category = "Movies"),
            Channel("16", "More Channels", "View All", "http://example.com/more", category = "General")
        )
    }
    
    
    
    private fun clearChannelCache() {
        // Clear all cached channel data to force fresh reload
        val prefs = getSharedPreferences("iptv_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        android.util.Log.d("CarTestActivity", "Channel cache cleared - will reload fresh M3U data")
    }
}