package com.carplayer.iptv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection

class NetworkBalancer(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkBalancer"
        private const val CONNECTION_TIMEOUT = 5000
        private const val READ_TIMEOUT = 10000
    }
    
    data class NetworkInfo(
        val type: String,
        val isConnected: Boolean,
        val hasInternet: Boolean,
        val supportsIPv4: Boolean,
        val supportsIPv6: Boolean,
        val network: Network?
    )
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    suspend fun getAvailableNetworks(): List<NetworkInfo> = withContext(Dispatchers.IO) {
        val networks = mutableListOf<NetworkInfo>()
        
        try {
            connectivityManager.allNetworks.forEach { network ->
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val networkInfo = connectivityManager.getNetworkInfo(network)
                
                if (capabilities != null && networkInfo != null) {
                    val type = when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        else -> "Unknown"
                    }
                    
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isConnected = networkInfo.isConnected
                    
                    // Test IPv4/IPv6 support
                    val (ipv4, ipv6) = testIPSupport(network)
                    
                    networks.add(NetworkInfo(
                        type = type,
                        isConnected = isConnected,
                        hasInternet = hasInternet,
                        supportsIPv4 = ipv4,
                        supportsIPv6 = ipv6,
                        network = network
                    ))
                    
                    Log.d(TAG, "Network: $type, Connected: $isConnected, Internet: $hasInternet, IPv4: $ipv4, IPv6: $ipv6")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting networks", e)
        }
        
        networks
    }
    
    private suspend fun testIPSupport(network: Network): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        var ipv4 = false
        var ipv6 = false
        
        try {
            // Test IPv4 connectivity
            val socket4 = Socket()
            try {
                network.bindSocket(socket4)
                socket4.connect(InetSocketAddress("8.8.8.8", 53), CONNECTION_TIMEOUT)
                ipv4 = true
                socket4.close()
            } catch (e: Exception) {
                Log.d(TAG, "IPv4 test failed: ${e.message}")
            }
            
            // Test IPv6 connectivity
            val socket6 = Socket()
            try {
                network.bindSocket(socket6)
                socket6.connect(InetSocketAddress("2001:4860:4860::8888", 53), CONNECTION_TIMEOUT)
                ipv6 = true
                socket6.close()
            } catch (e: Exception) {
                Log.d(TAG, "IPv6 test failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network binding failed", e)
        }
        
        Pair(ipv4, ipv6)
    }
    
    suspend fun testStreamUrl(url: String, preferredNetwork: Network? = null): StreamTestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Testing stream URL: $url")
        
        // First try with IPv4-only approach for IPv6 environments
        val result = tryWithIPv4Only(url)
        if (result.success) {
            Log.d(TAG, "IPv4-only approach successful")
            return@withContext result
        }
        
        Log.d(TAG, "IPv4-only failed, trying with network balancer")
        
        val networks = if (preferredNetwork != null) {
            listOf(preferredNetwork)
        } else {
            getAvailableNetworks().filter { it.isConnected && it.hasInternet }.mapNotNull { it.network }
        }
        
        var bestResult: StreamTestResult? = result // Keep IPv4-only result as fallback
        
        for (network in networks) {
            try {
                val startTime = System.currentTimeMillis()
                
                val connection = network.openConnection(URL(url))
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                
                // Set headers for IPTV streams
                connection.setRequestProperty("User-Agent", "Nordic-IPTV/1.0")
                connection.setRequestProperty("Accept", "*/*")
                connection.setRequestProperty("Connection", "keep-alive")
                
                // Force IPv4 when possible
                if (url.contains("fortv.cc") || url.contains("live-hls-web-aje.getaj.net")) {
                    connection.setRequestProperty("X-Forwarded-For", "8.8.8.8")
                }
                
                connection.connect()
                
                val responseTime = System.currentTimeMillis() - startTime
                val contentType = connection.contentType ?: "unknown"
                val contentLength = connection.contentLength
                
                Log.d(TAG, "Stream test successful - Response time: ${responseTime}ms, Content-Type: $contentType")
                
                val networkResult = StreamTestResult(
                    success = true,
                    responseTimeMs = responseTime,
                    contentType = contentType,
                    contentLength = contentLength,
                    network = network,
                    errorMessage = null
                )
                
                if (bestResult == null || !bestResult.success || responseTime < bestResult.responseTimeMs) {
                    bestResult = networkResult
                }
                
                connection.inputStream.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Stream test failed on network: ${e.message}", e)
            }
        }
        
        bestResult ?: StreamTestResult(
            success = false,
            responseTimeMs = -1,
            contentType = null,
            contentLength = -1,
            network = null,
            errorMessage = "IPv6 connectivity issue - all connection attempts failed"
        )
    }
    
    private suspend fun tryWithIPv4Only(url: String): StreamTestResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting IPv4-only connection to: $url")
            
            // Force IPv4 by setting system property
            System.setProperty("java.net.preferIPv4Stack", "true")
            System.setProperty("java.net.preferIPv6Addresses", "false")
            
            val startTime = System.currentTimeMillis()
            
            // Create connection without network binding
            val connection = URL(url).openConnection()
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            // Set headers for IPTV streams
            connection.setRequestProperty("User-Agent", "Nordic-IPTV/1.0")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Connection", "keep-alive")
            
            // Try to resolve to IPv4 address for problematic hosts
            if (url.contains("fortv.cc")) {
                try {
                    val host = "fortv.cc"
                    val addresses = InetAddress.getAllByName(host)
                    val ipv4Address = addresses.find { it is java.net.Inet4Address }
                    if (ipv4Address != null) {
                        Log.d(TAG, "Resolved $host to IPv4: ${ipv4Address.hostAddress}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "DNS resolution failed for fortv.cc: ${e.message}")
                }
            }
            
            connection.connect()
            
            val responseTime = System.currentTimeMillis() - startTime
            val contentType = connection.contentType ?: "unknown"
            val contentLength = connection.contentLength
            
            Log.d(TAG, "IPv4-only connection successful - Response time: ${responseTime}ms")
            
            connection.inputStream.close()
            
            StreamTestResult(
                success = true,
                responseTimeMs = responseTime,
                contentType = contentType,
                contentLength = contentLength,
                network = null,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "IPv4-only connection failed: ${e.message}", e)
            StreamTestResult(
                success = false,
                responseTimeMs = -1,
                contentType = null,
                contentLength = -1,
                network = null,
                errorMessage = "IPv4 fallback failed: ${e.message}"
            )
        }
    }
    
    data class StreamTestResult(
        val success: Boolean,
        val responseTimeMs: Long,
        val contentType: String?,
        val contentLength: Int,
        val network: Network?,
        val errorMessage: String?
    )
}