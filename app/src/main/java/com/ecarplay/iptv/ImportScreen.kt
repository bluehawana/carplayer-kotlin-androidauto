package com.ecarplay.iptv

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
import com.ecarplay.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportScreen(carContext: CarContext) : Screen(carContext) {
    
    private val channelManager = ChannelManager()
    private val m3uFileManager = M3UFileManager(carContext)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        // Add common IPTV subscription options
        val commonSubscriptions = listOf(
            "Load Built-in Channels",
            "Manual URL Entry",
            "From Phone Storage",
            "Popular Providers"
        )
        
        commonSubscriptions.forEach { option ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(option)
                    .addText("Import subscription from $option")
                    .setOnClickListener {
                        when (option) {
                            "Load Built-in Channels" -> handleBuiltInChannels()
                            "Manual URL Entry" -> handleManualUrlEntry()
                            "From Phone Storage" -> handleFileImport()
                            "Popular Providers" -> handlePopularProviders()
                        }
                    }
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_import)
                        ).build()
                    )
                    .build()
            )
        }
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle("Import Subscription")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Cancel")
                            .setOnClickListener {
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun handleManualUrlEntry() {
        // In a real implementation, this would show a text input dialog
        // For now, we'll use a predefined URL for demonstration
        val demoUrl = "https://example.com/playlist.m3u"
        importSubscription(demoUrl)
    }
    
    private fun handleFileImport() {
        // This would typically open a file picker on the phone
        // For car safety, we might need to handle this differently
        showToast("Use phone app to import from file")
    }
    
    private fun importSubscription(url: String) {
        coroutineScope.launch {
            // Save M3U file locally
            val metadata = m3uFileManager.downloadAndSaveM3U(url, "Imported Playlist")
            
            if (metadata != null) {
                // Also import to channel manager for backward compatibility
                val success = channelManager.importSubscription(url)
                if (success) {
                    channelManager.saveSubscriptions(carContext)
                }
                
                showToast("M3U file saved with ${metadata.channelCount} channels")
                screenManager.pop()
            } else {
                showToast("Failed to import M3U file")
            }
        }
    }
    
    private fun handleBuiltInChannels() {
        coroutineScope.launch {
            val m3uImporter = M3UImporter(carContext)
            val importedFiles = m3uImporter.importProjectM3UFiles()
            
            if (importedFiles.isNotEmpty()) {
                showToast("Loaded ${importedFiles.first().channelCount} channels including BBC, CNN, Fox News")
                screenManager.pop()
            } else {
                showToast("Failed to load built-in channels")
            }
        }
    }
    
    private fun handlePopularProviders() {
        showToast("Popular providers feature coming soon")
    }
    
    private fun showToast(message: String) {
        androidx.car.app.CarToast.makeText(carContext, message, androidx.car.app.CarToast.LENGTH_SHORT).show()
    }
}