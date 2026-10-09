package com.aroli.storybox.util

import android.content.Context
import android.content.pm.PackageManager

object VersionInfo {
    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }

    val RELEASE_NOTES = """
        Version 1.1 (2026-10-09)
        
        ✨ New Features:
        • Maximum volume limiter - protect children's hearing with a volume cap
        • Logarithmic volume scaling - small slider changes produce more perceptible differences
        • Improved installation scripts (install.bat & install.sh)
        • Interactive setup wizard for kiosk mode enrollment
        • Auto-launch app after installation
        
        🔧 Improvements:
        • Volume control now uses quadratic progression for better sensitivity
        • Installation scripts now prompt for kiosk mode setup
        • Parent Settings includes new "Maximum Volume" slider (10-100%)
        • Release assets on GitHub include both APK and install scripts
        
        📦 Technical:
        • Player volume now capped at app level (independent of device volume)
        • AppSettings.kt: Added maxVolumePercent preference
        • PlayerViewModel: Logarithmic volume formula (percent/100)²
        
        ---
        
        Version 1.0 (2026-09-29)
        
        ✨ Features:
        • Kiosk mode with Device Owner support (no unpin gesture)
        • PIN-protected parent menu (default code: 000000, backup code: 568749)
        • AI story filter with age-based recommendations
        • Web mode (GitHub) and Local folder content support
        • Tap-to-play story interface
        • Dark theme for night-time viewing
        
        🔧 Settings:
        • Change parent PIN code
        • Enable/disable AI-generated stories
        • Set child's age for content filtering
        • Clear cache
        
        🎨 Interface:
        • Full-screen kiosk interface
        • No system navigation available
        • Large touch targets for kids
    """.trimIndent()
}

