package com.ecarplay.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.ecarplay.iptv.models.Channel

class GridChannelScreen(
    carContext: CarContext,
    private val channels: List<Channel>,
    private val sourceTitle: String = "IPTV Channels"
) : Screen(carContext) {
    
    override fun onGetTemplate(): Template {
        val gridItemListBuilder = ItemList.Builder()
        
        // Take first 12 channels for the grid (3x4 layout is typical for car displays)
        val displayChannels = channels.take(12)
        
        displayChannels.forEach { channel ->
            val gridItem = GridItem.Builder()
                .setTitle(channel.name)
                .setText(getChannelCategory(channel))
                .setImage(getChannelIcon(channel))
                .setOnClickListener {
                    screenManager.push(PlayerScreen(carContext, channel))
                }
                .build()
            
            gridItemListBuilder.addItem(gridItem)
        }
        
        return GridTemplate.Builder()
            .setSingleList(gridItemListBuilder.build())
            .setTitle(sourceTitle)
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("All Channels")
                            .setOnClickListener {
                                screenManager.push(ChannelListScreen(carContext, channels, sourceTitle))
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
    
    private fun getChannelCategory(channel: Channel): String {
        return when {
            channel.name.contains("BBC", ignoreCase = true) -> "News"
            channel.name.contains("CNN", ignoreCase = true) -> "News" 
            channel.name.contains("Fox", ignoreCase = true) -> "News"
            channel.name.contains("Sports", ignoreCase = true) -> "Sports"
            channel.name.contains("Movie", ignoreCase = true) -> "Movies"
            channel.name.contains("HBO", ignoreCase = true) -> "Premium"
            channel.name.contains("ESPN", ignoreCase = true) -> "Sports"
            channel.name.contains("Discovery", ignoreCase = true) -> "Documentary"
            channel.name.contains("National Geographic", ignoreCase = true) -> "Documentary"
            channel.name.contains("Cartoon", ignoreCase = true) -> "Kids"
            else -> "General"
        }
    }
    
    private fun getChannelIcon(channel: Channel): CarIcon {
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
        
        // Create Nordic-themed icon with tint
        return CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconResource)
        ).setTint(androidx.car.app.model.CarColor.createCustom(0xFF0c2d48.toInt(), 0xFF145da0.toInt()))
        .build()
    }
}