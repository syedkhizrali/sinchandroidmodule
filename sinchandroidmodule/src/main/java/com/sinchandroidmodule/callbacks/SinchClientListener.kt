package com.sinchandroidmodule.callbacks

interface SinchClientListener {
    fun onLogMessage(level: Int, area: String, message: String)
    fun onPushTokenRegistered()
    fun onPushTokenUnregistered()
    fun onUserRegistered()
    fun onPushTokenRegistrationFailed(error: String)
    fun onPushTokenUnRegistrationFailed(error: String)
    fun onUserRegistrationFailed(error: String)
    fun onClientFailed(error: String)
    fun onClientStarted()
}