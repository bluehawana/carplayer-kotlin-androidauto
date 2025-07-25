# CarPlayer IPTV

An Android Auto IPTV application that allows users to watch television channels in their car with optimized controls for driving safety.

## Features

- **Car-Optimized UI**: Designed specifically for Android Auto with large, easy-to-read interfaces
- **Channel Management**: Import M3U playlists and organize channels by categories
- **Safe Controls**: Volume control, play/pause optimized for use while driving
- **Subscription Import**: Support for importing IPTV subscriptions from URLs or files
- **Data Efficient**: Optimized for users with limited data plans (7GB-11GB monthly)

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

## Architecture

- **CarAppService**: Main Android Auto service
- **ChannelManager**: Handles channel data and subscriptions
- **VlcMediaController**: Controls video playback using VLC
- **Screen Classes**: Car-optimized UI screens

## Data Usage Optimization

- Adaptive bitrate streaming
- Configurable quality settings
- Data usage monitoring
- Offline channel list caching

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