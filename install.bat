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

REM Ask if user wants to setup Kiosk Mode
echo.
echo Do you want to setup Kiosk Mode now?
echo (Note: Device should be factory reset and no Google Account added)
echo.
choice /C YN /M "Setup Kiosk Mode? (Y/N): "
if errorlevel 2 goto skip_kiosk
if errorlevel 1 (
    echo.
    echo Setting up Kiosk Mode...
    adb shell dpm set-device-owner com.aroli.storybox/.AdminReceiver
    if errorlevel 1 (
        echo.
        echo WARNING: Kiosk Mode setup failed.
        echo Make sure device is factory reset and no Google Account is added.
    ) else (
        echo.
        echo SUCCESS! Kiosk Mode enabled.
    )
)

:skip_kiosk
echo.
echo Do you want to launch Aroli now?
choice /C YN /M "Start Aroli? (Y/N): "
if errorlevel 2 goto skip_launch
if errorlevel 1 (
    echo.
    echo Launching Aroli...
    adb shell am start -n com.aroli.storybox/.MainActivity
    echo.
    echo Aroli is now running!
)

:skip_launch
echo.
echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo To access Parent Settings:
echo - Tap the gear icon (top-right)
echo - Enter PIN: 000000 (default)
echo.
pause
