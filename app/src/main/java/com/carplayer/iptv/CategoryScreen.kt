package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.carplayer.iptv.models.Channel

class CategoryScreen(
    carContext: CarContext,
    private val channels: List<Channel>
) : Screen(carContext) {
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        // Group channels by category
        val categorizedChannels = channels.groupBy { getChannelCategory(it) }
        
        categorizedChannels.forEach { (category, channelList) ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(category)
                    .addText("${channelList.size} channels")
                    .setOnClickListener {
                        screenManager.push(ChannelListScreen(carContext, channelList, category))
                    }
                    .setImage(getCategoryIcon(category))
                    .build()
            )
        }
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle("Channel Categories")
            .setHeaderAction(Action.BACK)
            .build()
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
            channel.name.contains("NBC Sports", ignoreCase = true) -> "Sports"
            channel.name.contains("Movie", ignoreCase = true) -> "Movies"
            channel.name.contains("Cinema", ignoreCase = true) -> "Movies"
            channel.name.contains("HBO", ignoreCase = true) -> "Premium"
            channel.name.contains("Showtime", ignoreCase = true) -> "Premium"
            channel.name.contains("Cinemax", ignoreCase = true) -> "Premium"
            channel.name.contains("Max", ignoreCase = true) -> "Premium"
            channel.name.contains("Paramount", ignoreCase = true) -> "Premium"
            channel.name.contains("Discovery", ignoreCase = true) -> "Documentary"
            channel.name.contains("National Geographic", ignoreCase = true) -> "Documentary"
            channel.name.contains("History", ignoreCase = true) -> "Documentary"
            channel.name.contains("Science", ignoreCase = true) -> "Documentary"
            channel.name.contains("Animal Planet", ignoreCase = true) -> "Documentary"
            channel.name.contains("Cartoon", ignoreCase = true) -> "Kids"
            channel.name.contains("Nick", ignoreCase = true) -> "Kids"
            channel.name.contains("Disney", ignoreCase = true) -> "Kids"
            channel.name.contains("Food", ignoreCase = true) -> "Lifestyle"
            channel.name.contains("HGTV", ignoreCase = true) -> "Lifestyle"
            channel.name.contains("Travel", ignoreCase = true) -> "Lifestyle"
            channel.name.contains("Music", ignoreCase = true) -> "Music"
            channel.name.contains("MTV", ignoreCase = true) -> "Music"
            else -> "General"
        }
    }
    
    private fun getCategoryIcon(category: String): CarIcon {
        val iconResource = when (category) {
            "News" -> R.drawable.ic_news
            "Sports" -> R.drawable.ic_sports
            "Movies" -> R.drawable.ic_movies
            "Premium" -> R.drawable.ic_movies
            "Documentary" -> R.drawable.ic_info
            "Kids" -> R.drawable.ic_category
            "Lifestyle" -> R.drawable.ic_category
            "Music" -> R.drawable.ic_volume
            else -> R.drawable.ic_tv
        }
        
        return CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconResource)
        ).build()
    }
}