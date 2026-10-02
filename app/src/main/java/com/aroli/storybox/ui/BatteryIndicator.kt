package com.aroli.storybox.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aroli.storybox.R

/**
 * Kid-friendly battery indicator using image assets.
 * Displays different seed/graine images based on battery level.
 * Optionally shows battery percentage text.
 * Blinks when battery is below 10%.
 * 
 * Battery levels:
 * - < 10%: aroli_graine_10 (blinking)
 * - < 25%: aroli_graine_25
 * - < 50%: aroli_graine_50
 * - < 75%: aroli_graine_75
 * - < 100%: aroli_graine_100
 * - Charging: aroli_graine_charge
 */
@Composable
fun BatteryIndicator(showPercentage: Boolean = true) {
    val context = LocalContext.current
    val batteryLevel = remember { mutableIntStateOf(0) }
    val isCharging = remember { mutableStateOf(false) }

    // Listen for battery changes using BroadcastReceiver
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    
                    batteryLevel.intValue = if (scale > 0) (level * 100 / scale) else 0
                    isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                       status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(batteryReceiver, filter)
        }

        onDispose { 
            context.unregisterReceiver(batteryReceiver)
        }
    }

    val percentage = batteryLevel.intValue

    // Get the appropriate drawable resource based on battery level
    val drawableResId = when {
        isCharging.value -> R.drawable.aroli_graine_charge_80x80
        percentage < 10 -> R.drawable.aroli_graine_10_80x80
        percentage < 25 -> R.drawable.aroli_graine_25_80x80
        percentage < 50 -> R.drawable.aroli_graine_50_80x80
        percentage < 75 -> R.drawable.aroli_graine_75_80x80
        else -> R.drawable.aroli_graine_100_80x80
    }

    // Blinking animation for low battery (< 10%)
    val infiniteTransition = rememberInfiniteTransition(label = "BatteryBlink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "BatteryAlpha"
    )

    Box(
        modifier = Modifier
            .background(
                color = Color(0x1A000000),  // Transparent black (10% opacity)
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(2.dp),
        ) {
            // Battery image icon (with blinking effect if < 10%)
            Image(
                painter = painterResource(id = drawableResId),
                contentDescription = "Battery level",
                modifier = Modifier
                    .size(40.dp)
                    .let { if (percentage < 10 && !isCharging.value) it else it },
                alpha = if (percentage < 10 && !isCharging.value) alpha else 1f,
            )

            // Battery percentage text (optional)
            if (showPercentage) {
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
}
