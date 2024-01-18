package com.sinchandroidmodule.callbacks

interface PushTokenUnregisterCallback {
    fun onPushTokenUnregistered()
    fun onPushTokenUnRegistrationFailed(error: String?)
}