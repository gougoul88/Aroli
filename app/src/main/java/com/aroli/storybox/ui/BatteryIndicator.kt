package com.aroli.storybox.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kid-friendly battery indicator with simple text display.
 * Shows battery percentage in large, easy-to-read text.
 * Gray color scheme for consistency with app theme.
 */
@Composable
fun BatteryIndicator() {
    val context = LocalContext.current
    val batteryLevel = remember { mutableIntStateOf(0) }

    // Update battery level on composition
    DisposableEffect(context) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryLevel.intValue = if (scale > 0) (level * 100 / scale) else 0
        }

        onDispose { }
    }

    val percentage = batteryLevel.intValue

    Box(
        modifier = Modifier
            .background(
                color = Color(0x1A808080),  // Transparent gray (10% opacity)
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp),
        ) {
            // Battery icon (simple visual representation)
            Box(
                modifier = Modifier
                    .size(24.dp, 14.dp)
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Battery outline in gray
                Box(
                    modifier = Modifier
                        .size(22.dp, 12.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(2.dp)
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Battery fill (proportional to percentage)
                    Box(
                        modifier = Modifier
                            .size((22 * percentage / 100).dp, 12.dp)
                            .background(
                                color = when {
                                    percentage > 50 -> Color(0xFF808080)  // Full gray
                                    percentage > 20 -> Color(0xFF808080)  // Full gray
                                    else -> Color(0xFF808080)  // Full gray (kids don't need warning colors)
                                },
                                shape = RoundedCornerShape(1.dp)
                            ),
                    )
                }
            }

            // Battery percentage text
            Text(
                text = "$percentage%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
