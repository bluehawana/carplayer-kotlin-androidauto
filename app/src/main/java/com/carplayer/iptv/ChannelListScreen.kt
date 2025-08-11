package com.carplayer.iptv

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.carplayer.iptv.models.Channel

class ChannelListScreen(
    carContext: CarContext,
    private val channels: List<Channel>? = null,
    private val sourceTitle: String? = null
) : Screen(carContext) {
    
    private val channelManager = ChannelManager()
    
    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()
        
        // Use provided channels or get from channel manager
        val channelList = channels ?: channelManager.getChannels()
        
        // Add channels from subscription
        channelList.forEach { channel ->
            val displayTitle = if (channel.channelNumber != null) {
                "${channel.channelNumber} - ${channel.name}"
            } else {
                channel.name
            }
            
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(displayTitle)
                    .addText(channel.description)
                    .setOnClickListener {
                        screenManager.push(PlayerScreen(carContext, channel, channelList))
                    }
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_tv)
                        ).build()
                    )
                    .build()
            )
        }
        
        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle(sourceTitle ?: "IPTV Channels")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("M3U Files")
                            .setOnClickListener {
                                screenManager.push(M3UFileListScreen(carContext))
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Import New")
                            .setOnClickListener {
                                screenManager.push(ImportScreen(carContext))
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}