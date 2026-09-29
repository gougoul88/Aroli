#!/bin/bash
# Installation script for Aroli - Kids' Kiosk Story App
# This script automates the ADB installation process

echo "========================================"
echo "Aroli Installation Script"
echo "========================================"
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "ERROR: ADB (Android Debug Bridge) is not installed or not in PATH"
    echo ""
    echo "Please install ADB by:"
    echo "1. Installing Android SDK Platform Tools from:"
    echo "   https://developer.android.com/tools/releases/platform-tools"
    echo "2. Add it to your PATH environment variable"
    echo ""
    exit 1
fi

echo "Step 1: Checking for connected devices..."
adb devices
echo ""

# Find APK file
APK_FILE=""
if [ -f "aroli-release.apk" ]; then
    APK_FILE="aroli-release.apk"
elif [ -f "app-release.apk" ]; then
    APK_FILE="app-release.apk"
elif [ -f "aroli-debug.apk" ]; then
    APK_FILE="aroli-debug.apk"
elif [ -f "app-debug.apk" ]; then
    APK_FILE="app-debug.apk"
else
    echo "ERROR: No APK file found in current directory!"
    echo ""
    echo "Please download the APK from GitHub Releases and place it in this folder."
    echo ""
    exit 1
fi

echo "Step 2: Installing APK..."
echo "APK File: $APK_FILE"
echo ""

adb install -r "$APK_FILE"

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Installation failed!"
    echo ""
    echo "Possible solutions:"
    echo "1. Check that device is connected: 'adb devices'"
    echo "2. Enable USB Debugging on your device:"
    echo "   Settings > Developer Options > USB Debugging"
    echo "3. Accept any ADB authorization prompts on your device"
    echo ""
    exit 1
fi

echo ""
echo "========================================"
echo "SUCCESS! Aroli installed successfully!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. For Kiosk Mode (optional, requires factory reset):"
echo "   - Factory reset your device"
echo "   - Skip Google Account during setup"
echo "   - Run: adb shell dpm set-device-owner com.aroli.storybox/.AdminReceiver"
echo ""
echo "2. Launch the app:"
echo "   - Tap the Aroli icon on your device"
echo "   - Or run: adb shell am start -n com.aroli.storybox/.MainActivity"
echo ""
echo "3. Access Parent Settings:"
echo "   - Tap the gear icon (top-right)"
echo "   - Enter PIN: 568749"
echo ""
