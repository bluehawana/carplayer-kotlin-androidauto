package com.carplayer.iptv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkBalancer = NetworkBalancer(context)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    private var onNetworkChangedCallback: ((NetworkBalancer.NetworkInfo?) -> Unit)? = null
    private var onStreamingProfileChangedCallback: ((NetworkBalancer.StreamingProfile) -> Unit)? = null
    
    fun setOnNetworkChangedCallback(callback: (NetworkBalancer.NetworkInfo?) -> Unit) {
        onNetworkChangedCallback = callback
    }
    
    fun setOnStreamingProfileChangedCallback(callback: (NetworkBalancer.StreamingProfile) -> Unit) {
        onStreamingProfileChangedCallback = callback
    }
    
    fun startMonitoring() {
        Log.d(TAG, "Starting network monitoring for adaptive streaming")
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network became available: $network")
                checkNetworkAndNotify()
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Log.d(TAG, "Network capabilities changed: $network")
                checkNetworkAndNotify()
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                checkNetworkAndNotify()
            }
        }
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            // Initial check
            checkNetworkAndNotify()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }
    
    private fun checkNetworkAndNotify() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentNetwork = networkBalancer.getCurrentNetworkType()
                val streamingProfile = networkBalancer.getOptimalStreamingProfile(currentNetwork)
                
                Log.d(TAG, "Network changed - Type: ${currentNetwork?.type}, " +
                          "Hotspot: ${currentNetwork?.isHotspot}, " +
                          "Metered: ${currentNetwork?.isMetered}, " +
                          "Profile: ${streamingProfile.description}")
                
                // Notify callbacks on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    onNetworkChangedCallback?.invoke(currentNetwork)
                    onStreamingProfileChangedCallback?.invoke(streamingProfile)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network status", e)
            }
        }
    }
    
    fun stopMonitoring() {
        Log.d(TAG, "Stopping network monitoring")
        try {
            networkCallback?.let { callback ->
                connectivityManager.unregisterNetworkCallback(callback)
            }
            networkCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network monitoring", e)
        }
    }
    
    fun getCurrentNetworkInfo(): NetworkBalancer.NetworkInfo? {
        return networkBalancer.getCurrentNetworkType()
    }
    
    fun getStreamingProfile(): NetworkBalancer.StreamingProfile {
        val currentNetwork = networkBalancer.getCurrentNetworkType()
        return networkBalancer.getOptimalStreamingProfile(currentNetwork)
    }
}