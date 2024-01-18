package com.sinchandroidmodule.callbacks

interface PushTokenRegisterCallback {
    fun onPushTokenRegistered()
    fun onPushTokenRegistrationFailed(error: String?)
}