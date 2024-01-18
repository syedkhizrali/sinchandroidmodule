package com.sinchapp.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sinchandroidmodule.SinchApp

class FCMService : FirebaseMessagingService() {
    private var sinchApp: SinchApp? = null
    override fun onMessageReceived(data: RemoteMessage) {
        Log.d("onMessageReceived", ": $data")
        if (SinchApp.isSinchPayload(data.data)) {
            if (sinchApp == null) {
                sinchApp = SinchApp(this)
            }
            sinchApp?.handelCall(data.data)
        }
    }

    override fun onNewToken(p0: String) {

    }
}