package com.carplayer.iptv

import android.content.Context
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class M3UImporter(private val context: Context) {
    
    private val m3uFileManager = M3UFileManager(context)
    
    suspend fun importLocalM3UFile(filePath: String, displayName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext false
                }
                
                val content = file.readText()
                val metadata = m3uFileManager.saveM3UFile(content, displayName)
                
                metadata != null
            } catch (e: Exception) {
                false
            }
        }
    }
    
    suspend fun importProjectM3UFiles(): List<M3UFileManager.M3UFileMetadata> {
        return withContext(Dispatchers.IO) {
            val importedFiles = mutableListOf<M3UFileManager.M3UFileMetadata>()
            
            try {
                // Import the iptv.m3u file from assets
                val assetManager = context.assets
                val content = assetManager.open("iptv.m3u").bufferedReader().use { it.readText() }
                
                android.util.Log.d("M3UImporter", "Loaded M3U content, length: ${content.length}")
                android.util.Log.d("M3UImporter", "First 200 chars: ${content.take(200)}")
                
                val metadata = m3uFileManager.saveM3UFile(content, "IPTV Channels")
                if (metadata != null) {
                    android.util.Log.d("M3UImporter", "Successfully saved M3U file: ${metadata.fileName}")
                    importedFiles.add(metadata)
                } else {
                    android.util.Log.e("M3UImporter", "Failed to save M3U file")
                }
            } catch (e: Exception) {
                android.util.Log.e("M3UImporter", "Failed to import M3U file", e)
            }
            
            importedFiles
        }
    }
    
    suspend fun getChannelStats(m3uContent: String): ChannelStats {
        return withContext(Dispatchers.IO) {
            val lines = m3uContent.split("\n")
            val channels = mutableListOf<String>()
            val categories = mutableSetOf<String>()
            
            lines.forEach { line ->
                when {
                    line.startsWith("#EXTINF:") -> {
                        val groupTitle = extractGroupTitle(line)
                        if (groupTitle.isNotEmpty()) {
                            categories.add(groupTitle)
                        }
                        
                        val channelName = extractChannelName(line)
                        if (channelName.isNotEmpty()) {
                            channels.add(channelName)
                        }
                    }
                }
            }
            
            ChannelStats(
                totalChannels = channels.size,
                categories = categories.toList(),
                sampleChannels = channels.take(10)
            )
        }
    }
    
    private fun extractGroupTitle(line: String): String {
        val groupPattern = """group-title="([^"]+)"""".toRegex()
        return groupPattern.find(line)?.groupValues?.get(1) ?: ""
    }
    
    private fun extractChannelName(line: String): String {
        return line.substringAfterLast(",").trim()
    }
    
    data class ChannelStats(
        val totalChannels: Int,
        val categories: List<String>,
        val sampleChannels: List<String>
    )
}