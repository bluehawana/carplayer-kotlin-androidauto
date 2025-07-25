package com.carplayer.iptv

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import com.carplayer.iptv.models.Channel
import com.carplayer.iptv.models.IPTVSubscription

class ChannelManager {
    
    private val gson = Gson()
    private var channels: List<Channel> = emptyList()
    private var subscriptions: List<IPTVSubscription> = emptyList()
    
    fun getChannels(): List<Channel> {
        return channels
    }
    
    fun getSubscriptions(): List<IPTVSubscription> {
        return subscriptions
    }
    
    suspend fun importSubscription(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val content = URL(url).readText()
                val parsedChannels = parseM3U(content)
                val subscription = IPTVSubscription(
                    name = "Imported Subscription",
                    url = url,
                    channels = parsedChannels
                )
                
                subscriptions = subscriptions + subscription
                channels = channels + parsedChannels
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n")
        
        var currentChannel: Channel? = null
        var channelInfo = ""
        
        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    channelInfo = line.substringAfter("#EXTINF:")
                }
                line.startsWith("http") -> {
                    val name = extractChannelName(channelInfo)
                    val logoUrl = extractLogoUrl(channelInfo)
                    val category = extractCategory(channelInfo)
                    
                    currentChannel = Channel(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        description = category,
                        streamUrl = line.trim(),
                        logoUrl = logoUrl,
                        category = category
                    )
                    channels.add(currentChannel)
                }
            }
        }
        
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
    
    fun saveSubscriptions(context: Context) {
        val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        val json = gson.toJson(subscriptions)
        prefs.edit().putString("subscriptions", json).apply()
    }
    
    fun loadSubscriptions(context: Context) {
        val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("subscriptions", null)
        if (json != null) {
            val type = object : TypeToken<List<IPTVSubscription>>() {}.type
            subscriptions = gson.fromJson(json, type)
            channels = subscriptions.flatMap { it.channels }
        }
    }
    
    fun getAllChannels(context: Context): List<Channel> {
        // ALWAYS reload from assets to get latest M3U content
        android.util.Log.d("ChannelManager", "getAllChannels called - force loading from assets")
        loadFromAssets(context)
        
        android.util.Log.d("ChannelManager", "Returning ${channels.size} channels")
        return channels
    }
    
    private fun loadFromAssets(context: Context) {
        try {
            val inputStream = context.assets.open("iptv.m3u")
            val content = inputStream.bufferedReader().readText()
            inputStream.close()
            
            val parsedChannels = parseM3U(content)
            val assetSubscription = IPTVSubscription(
                name = "Default IPTV Channels",
                url = "assets://iptv.m3u",
                channels = parsedChannels
            )
            
            subscriptions = listOf(assetSubscription)
            channels = parsedChannels
            
            android.util.Log.d("ChannelManager", "Loaded ${channels.size} channels from assets")
        } catch (e: Exception) {
            android.util.Log.e("ChannelManager", "Failed to load from assets: ${e.message}")
            channels = emptyList()
        }
    }
    
    fun reloadFromAssets(context: Context) {
        // Force reload from assets (ignores cache completely)
        channels = emptyList() // Clear current channels
        subscriptions = emptyList() // Clear current subscriptions
        loadFromAssets(context)
        saveSubscriptions(context) // Save the reloaded channels
        android.util.Log.d("ChannelManager", "Force reloaded - now have ${channels.size} channels")
    }
}