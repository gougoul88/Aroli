@echo off
REM Aroli Story Manager - Start Script
REM This script starts the server and opens the story manager in the browser

echo.
echo ========================================
echo  🚀 Aroli Story Manager
echo ========================================
echo.

REM Check if Node.js is installed
where /q node
if errorlevel 1 (
    echo ❌ Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org
    pause
    exit /b 1
)

REM Navigate to the content directory
cd /d "%~dp0"

REM Check if node_modules exists, if not install dependencies
if not exist "node_modules\" (
    echo 📦 Installing dependencies...
    call npm install express multer
    if errorlevel 1 (
        echo ❌ Failed to install dependencies
        pause
        exit /b 1
    )
)

REM Start the server in the background
echo 🔄 Starting server on http://localhost:3000...
start "" "C:\Program Files\nodejs\node.exe" server.js

REM Wait for server to start
timeout /t 2 /nobreak

REM Open the browser
echo 🌐 Opening Story Manager in browser...
start "" http://localhost:3000/story-manager-server.html

echo.
echo ✅ Story Manager is ready!
echo 📖 Press Ctrl+C in the server window to stop the server
echo.

REM Keep the window open
pause
