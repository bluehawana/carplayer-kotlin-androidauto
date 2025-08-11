package com.carplayer.iptv

import android.util.Log
import java.net.InetAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DnsResolver(private val timeoutMs: Int) {
    companion object {
        private const val TAG = "DnsResolver"
    }
    
    fun getAllByName(host: String): Array<InetAddress> {
        try {
            val future = CompletableFuture.supplyAsync {
                InetAddress.getAllByName(host)
            }
            
            return future.get(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed for $host: ${e.message}")
            return emptyArray()
        }
    }
}