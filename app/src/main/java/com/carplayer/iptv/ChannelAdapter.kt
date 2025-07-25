package com.carplayer.iptv

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.carplayer.iptv.models.Channel

class ChannelAdapter(private val channels: List<Channel>) :
    RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_2, parent, false
        )
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel)
    }

    override fun getItemCount() = channels.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(android.R.id.text1)
        private val subtitleView: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(channel: Channel) {
            titleView.text = channel.name
            subtitleView.text = "${channel.category} • HD"

            // Nordic Ice Age Theme Colors
            val backgroundColor = when (channel.category) {
                "News" -> 0xFF145da0.toInt()        // Midnight Blue
                "Sports" -> 0xFF2e8bc0.toInt()      // Blue
                "Movies" -> 0xFF0c2d48.toInt()      // Dark Blue
                "Documentary" -> 0xFF145da0.toInt() // Midnight Blue
                "General" -> 0xFFb1d4e0.toInt()    // Baby Blue
                else -> 0xFF2e8bc0.toInt()         // Default Blue
            }
            itemView.setBackgroundColor(backgroundColor)

            // Set text color for Nordic feel - white for dark backgrounds, dark for light
            val textColor = when (channel.category) {
                "General" -> 0xFF0c2d48.toInt()    // Dark Blue text on Baby Blue background
                else -> 0xFFFFFFFF.toInt()         // White text on dark backgrounds
            }
            titleView.setTextColor(textColor)
            subtitleView.setTextColor(textColor)

            // Add padding for better appearance
            itemView.setPadding(16, 16, 16, 16)

            // Add click listener to test playback
            itemView.setOnClickListener {
                android.util.Log.d("CarTestActivity", "Clicked channel: ${channel.name}")
                android.util.Log.d("CarTestActivity", "Stream URL: ${channel.streamUrl}")

                // Launch our custom Nordic video player with channel navigation
                try {
                    val allChannels = (itemView.context.applicationContext as CarPlayerApplication).channels
                    val currentIndex = allChannels.indexOf(channel)
                    val channelNames = allChannels.map { it.name }
                    val channelUrls = allChannels.map { it.streamUrl }

                    val intent = Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                        putExtra("CHANNEL_NAME", channel.name)
                        putExtra("STREAM_URL", channel.streamUrl)
                        putStringArrayListExtra("ALL_CHANNELS", ArrayList(channelNames))
                        putStringArrayListExtra("ALL_CHANNEL_URLS", ArrayList(channelUrls))
                        putExtra("CURRENT_INDEX", currentIndex)
                    }
                    itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("CarTestActivity", "Failed to launch player", e)
                    Toast.makeText(
                        itemView.context,
                        "Selected: ${channel.name}\nTap to play stream",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}