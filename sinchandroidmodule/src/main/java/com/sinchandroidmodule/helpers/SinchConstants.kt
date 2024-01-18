package com.sinchandroidmodule.helpers

internal interface SinchConstants {

    object Preferences {
        const val userModel = "UserModel"
    }

    object UserType {
        const val caller = "caller"
        const val receiver = "receiver"

    }
    object NotificationConstants {
        const val actionButtons = "actionButtons"
        const val DEF_CHANNEL_ID = "Calling"
        const val DEF_CHANNEL_DESC = "this channel is design for calling "
    }

    object Broadcast {
        const val clientStarted = "clientStarted"
        const val connectionEstablished = "connectionEstablished"
        const val incomingCall = "incomingCall"
        const val type = "broadcastType"
    }

    object Calls {
        const val payload = "AudioCall"
        const val audioCall = "Audio Call"
        const val videoCall = "Video Call"
        const val type = "callType"
        const val caller = "caller"
        const val callerID = "callerID"
        const val min = "min"
        const val seconds = "seconds"
    }

}