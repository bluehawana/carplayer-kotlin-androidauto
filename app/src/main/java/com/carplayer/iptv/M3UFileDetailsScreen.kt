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

class M3UFileDetailsScreen(
    carContext: CarContext,
    private val metadata: M3UFileManager.M3UFileMetadata
) : Screen(carContext) {
    
    private val m3uFileManager = M3UFileManager(carContext)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private fun showToast(message: String) {
        androidx.car.app.CarToast.makeText(carContext, message, androidx.car.app.CarToast.LENGTH_SHORT).show()
    }
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        // File information
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("File Details")
                .addText("Name: ${metadata.displayName}")
                .addText("Channels: ${metadata.channelCount}")
                .addText("Size: ${formatFileSize(metadata.fileSize)}")
                .addText("Imported: ${metadata.importDate}")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_info)
                    ).build()
                )
                .build()
        )
        
        // Categories
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("Categories")
                .addText(metadata.categories.joinToString(", "))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_category)
                    ).build()
                )
                .build()
        )
        
        // Actions
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("Load Channels")
                .addText("View channels from this playlist")
                .setOnClickListener {
                    loadChannels()
                }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_play_arrow)
                    ).build()
                )
                .build()
        )
        
        // Update option (if source URL exists)
        if (metadata.sourceUrl != null) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle("Update Playlist")
                    .addText("Refresh from source URL")
                    .setOnClickListener {
                        updatePlaylist()
                    }
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_refresh)
                        ).build()
                    )
                    .build()
            )
        }
        
        // Delete option
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("Delete Playlist")
                .addText("Remove this M3U file")
                .setOnClickListener {
                    deletePlaylist()
                }
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_delete)
                    ).build()
                )
                .build()
        )
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle(metadata.displayName)
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Back")
                            .setOnClickListener {
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun loadChannels() {
        coroutineScope.launch {
            val channels = m3uFileManager.loadM3UFile(metadata.fileName)
            if (channels != null) {
                screenManager.push(GridChannelScreen(carContext, channels, metadata.displayName))
            } else {
                showToast("Failed to load channels")
            }
        }
    }
    
    private fun updatePlaylist() {
        coroutineScope.launch {
            val updatedMetadata = m3uFileManager.updateM3UFile(metadata.fileName, metadata.sourceUrl)
            if (updatedMetadata != null) {
                showToast("Playlist updated successfully")
                invalidate()
            } else {
                showToast("Failed to update playlist")
            }
        }
    }
    
    private fun deletePlaylist() {
        coroutineScope.launch {
            val deleted = m3uFileManager.deleteM3UFile(metadata.fileName)
            if (deleted) {
                showToast("Playlist deleted")
                screenManager.pop()
            } else {
                showToast("Failed to delete playlist")
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
}