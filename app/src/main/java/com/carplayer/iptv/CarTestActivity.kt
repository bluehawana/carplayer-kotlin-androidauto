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
import com.carplayer.iptv.models.Channel
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarTestActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter
    private val m3uFileManager = M3UFileManager(this)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple grid layout programmatically with Nordic theme
        recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@CarTestActivity, 4) // 4 columns like APTV
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFFb1d4e0.toInt()) // Baby Blue Nordic background
        }
        
        setContentView(recyclerView)
        
        // Set title
        title = "ECarTV - Test Interface"
        
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
                
                // Try to load from assets directly first
                val channels = try {
                    android.util.Log.d("CarTestActivity", "Loading M3U directly from assets...")
                    val content = assets.open("iptv.m3u").bufferedReader().use { it.readText() }
                    android.util.Log.d("CarTestActivity", "M3U content length: ${content.length}")
                    
                    val parsedChannels = parseM3UContent(content)
                    android.util.Log.d("CarTestActivity", "Parsed ${parsedChannels.size} channels from assets")
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
                    channelAdapter = ChannelAdapter(channels) // Show all channels
                    recyclerView.adapter = channelAdapter
                    
                    // Update title to show channel count
                    runOnUiThread {
                        title = "ECarTV - ${channels.size} Channels (ALL)"
                    }
                } else {
                    android.util.Log.w("CarTestActivity", "No channels loaded, using sample data")
                    // Create sample channels if loading fails
                    val sampleChannels = createSampleChannels()
                    channelAdapter = ChannelAdapter(sampleChannels)
                    recyclerView.adapter = channelAdapter
                    
                    runOnUiThread {
                        title = "ECarTV - Sample Data (No M3U)"
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CarTestActivity", "Failed to load channels", e)
                // Create sample channels if loading fails
                val sampleChannels = createSampleChannels()
                channelAdapter = ChannelAdapter(sampleChannels)
                recyclerView.adapter = channelAdapter
                
                runOnUiThread {
                    title = "ECarTV - Error Loading"
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
    
    private class ChannelAdapter(private val channels: List<Channel>) : 
        RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                android.R.layout.simple_list_item_2, parent, false
            )
            return ChannelViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            val channel = channels[position]
            holder.bind(channel)
        }
        
        override fun getItemCount() = channels.size
        
        class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleView: TextView = itemView.findViewById(android.R.id.text1)
            private val subtitleView: TextView = itemView.findViewById(android.R.id.text2)
            
            fun bind(channel: Channel) {
                titleView.text = channel.name
                subtitleView.text = "${channel.category} • HD"
                
                // Nordic Ice Age Theme Colors
                val backgroundColor = when (channel.category) {
                    "News" -> 0xFF145da0.toInt()        // Midnight Blue
                    "Sports" -> 0xFF2e8bc0.toInt()      // Blue  
                    "Movies" -> 0xFF0c2d48.toInt()      // Dark Blue
                    "Documentary" -> 0xFF145da0.toInt() // Midnight Blue
                    "General" -> 0xFFb1d4e0.toInt()    // Baby Blue
                    else -> 0xFF2e8bc0.toInt()         // Default Blue
                }
                itemView.setBackgroundColor(backgroundColor)
                
                // Set text color for Nordic feel - white for dark backgrounds, dark for light
                val textColor = when (channel.category) {
                    "General" -> 0xFF0c2d48.toInt()    // Dark Blue text on Baby Blue background
                    else -> 0xFFFFFFFF.toInt()         // White text on dark backgrounds
                }
                titleView.setTextColor(textColor)
                subtitleView.setTextColor(textColor)
                
                // Add padding for better appearance
                itemView.setPadding(16, 16, 16, 16)
                
                // Add click listener to test playback
                itemView.setOnClickListener {
                    android.util.Log.d("CarTestActivity", "Clicked channel: ${channel.name}")
                    android.util.Log.d("CarTestActivity", "Stream URL: ${channel.streamUrl}")
                    
                    // Launch our custom Nordic video player
                    try {
                        val intent = android.content.Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                            putExtra("CHANNEL_NAME", channel.name)
                            putExtra("STREAM_URL", channel.streamUrl)
                        }
                        itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("CarTestActivity", "Failed to launch player", e)
                        android.widget.Toast.makeText(
                            itemView.context, 
                            "Selected: ${channel.name}\nTap to play stream", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}