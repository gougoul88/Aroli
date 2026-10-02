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

