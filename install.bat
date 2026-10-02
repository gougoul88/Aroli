@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Aroli Installation Script
echo ========================================
echo.

REM Check if ADB is available
adb version >nul 2>&1
if errorlevel 1 (
    echo ERROR: ADB is not installed or not in PATH
    echo.
    echo Please install ADB by:
    echo 1. Installing Android SDK Platform Tools from:
    echo    https://developer.android.com/tools/releases/platform-tools
    echo 2. Add it to your PATH environment variable
    echo.
    pause
    exit /b 1
)

echo Step 1: Checking for connected devices...
adb devices
echo.

REM Find APK file
set "APK_FILE="
if exist "aroli-release.apk" (
    set "APK_FILE=aroli-release.apk"
) else if exist "app-release.apk" (
    set "APK_FILE=app-release.apk"
) else if exist "aroli-debug.apk" (
    set "APK_FILE=aroli-debug.apk"
) else if exist "app-debug.apk" (
    set "APK_FILE=app-debug.apk"
) else (
    echo ERROR: No APK file found in current directory!
    echo.
    echo Please download the APK from GitHub Releases and place it in this folder.
    echo.
    pause
    exit /b 1
)

echo Step 2: Installing APK...
echo APK File: !APK_FILE!
echo.

adb install -r "!APK_FILE!"

if errorlevel 1 (
    echo.
    echo ERROR: Installation failed!
    echo.
    echo Possible solutions:
    echo 1. Check device: adb devices
    echo 2. Enable USB Debugging in Settings on your device
    echo 3. Accept any ADB authorization prompts
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! Aroli installed successfully!
echo ========================================
echo.
echo Next steps:
echo 1. For Kiosk Mode (requires factory reset):
echo    - Factory reset your device
echo    - Skip Google Account during setup
echo    - Run: adb shell dpm set-device-owner com.aroli.storybox/.AdminReceiver
echo.
echo 2. Launch the app:
echo    - Tap the Aroli icon on your device
echo    - Or run: adb shell am start -n com.aroli.storybox/.MainActivity
echo.
echo 3. Access Parent Settings:
echo    - Tap the gear icon at top-right
echo    - Enter PIN: 000000
echo.
pause
