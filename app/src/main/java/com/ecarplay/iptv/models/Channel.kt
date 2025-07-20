package com.ecarplay.iptv.models

data class Channel(
    val id: String,
    val name: String,
    val description: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val isHD: Boolean = false
)

data class ChannelGroup(
    val name: String,
    val channels: List<Channel>
)

data class IPTVSubscription(
    val name: String,
    val url: String,
    val channels: List<Channel> = emptyList(),
    val groups: List<ChannelGroup> = emptyList()
)