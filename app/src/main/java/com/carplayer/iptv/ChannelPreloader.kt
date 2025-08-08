package com.carplayer.iptv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ChannelPreloader(private val context: Context) {
    
    companion object {
        private const val TAG = "ChannelPreloader"
        private const val MAX_PRELOADED_CHANNELS = 3
        private const val PRELOAD_TIMEOUT_MS = 10000L
    }
    
    private val networkBalancer = NetworkBalancer(context)
    private val preloadedChannels = ConcurrentHashMap<String, PreloadResult>()
    private val activePreloadJobs = ConcurrentHashMap<String, Job>()
    
    data class PreloadResult(
        val url: String,
        val isWorking: Boolean,
        val responseTimeMs: Long,
        val contentType: String?,
        val timestamp: Long,
        val networkType: String
    )
    
    fun shouldPreload(networkInfo: NetworkBalancer.NetworkInfo?): Boolean {
        return networkInfo?.isHotspot == true || 
               networkInfo?.type == "Mobile Data" ||
               (networkInfo?.bandwidth != -1L && networkInfo?.bandwidth ?: -1L < 10000) // < 10Mbps
    }
    
    fun preloadChannels(channels: List<String>, currentChannelUrl: String) {
        val networkInfo = networkBalancer.getCurrentNetworkType()
        
        if (!shouldPreload(networkInfo)) {
            Log.d(TAG, "Preloading disabled for current network: ${networkInfo?.type}")
            return
        }
        
        Log.d(TAG, "Starting channel preloading for ${networkInfo?.type} connection")
        
        // Clear old preloaded channels
        cleanupOldPreloads()
        
        // Get next channels to preload (skip current channel)
        val channelsToPreload = channels
            .filter { it != currentChannelUrl }
            .take(MAX_PRELOADED_CHANNELS)
        
        channelsToPreload.forEach { channelUrl ->
            preloadChannel(channelUrl, networkInfo?.type ?: "Unknown")
        }
    }
    
    private fun preloadChannel(url: String, networkType: String) {
        // Cancel existing preload job for this URL
        activePreloadJobs[url]?.cancel()
        
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Preloading channel: $url")
                
                val startTime = System.currentTimeMillis()
                val testResult = networkBalancer.testStreamUrl(url)
                
                if (isActive) {
                    val preloadResult = PreloadResult(
                        url = url,
                        isWorking = testResult.success,
                        responseTimeMs = testResult.responseTimeMs,
                        contentType = testResult.contentType,
                        timestamp = System.currentTimeMillis(),
                        networkType = networkType
                    )
                    
                    preloadedChannels[url] = preloadResult
                    
                    Log.d(TAG, "Channel preload ${if (testResult.success) "successful" else "failed"}: $url " +
                              "(${testResult.responseTimeMs}ms)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading channel: $url", e)
                preloadedChannels[url] = PreloadResult(
                    url = url,
                    isWorking = false,
                    responseTimeMs = -1,
                    contentType = null,
                    timestamp = System.currentTimeMillis(),
                    networkType = networkType
                )
            } finally {
                activePreloadJobs.remove(url)
            }
        }
        
        activePreloadJobs[url] = job
        
        // Cancel job after timeout
        CoroutineScope(Dispatchers.IO).launch {
            delay(PRELOAD_TIMEOUT_MS)
            if (job.isActive) {
                Log.d(TAG, "Preload timeout for: $url")
                job.cancel()
                activePreloadJobs.remove(url)
            }
        }
    }
    
    private fun cleanupOldPreloads() {
        val currentTime = System.currentTimeMillis()
        val maxAge = 5 * 60 * 1000L // 5 minutes
        
        preloadedChannels.entries.removeAll { (url, result) ->
            val isOld = currentTime - result.timestamp > maxAge
            if (isOld) {
                Log.d(TAG, "Removing old preload result: $url")
            }
            isOld
        }
    }
    
    fun getPreloadResult(url: String): PreloadResult? {
        return preloadedChannels[url]
    }
    
    fun isChannelPreloaded(url: String): Boolean {
        val result = preloadedChannels[url]
        return result != null && result.isWorking
    }
    
    fun getPreloadedChannelsInfo(): Map<String, PreloadResult> {
        return preloadedChannels.toMap()
    }
    
    fun getChannelRecommendation(url: String): String? {
        val preloadResult = getPreloadResult(url)
        
        return when {
            preloadResult == null -> "⚠️ Channel not tested"
            preloadResult.isWorking -> {
                when {
                    preloadResult.responseTimeMs < 1000 -> "✅ Fast connection (${preloadResult.responseTimeMs}ms)"
                    preloadResult.responseTimeMs < 3000 -> "🟡 Good connection (${preloadResult.responseTimeMs}ms)"
                    preloadResult.responseTimeMs < 5000 -> "🟠 Slow connection (${preloadResult.responseTimeMs}ms)"
                    else -> "🔴 Very slow (${preloadResult.responseTimeMs}ms)"
                }
            }
            else -> "❌ Channel may not work"
        }
    }
    
    fun stopAllPreloading() {
        Log.d(TAG, "Stopping all channel preloading")
        
        // Cancel all active jobs
        activePreloadJobs.values.forEach { job ->
            job.cancel()
        }
        activePreloadJobs.clear()
    }
    
    fun clearCache() {
        stopAllPreloading()
        preloadedChannels.clear()
        Log.d(TAG, "Channel preload cache cleared")
    }
}