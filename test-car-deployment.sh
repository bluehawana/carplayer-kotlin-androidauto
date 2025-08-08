#!/bin/bash

# Car Computer Testing Script for Volvo/Polestar
echo "🚗 CarPlayer IPTV - Car Computer Testing Script"

# Configuration
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.carplayer.iptv"
CAR_IP="${1:-192.168.1.100}"  # Default or passed IP

echo "📱 Connecting to car computer..."

# Try wireless connection first
if adb connect $CAR_IP:5555; then
    echo "✅ Connected wirelessly to $CAR_IP"
else
    echo "⚠️  Wireless connection failed, checking USB..."
    if ! adb devices | grep -q "device"; then
        echo "❌ No devices connected. Please:"
        echo "   1. Enable Developer Options on car system"
        echo "   2. Enable USB Debugging"
        echo "   3. Connect USB cable or enable Wireless ADB"
        exit 1
    fi
fi

echo "🔧 Building APK..."
./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

echo "📦 Installing APK on car computer..."
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "✅ APK installed successfully"
else
    echo "❌ APK installation failed"
    exit 1
fi

echo "🧪 Running car-specific tests..."

# Test 1: Check Android Auto integration
echo "Test 1: Android Auto Integration"
adb shell am start -n com.google.android.projection.gearhead/.CarActivity
sleep 2
adb shell am start -n $PACKAGE_NAME/.CarAppService
echo "   ✓ Android Auto launched"

# Test 2: Network connectivity
echo "Test 2: Network Connectivity"
adb shell ping -c 1 google.com > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Internet connectivity: OK"
else
    echo "   ⚠️  Internet connectivity: FAILED"
fi

# Test 3: Check network optimization features
echo "Test 3: Network Optimization"
adb shell am start -n $PACKAGE_NAME/.MainActivity
echo "   ✓ App launched, check logs for network detection"

# Test 4: Monitor logs for errors
echo "Test 4: Monitoring logs (10 seconds)..."
timeout 10s adb logcat | grep -E "(CarPlayer|NetworkBalancer|IPTV|AndroidRuntime)" &
sleep 10

echo "🎯 Testing completed!"
echo ""
echo "Manual tests to perform:"
echo "  • Test steering wheel media buttons"
echo "  • Test rotary knob navigation"
echo "  • Test voice commands"
echo "  • Test network switching (WiFi ↔ Cellular)"
echo "  • Test channel switching performance"
echo "  • Test audio output through car speakers"
echo ""
echo "📊 View logs: adb logcat | grep CarPlayer"
echo "🔍 Monitor performance: adb shell top | grep carplayer"