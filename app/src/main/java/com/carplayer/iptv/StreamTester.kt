package com.carplayer.iptv

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLConnection

class StreamTester {
    
    companion object {
        private const val TAG = "StreamTester"
        
        // Known working test streams for validation
        val TEST_STREAMS = listOf(
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8" to "Akamai Test Stream",
            "https://demo-i.akamaihd.net/hls/live/255608/earthcam_network01/playlist.m3u8" to "EarthCam Demo",
            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8" to "Bitdash Demo",
            "https://d2zihajmogu5jn.cloudfront.net/bipbop-advanced/bipbop_16x9_variant.m3u8" to "Apple Demo Stream",
            "https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8" to "Big Buck Bunny"
        )
        
        
    }
    
    suspend fun testWorkingStreams(): List<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, Boolean>>()
        
        for ((url, name) in TEST_STREAMS) {
            try {
                Log.d(TAG, "Testing known good stream: $name")
                val connection = URL(url).openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Nordic-IPTV/1.0")
                
                connection.connect()
                val success = connection.contentType?.contains("application") == true || 
                             connection.contentType?.contains("video") == true ||
                             url.endsWith(".m3u8")
                
                connection.inputStream.close()
                results.add(name to success)
                Log.d(TAG, "Test stream $name: ${if (success) "SUCCESS" else "FAILED"}")
                
            } catch (e: Exception) {
                Log.w(TAG, "Test stream $name failed: ${e.message}")
                results.add(name to false)
            }
        }
        
        results
    }
    
    
    
    suspend fun validateIPTVStream(url: String): StreamValidationResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            // Set IPTV-specific headers
            connection.setRequestProperty("User-Agent", "Nordic-IPTV/1.0")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Connection", "keep-alive")
            
            connection.connect()
            
            val contentType = connection.contentType
            val responseCode = if (connection is java.net.HttpURLConnection) {
                connection.responseCode
            } else {
                200
            }
            
            val isValidStream = when {
                responseCode in 200..299 -> true
                contentType?.contains("application/vnd.apple.mpegurl") == true -> true
                contentType?.contains("application/x-mpegURL") == true -> true
                contentType?.contains("video/") == true -> true
                url.endsWith(".m3u8") -> true
                url.endsWith(".ts") -> true
                else -> false
            }
            
            connection.inputStream.close()
            
            StreamValidationResult(
                isValid = isValidStream,
                contentType = contentType,
                responseCode = responseCode,
                error = null
            )
            
        } catch (e: Exception) {
            StreamValidationResult(
                isValid = false,
                contentType = null,
                responseCode = -1,
                error = e.message
            )
        }
    }
    
    data class StreamValidationResult(
        val isValid: Boolean,
        val contentType: String?,
        val responseCode: Int,
        val error: String?
    )
}