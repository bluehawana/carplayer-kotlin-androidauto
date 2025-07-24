package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class M3UFileListScreen(carContext: CarContext) : Screen(carContext) {
    
    private val m3uFileManager = M3UFileManager(carContext)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var m3uFiles: List<M3UFileManager.M3UFileMetadata> = emptyList()
    
    init {
        loadM3UFiles()
        // Auto-import built-in M3U file if no files exist
        coroutineScope.launch {
            if (m3uFiles.isEmpty()) {
                val importer = M3UImporter(carContext)
                importer.importProjectM3UFiles()
                loadM3UFiles() // Reload after import
            }
        }
    }
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        if (m3uFiles.isEmpty()) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle("No M3U files found")
                    .addText("Import some M3U files to get started")
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_info)
                        ).build()
                    )
                    .build()
            )
        } else {
            m3uFiles.forEach { metadata ->
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(metadata.displayName)
                        .addText("${metadata.channelCount} channels • ${formatFileSize(metadata.fileSize)}")
                        .addText("Categories: ${metadata.categories.take(3).joinToString(", ")}")
                        .setOnClickListener {
                            screenManager.push(M3UFileDetailsScreen(carContext, metadata))
                        }
                        .setImage(
                            CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_playlist)
                            ).build()
                        )
                        .build()
                )
            }
        }
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle("M3U Files")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Import New")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_add)
                                ).build()
                            )
                            .setOnClickListener {
                                screenManager.push(ImportScreen(carContext))
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Refresh")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_refresh)
                                ).build()
                            )
                            .setOnClickListener {
                                loadM3UFiles()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun loadM3UFiles() {
        coroutineScope.launch {
            m3uFiles = m3uFileManager.getAllM3UFiles()
            invalidate()
        }
    }
    
    private fun loadChannelsFromFile(metadata: M3UFileManager.M3UFileMetadata) {
        coroutineScope.launch {
            val channels = m3uFileManager.loadM3UFile(metadata.fileName)
            if (channels != null) {
                screenManager.push(ChannelListScreen(carContext, channels, metadata.displayName))
            } else {
                showToast("Failed to load channels from file")
            }
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        
        return when {
            mb > 0 -> "${mb}MB"
            kb > 0 -> "${kb}KB"
            else -> "${bytes}B"
        }
    }
    
    private fun showToast(message: String) {
        androidx.car.app.CarToast.makeText(carContext, message, androidx.car.app.CarToast.LENGTH_SHORT).show()
    }
}