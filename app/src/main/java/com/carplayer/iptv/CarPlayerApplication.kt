package com.carplayer.iptv

import android.app.Application
import com.carplayer.iptv.models.Channel

class CarPlayerApplication : Application() {
    val channels = mutableListOf<Channel>()
}
