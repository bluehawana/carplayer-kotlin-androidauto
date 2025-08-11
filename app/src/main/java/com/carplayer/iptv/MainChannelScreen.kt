package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.carplayer.iptv.models.Channel
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainChannelScreen(carContext: CarContext) : Screen(carContext) {
    
    private val m3uFileManager = M3UFileManager(carContext)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var channels: List<Channel> = emptyList()
    private var isLoading = true
    
    init {
        loadChannelsFromBuiltInFile()
    }
    
    override fun onGetTemplate(): Template {
        if (isLoading) {
            return createLoadingTemplate()
        }
        
        if (channels.isEmpty()) {
            return createImportTemplate()
        }
        
        // Use list template for better scrolling with many channels
        return if (channels.size > 20) {
            createChannelListTemplate()
        } else {
            createChannelGridTemplate()
        }
    }
    
    private fun createLoadingTemplate(): Template {
        return GridTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Loading Channels...")
                            .setText("Please wait")
                            .setImage(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_refresh)
                                ).build()
                            )
                            .build()
                    )
                    .build()
            )
            .setTitle(carContext.getString(R.string.app_name))
            .build()
    }
    
    private fun createImportTemplate(): Template {
        return GridTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Import Channels")
                            .setText("Load IPTV Playlist")
                            .setImage(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, R.drawable.ic_import)
                                ).build()
                            )
                            .setOnClickListener {
                                screenManager.push(ImportScreen(carContext))
                            }
                            .build()
                    )
                    .build()
            )
            .setTitle(carContext.getString(R.string.app_name))
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Load Built-in")
                            .setOnClickListener {
                                loadChannelsFromBuiltInFile()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun createChannelGridTemplate(): Template {
        val gridItemListBuilder = ItemList.Builder()
        
        // Show all channels instead of limiting to 16
        channels.forEachIndexed { _, channel ->
            val displayTitle = if (channel.channelNumber != null) {
                "${channel.channelNumber} - ${channel.name}"
            } else {
                channel.name
            }
            
            val gridItem = GridItem.Builder()
                .setTitle(displayTitle)
                .setText(getChannelInfo(channel))
                .setImage(getChannelThumbnail(channel))
                .setOnClickListener {
                    android.util.Log.d("MainChannelScreen", "Attempting to play channel: ${channel.name}")
                    try {
                        screenManager.push(PlayerScreen(carContext, channel, channels))
                    } catch (e: Exception) {
                        android.util.Log.e("MainChannelScreen", "Failed to start PlayerScreen", e)
                        // Fallback to channel list if PlayerScreen fails
                        screenManager.push(ChannelListScreen(carContext, channels, "All Channels"))
                    }
                }
                .build()
            
            gridItemListBuilder.addItem(gridItem)
        }
        
        return GridTemplate.Builder()
            .setSingleList(gridItemListBuilder.build())
            .setTitle("❄️ Car TV Player - ${channels.size} Channels")
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Categories")
                            .setOnClickListener {
                                screenManager.push(CategoryScreen(carContext, channels))
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("All Channels")
                            .setOnClickListener {
                                try {
                                    screenManager.push(ChannelListScreen(carContext, channels, "All Channels"))
                                } catch (e: Exception) {
                                    // If ChannelListScreen fails, stay on current screen
                                    invalidate()
                                }
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Settings")
                            .setOnClickListener {
                                try {
                                    screenManager.push(M3UFileListScreen(carContext))
                                } catch (e: Exception) {
                                    // If settings fails, show import screen instead
                                    screenManager.push(ImportScreen(carContext))
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun createChannelListTemplate(): Template {
        val listBuilder = ItemList.Builder()
        
        channels.forEach { channel ->
            val displayTitle = if (channel.channelNumber != null) {
                "${channel.channelNumber} - ${channel.name}"
            } else {
                channel.name
            }
            
            val row = Row.Builder()
                .setTitle(displayTitle)
                .addText(getChannelInfo(channel))
                .addText("Category: ${getChannelCategory(channel)}")
                .setImage(getChannelThumbnail(channel))
                .setOnClickListener {
                    android.util.Log.d("MainChannelScreen", "List: Attempting to play channel: ${channel.name}")
                    android.util.Log.d("MainChannelScreen", "Stream URL: ${channel.streamUrl}")
                    try {
                        screenManager.push(PlayerScreen(carContext, channel, channels))
                    } catch (e: Exception) {
                        android.util.Log.e("MainChannelScreen", "Failed to start PlayerScreen from list", e)
                    }
                }
                .build()
            
            listBuilder.addItem(row)
        }
        
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("❄️ Car TV Player - ${channels.size} Channels")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Grid View")
                            .setOnClickListener {
                                // Force grid view
                                invalidate()
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Categories")
                            .setOnClickListener {
                                screenManager.push(CategoryScreen(carContext, channels))
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
    
    private fun loadChannelsFromBuiltInFile() {
        isLoading = true
        invalidate()
        
        coroutineScope.launch {
            try {
                android.util.Log.d("MainChannelScreen", "Starting to load channels...")
                
                // First try to auto-import the built-in M3U file
                val importer = M3UImporter(carContext)
                val importedFiles = importer.importProjectM3UFiles()
                
                android.util.Log.d("MainChannelScreen", "Imported files count: ${importedFiles.size}")
                
                if (importedFiles.isNotEmpty()) {
                    // Load channels from the imported file
                    val fileName = importedFiles.first().fileName
                    android.util.Log.d("MainChannelScreen", "Loading channels from: $fileName")
                    
                    val loadedChannels = m3uFileManager.loadM3UFile(fileName)
                    if (loadedChannels != null) {
                        channels = loadedChannels
                        android.util.Log.d("MainChannelScreen", "Successfully loaded ${channels.size} channels")
                    } else {
                        android.util.Log.e("MainChannelScreen", "Failed to load channels from file")
                    }
                } else {
                    // Try to find existing M3U files
                    android.util.Log.d("MainChannelScreen", "No imported files, checking existing...")
                    val existingFiles = m3uFileManager.getAllM3UFiles()
                    android.util.Log.d("MainChannelScreen", "Found ${existingFiles.size} existing files")
                    
                    if (existingFiles.isNotEmpty()) {
                        val loadedChannels = m3uFileManager.loadM3UFile(existingFiles.first().fileName)
                        if (loadedChannels != null) {
                            channels = loadedChannels
                            android.util.Log.d("MainChannelScreen", "Loaded ${channels.size} channels from existing file")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainChannelScreen", "Failed to load channels", e)
            }
            
            isLoading = false
            invalidate()
        }
    }
    
    private fun getChannelInfo(channel: Channel): String {
        val category = getChannelCategory(channel)
        return "$category • HD"
    }
    
    private fun getChannelCategory(channel: Channel): String {
        return when {
            channel.name.contains("BBC", ignoreCase = true) -> "News"
            channel.name.contains("CNN", ignoreCase = true) -> "News" 
            channel.name.contains("Fox", ignoreCase = true) -> "News"
            channel.name.contains("CBSN", ignoreCase = true) -> "News"
            channel.name.contains("Al Jazeera", ignoreCase = true) -> "News"
            channel.name.contains("NewsNation", ignoreCase = true) -> "News"
            channel.name.contains("Sports", ignoreCase = true) -> "Sports"
            channel.name.contains("ESPN", ignoreCase = true) -> "Sports"
            channel.name.contains("NBA", ignoreCase = true) -> "Sports"
            channel.name.contains("NFL", ignoreCase = true) -> "Sports"
            channel.name.contains("MLB", ignoreCase = true) -> "Sports"
            channel.name.contains("Sky Sports", ignoreCase = true) -> "Sports"
            channel.name.contains("Fox Sports", ignoreCase = true) -> "Sports"
            channel.name.contains("Movie", ignoreCase = true) -> "Movies"
            channel.name.contains("Cinema", ignoreCase = true) -> "Movies"
            channel.name.contains("HBO", ignoreCase = true) -> "Premium"
            channel.name.contains("Showtime", ignoreCase = true) -> "Premium"
            channel.name.contains("Cinemax", ignoreCase = true) -> "Premium"
            channel.name.contains("Discovery", ignoreCase = true) -> "Documentary"
            channel.name.contains("National Geographic", ignoreCase = true) -> "Documentary"
            channel.name.contains("History", ignoreCase = true) -> "Documentary"
            channel.name.contains("Cartoon", ignoreCase = true) -> "Kids"
            else -> "General"
        }
    }
    
    private fun getChannelThumbnail(channel: Channel): CarIcon {
        val iconResource = when {
            channel.name.contains("BBC", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("CNN", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("Fox", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("News", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("CBSN", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("Al Jazeera", ignoreCase = true) -> R.drawable.ic_news
            channel.name.contains("Sports", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("ESPN", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("NBA", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("NFL", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("MLB", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("Sky Sports", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("Fox Sports", ignoreCase = true) -> R.drawable.ic_sports
            channel.name.contains("Movie", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("Cinema", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("HBO", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("Showtime", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("Cinemax", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("Max", ignoreCase = true) -> R.drawable.ic_movies
            channel.name.contains("Discovery", ignoreCase = true) -> R.drawable.ic_info
            channel.name.contains("National Geographic", ignoreCase = true) -> R.drawable.ic_info
            channel.name.contains("History", ignoreCase = true) -> R.drawable.ic_info
            channel.name.contains("Cartoon", ignoreCase = true) -> R.drawable.ic_category
            else -> R.drawable.ic_tv
        }
        
        return CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconResource)
        ).build()
    }
}