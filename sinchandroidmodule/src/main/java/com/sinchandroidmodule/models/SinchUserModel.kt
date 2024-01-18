package com.sinchandroidmodule.models

import java.io.Serializable

data class SinchUserModel(
    val secret: String,
    val key: String,
    val environment: String,
    val fcmSenderID: String,
    val fcmToken: String,
    val userName: String,
    val userID: String,
    val acceptButton: String = "Accept",
    val rejectButton: String="Reject",
    var audioNotificationLogo: Int = androidx.core.R.drawable.ic_call_answer,
    var videoNotificationLogo: Int = androidx.core.R.drawable.ic_call_answer_video_low,
    val enableVideoCalls: Boolean = true
) : Serializable
