package com.sinchandroidmodule.callbacks

interface UserRegisterCallbacks {
    fun onUserRegistered()
    fun onUserRegistrationFailed(error: String?)
}