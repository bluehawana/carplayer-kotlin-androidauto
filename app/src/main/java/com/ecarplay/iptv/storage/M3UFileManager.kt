package com.ecarplay.iptv.storage

import android.content.Context
import android.os.Environment
import com.ecarplay.iptv.Channel
import com.ecarplay.iptv.IPTVSubscription
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class M3UFileManager(private val context: Context) {
    
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    companion object {
        private const val M3U_FOLDER_NAME = "IPTV_M3U_Files"
        private const val METADATA_FILE_NAME = "metadata.json"
    }
    
    data class M3UFileMetadata(
        val fileName: String,
        val displayName: String,
        val sourceUrl: String?,
        val importDate: String,
        val channelCount: Int,
        val categories: List<String>,
        val fileSize: Long,
        val isActive: Boolean = true
    )
    
    private fun getM3UDirectory(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val m3uDir = File(externalDir, M3U_FOLDER_NAME)
        if (!m3uDir.exists()) {
            m3uDir.mkdirs()
        }
        return m3uDir
    }
    
    suspend fun saveM3UFile(
        content: String,
        displayName: String,
        sourceUrl: String? = null
    ): M3UFileMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = dateFormat.format(Date())
                val sanitizedName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val fileName = "${sanitizedName}_$timestamp.m3u"
                
                val m3uDir = getM3UDirectory()
                val file = File(m3uDir, fileName)
                
                file.writeText(content)
                
                // Parse content to get metadata
                val channels = parseM3UContent(content)
                val categories = channels.map { it.category }.distinct()
                
                val metadata = M3UFileMetadata(
                    fileName = fileName,
                    displayName = displayName,
                    sourceUrl = sourceUrl,
                    importDate = timestamp,
                    channelCount = channels.size,
                    categories = categories,
                    fileSize = file.length(),
                    isActive = true
                )
                
                // Save metadata
                saveMetadata(metadata)
                
                metadata
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun downloadAndSaveM3U(
        url: String,
        displayName: String
    ): M3UFileMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val content = URL(url).readText()
                saveM3UFile(content, displayName, url)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun getAllM3UFiles(): List<M3UFileMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val metadataFile = File(getM3UDirectory(), METADATA_FILE_NAME)
                if (metadataFile.exists()) {
                    val json = metadataFile.readText()
                    val metadataList = gson.fromJson(json, Array<M3UFileMetadata>::class.java)
                    metadataList.toList().filter { it.isActive }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun loadM3UFile(fileName: String): List<Channel>? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getM3UDirectory(), fileName)
                if (file.exists()) {
                    val content = file.readText()
                    parseM3UContent(content)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun deleteM3UFile(fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getM3UDirectory(), fileName)
                val deleted = file.delete()
                
                if (deleted) {
                    // Update metadata to mark as inactive
                    val allMetadata = getAllM3UFiles().toMutableList()
                    val updatedMetadata = allMetadata.map { metadata ->
                        if (metadata.fileName == fileName) {
                            metadata.copy(isActive = false)
                        } else {
                            metadata
                        }
                    }
                    saveAllMetadata(updatedMetadata)
                }
                
                deleted
            } catch (e: Exception) {
                false
            }
        }
    }
    
    suspend fun updateM3UFile(
        fileName: String,
        sourceUrl: String? = null
    ): M3UFileMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = getAllM3UFiles().find { it.fileName == fileName }
                if (metadata != null && sourceUrl != null) {
                    // Download updated content
                    val content = URL(sourceUrl).readText()
                    val file = File(getM3UDirectory(), fileName)
                    file.writeText(content)
                    
                    // Update metadata
                    val channels = parseM3UContent(content)
                    val categories = channels.map { it.category }.distinct()
                    
                    val updatedMetadata = metadata.copy(
                        channelCount = channels.size,
                        categories = categories,
                        fileSize = file.length(),
                        importDate = dateFormat.format(Date())
                    )
                    
                    saveMetadata(updatedMetadata)
                    updatedMetadata
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun parseM3UContent(content: String): List<Channel> {
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
    
    private suspend fun saveMetadata(metadata: M3UFileMetadata) {
        val allMetadata = getAllM3UFiles().toMutableList()
        val existingIndex = allMetadata.indexOfFirst { it.fileName == metadata.fileName }
        
        if (existingIndex >= 0) {
            allMetadata[existingIndex] = metadata
        } else {
            allMetadata.add(metadata)
        }
        
        saveAllMetadata(allMetadata)
    }
    
    private fun saveAllMetadata(metadataList: List<M3UFileMetadata>) {
        try {
            val metadataFile = File(getM3UDirectory(), METADATA_FILE_NAME)
            val json = gson.toJson(metadataList)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun getStorageInfo(): StorageInfo {
        val m3uDir = getM3UDirectory()
        val totalSize = m3uDir.listFiles()?.sumOf { it.length() } ?: 0
        val fileCount = m3uDir.listFiles()?.size ?: 0
        
        return StorageInfo(
            totalFiles = fileCount,
            totalSizeBytes = totalSize,
            availableSpace = m3uDir.freeSpace,
            directoryPath = m3uDir.absolutePath
        )
    }
    
    data class StorageInfo(
        val totalFiles: Int,
        val totalSizeBytes: Long,
        val availableSpace: Long,
        val directoryPath: String
    )
}