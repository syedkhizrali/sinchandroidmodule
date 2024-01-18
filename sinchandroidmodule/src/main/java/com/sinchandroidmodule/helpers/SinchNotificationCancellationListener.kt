package com.sinchandroidmodule.helpers

import android.app.NotificationManager
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener

internal class SinchNotificationCancellationListener(private val notificationManager: NotificationManager) :
    CallListener {


    override fun onCallEnded(call: Call) {
        notificationManager.cancelAll()
        SinchNotificationUtils.stopRingTone()

    }

    override fun onCallEstablished(call: Call) {
        notificationManager.cancelAll()
        SinchNotificationUtils.stopRingTone()

    }

    override fun onCallProgressing(call: Call) {
    }

}