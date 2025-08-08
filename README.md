I # CarPlayer IPTV

An Android Auto IPTV application that allows users to watch television channels in their car with optimized controls for driving safety.

## Features

- **Car-Optimized UI**: Designed specifically for Android Auto with large, easy-to-read interfaces
- **Channel Management**: Import M3U playlists and organize channels by categories
- **Safe Controls**: Volume control, play/pause optimized for use while driving
- **Subscription Import**: Support for importing IPTV subscriptions from URLs or files
- **Data Efficient**: Optimized for users with limited data plans (7GB-11GB monthly)

### 🚀 **Smart Network Optimization** (New!)
- **Intelligent Hotspot Detection**: Automatically detects when using cellular hotspot (Comviq/Tele2 5G)
- **Adaptive Streaming**: Adjusts quality and buffering based on connection type
- **Channel Preloading**: Pre-tests next channels on cellular for instant switching
- **Hybrid Media Engine**: ExoPlayer + VLC fallback with network-aware selection
- **Connection Monitoring**: Real-time network quality assessment and optimization

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 23 or higher
- Android Auto compatible device
- Valid IPTV subscription (M3U playlist format)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/carplayer-kotlin-androidauto.git
   cd carplayer-kotlin-androidauto
   ```

2. Open the project in Android Studio

3. Build and install the app:
   ```bash
   ./gradlew installDebug
   ```

### Android Auto Setup

1. Enable Developer Options on your Android device
2. Enable "Unknown sources" in Android Auto settings
3. Connect your device to your car's Android Auto system
4. Launch the CarPlayer IPTV app

## API Integration

### Channel API Integration

To integrate with your channel API, modify the `ChannelManager.kt` file:

```kotlin
suspend fun fetchChannelsFromAPI(apiUrl: String): List<Channel> {
    return withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.channelService.getChannels(apiUrl)
            response.channels.map { apiChannel ->
                Channel(
                    id = apiChannel.id,
                    name = apiChannel.name,
                    description = apiChannel.description,
                    streamUrl = apiChannel.streamUrl,
                    logoUrl = apiChannel.logoUrl,
                    category = apiChannel.category,
                    isHD = apiChannel.isHD
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

### Supported Formats

- **M3U/M3U8**: Standard IPTV playlist format
- **JSON**: Custom channel API format
- **XML**: XMLTV format for EPG data

### Example API Response

```json
{
  "channels": [
    {
      "id": "1",
      "name": "Channel Name",
      "description": "Channel description",
      "streamUrl": "https://stream.example.com/channel1",
      "logoUrl": "https://logo.example.com/channel1.png",
      "category": "Entertainment",
      "isHD": true
    }
  ]
}
```

## Usage

1. **Import Subscription**: 
   - Use the phone app to import M3U playlists
   - Or import directly in car via URL

2. **Browse Channels**:
   - Navigate through channel list using car controls
   - Select channels by category

3. **Playback Control**:
   - Play/pause with steering wheel controls
   - Volume control optimized for car audio
   - Safe UI that doesn't distract from driving

## 📱 Network Optimization

### Automatic Network Detection

The app intelligently detects and optimizes for different network types:

- **📶 WiFi**: Standard quality, minimal buffering
- **📱 Cellular Hotspot**: Extended buffering, channel preloading
- **🚀 5G (Comviq/Tele2)**: High-quality streaming with adaptive bitrate
- **🐌 Poor Connection**: Conservative settings, VLC fallback engine

### Smart Features for Cellular Users

- **Hotspot Detection**: Automatically detects when using phone hotspot
- **Channel Preloading**: Tests next 3 channels in background for instant switching
- **Connection Quality Indicators**:
  - ✅ **Fast connection (< 1s)**: Channel ready for instant playback
  - 🟡 **Good connection (1-3s)**: Channel loads smoothly
  - 🟠 **Slow connection (3-5s)**: May have buffering
  - ❌ **Poor connection**: Skip this channel

### Network Status Messages

The app provides real-time feedback about your connection:

- `📱 Hotspot detected - optimizing for cellular`
- `🚀 5G connection - high quality available`
- `📶 Using WiFi connection`
- `🐌 Low bandwidth - using VLC engine`

### Optimized for Swedish Networks

Specifically tested and optimized for:
- **Comviq (Tele2)**: 5G and 4G hotspot streaming
- **Poor office WiFi scenarios**
- **Mixed connection environments**

## Architecture

- **CarAppService**: Main Android Auto service
- **ChannelManager**: Handles channel data and subscriptions
- **HybridMediaController**: Hybrid ExoPlayer + VLC media engine with intelligent switching
- **NetworkBalancer**: Network detection and optimization system
- **NetworkMonitor**: Real-time connection monitoring and adaptation
- **ChannelPreloader**: Background channel testing for smooth switching
- **Screen Classes**: Car-optimized UI screens

## Data Usage Optimization

### Smart Streaming Profiles

The app automatically selects the best streaming profile based on your connection:

- **📶 WiFi Standard**: Full quality, 3-second buffer
- **📱 Cellular/Hotspot Optimized**: Extended 8-second buffer, longer timeouts, channel preloading
- **🐌 Low Bandwidth Mode**: Conservative settings, VLC engine for better compatibility

### Advanced Features

- **Adaptive Bitrate Streaming**: Automatically adjusts quality based on connection speed
- **Network-Aware Buffering**: Longer buffers on cellular connections reduce interruptions
- **Intelligent Engine Selection**: Uses ExoPlayer for 5G, VLC for poor connections
- **Background Channel Testing**: Preloads next channels to reduce switching delays
- **Data-Efficient Headers**: Optimized HTTP headers for cellular compatibility
- **Connection Quality Monitoring**: Real-time assessment prevents failed streams

## Safety Features

- Voice control support
- Simplified UI for car use
- Automatic pause when parking
- Integration with car audio system

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please create an issue in the GitHub repository.