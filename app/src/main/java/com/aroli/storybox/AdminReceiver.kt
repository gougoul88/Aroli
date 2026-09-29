package com.aroli.storybox

import android.app.admin.DeviceAdminReceiver

/** Required Device Policy Controller receiver to enroll Aroli as Device Owner (full kiosk lockdown, no unpin gesture). */
class AdminReceiver : DeviceAdminReceiver()
