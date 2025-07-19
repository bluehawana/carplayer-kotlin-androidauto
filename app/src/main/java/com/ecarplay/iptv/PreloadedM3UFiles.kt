package com.ecarplay.iptv

import android.content.Context
import com.ecarplay.iptv.storage.M3UFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreloadedM3UFiles(private val context: Context) {
    
    private val m3uFileManager = M3UFileManager(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    fun loadSampleM3UFiles() {
        coroutineScope.launch {
            // Sample M3U content for different categories
            val sampleFiles = mapOf(
                "News Channels" to createNewsChannelsM3U(),
                "Entertainment" to createEntertainmentChannelsM3U(),
                "Sports" to createSportsChannelsM3U(),
                "Movies" to createMovieChannelsM3U(),
                "Music" to createMusicChannelsM3U()
            )
            
            sampleFiles.forEach { (displayName, content) ->
                m3uFileManager.saveM3UFile(content, displayName)
            }
        }
    }
    
    private fun createNewsChannelsM3U(): String {
        return """
            #EXTM3U
            #EXTINF:-1 tvg-id="1" tvg-name="CNN" tvg-logo="https://example.com/cnn.png" group-title="News",CNN
            https://example.com/stream/cnn
            #EXTINF:-1 tvg-id="2" tvg-name="BBC News" tvg-logo="https://example.com/bbc.png" group-title="News",BBC News
            https://example.com/stream/bbc
            #EXTINF:-1 tvg-id="3" tvg-name="Fox News" tvg-logo="https://example.com/fox.png" group-title="News",Fox News
            https://example.com/stream/fox
            #EXTINF:-1 tvg-id="4" tvg-name="Al Jazeera" tvg-logo="https://example.com/aljazeera.png" group-title="News",Al Jazeera
            https://example.com/stream/aljazeera
            #EXTINF:-1 tvg-id="5" tvg-name="RT News" tvg-logo="https://example.com/rt.png" group-title="News",RT News
            https://example.com/stream/rt
        """.trimIndent()
    }
    
    private fun createEntertainmentChannelsM3U(): String {
        return """
            #EXTM3U
            #EXTINF:-1 tvg-id="6" tvg-name="HBO" tvg-logo="https://example.com/hbo.png" group-title="Entertainment",HBO
            https://example.com/stream/hbo
            #EXTINF:-1 tvg-id="7" tvg-name="Netflix Live" tvg-logo="https://example.com/netflix.png" group-title="Entertainment",Netflix Live
            https://example.com/stream/netflix
            #EXTINF:-1 tvg-id="8" tvg-name="Comedy Central" tvg-logo="https://example.com/comedy.png" group-title="Entertainment",Comedy Central
            https://example.com/stream/comedy
            #EXTINF:-1 tvg-id="9" tvg-name="Discovery" tvg-logo="https://example.com/discovery.png" group-title="Entertainment",Discovery
            https://example.com/stream/discovery
            #EXTINF:-1 tvg-id="10" tvg-name="National Geographic" tvg-logo="https://example.com/natgeo.png" group-title="Entertainment",National Geographic
            https://example.com/stream/natgeo
        """.trimIndent()
    }
    
    private fun createSportsChannelsM3U(): String {
        return """
            #EXTM3U
            #EXTINF:-1 tvg-id="11" tvg-name="ESPN" tvg-logo="https://example.com/espn.png" group-title="Sports",ESPN
            https://example.com/stream/espn
            #EXTINF:-1 tvg-id="12" tvg-name="Fox Sports" tvg-logo="https://example.com/foxsports.png" group-title="Sports",Fox Sports
            https://example.com/stream/foxsports
            #EXTINF:-1 tvg-id="13" tvg-name="Sky Sports" tvg-logo="https://example.com/skysports.png" group-title="Sports",Sky Sports
            https://example.com/stream/skysports
            #EXTINF:-1 tvg-id="14" tvg-name="NBC Sports" tvg-logo="https://example.com/nbcsports.png" group-title="Sports",NBC Sports
            https://example.com/stream/nbcsports
            #EXTINF:-1 tvg-id="15" tvg-name="Eurosport" tvg-logo="https://example.com/eurosport.png" group-title="Sports",Eurosport
            https://example.com/stream/eurosport
        """.trimIndent()
    }
    
    private fun createMovieChannelsM3U(): String {
        return """
            #EXTM3U
            #EXTINF:-1 tvg-id="16" tvg-name="Movie Channel 1" tvg-logo="https://example.com/movie1.png" group-title="Movies",Movie Channel 1
            https://example.com/stream/movie1
            #EXTINF:-1 tvg-id="17" tvg-name="Hollywood Movies" tvg-logo="https://example.com/hollywood.png" group-title="Movies",Hollywood Movies
            https://example.com/stream/hollywood
            #EXTINF:-1 tvg-id="18" tvg-name="Classic Movies" tvg-logo="https://example.com/classic.png" group-title="Movies",Classic Movies
            https://example.com/stream/classic
            #EXTINF:-1 tvg-id="19" tvg-name="Action Movies" tvg-logo="https://example.com/action.png" group-title="Movies",Action Movies
            https://example.com/stream/action
            #EXTINF:-1 tvg-id="20" tvg-name="Horror Movies" tvg-logo="https://example.com/horror.png" group-title="Movies",Horror Movies
            https://example.com/stream/horror
        """.trimIndent()
    }
    
    private fun createMusicChannelsM3U(): String {
        return """
            #EXTM3U
            #EXTINF:-1 tvg-id="21" tvg-name="MTV" tvg-logo="https://example.com/mtv.png" group-title="Music",MTV
            https://example.com/stream/mtv
            #EXTINF:-1 tvg-id="22" tvg-name="VH1" tvg-logo="https://example.com/vh1.png" group-title="Music",VH1
            https://example.com/stream/vh1
            #EXTINF:-1 tvg-id="23" tvg-name="Music Box" tvg-logo="https://example.com/musicbox.png" group-title="Music",Music Box
            https://example.com/stream/musicbox
            #EXTINF:-1 tvg-id="24" tvg-name="Kiss FM" tvg-logo="https://example.com/kiss.png" group-title="Music",Kiss FM
            https://example.com/stream/kiss
            #EXTINF:-1 tvg-id="25" tvg-name="Radio 1" tvg-logo="https://example.com/radio1.png" group-title="Music",Radio 1
            https://example.com/stream/radio1
        """.trimIndent()
    }
}