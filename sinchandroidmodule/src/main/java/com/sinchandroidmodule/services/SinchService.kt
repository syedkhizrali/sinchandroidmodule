package com.sinchandroidmodule.services

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.PushConfiguration
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchClientListener
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallControllerListener
import com.sinchandroidmodule.SinchCallActivity
import com.sinchandroidmodule.helpers.SinchConstants
import com.sinchandroidmodule.helpers.SinchJwt
import com.sinchandroidmodule.helpers.SinchNotificationUtils
import com.sinchandroidmodule.helpers.SinchSharedPrefUtils
import com.sinchandroidmodule.models.SinchUserModel
import com.sinchandroidmodule.models.UserCallModel


@SuppressLint("AnnotateVersionCheck")
internal class SinchService : Service() {
    private var sinchClientInstance: SinchClient? = null
    private var sinchAppClientListener: SinchClientListener? = null

    private val preferences by lazy { SinchSharedPrefUtils(this) }
    private val model: SinchUserModel? by lazy { preferences.getUserModel() }
    private val systemVersionDisallowsExplicitActivityStart: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onBind(p0: Intent?): IBinder {
        return SinchClientServiceBinder()
    }

    inner class SinchClientServiceBinder : Binder() {
        val sinchClient: SinchClient? get() = sinchClientInstance
    }

    override fun onCreate() {
        super.onCreate()
        registerSinchClientIfNecessary()

    }

    private fun registerSinchClientIfNecessary() {
        if (sinchClientInstance != null && sinchClientInstance?.isStarted == true) {
            return
        }
        if (model != null) {
            val userID = model?.userID
            val userName = model?.userName
            sinchClientInstance = SinchClient.builder().context(this)
                .environmentHost(model?.environment ?: "")
                .applicationKey(model?.key ?: "").userId(userID ?: "")
                .enableVideoCalls(model?.enableVideoCalls ?: true)
                .pushNotificationDisplayName(userName ?: "")
                .pushConfiguration(
                    PushConfiguration.fcmPushConfigurationBuilder()
                        .senderID(model?.fcmSenderID ?: "")
                        .registrationToken(model?.fcmToken ?: "")
                        .build()
                )
                .build().apply {
                    addSinchClientListener(sinchClientListener)
                    start()
                }
        }
        sinchClientInstance?.callController?.addCallControllerListener(callController)

    }


    private val sinchClientListener = object : SinchClientListener {
        override fun onClientFailed(client: SinchClient, error: SinchError) {
            sinchAppClientListener?.onClientFailed(client,error)
        }

        override fun onClientStarted(client: SinchClient) {
            val tempIntent = Intent(SinchConstants.Broadcast.connectionEstablished)
            tempIntent.putExtra(SinchConstants.Broadcast.type, SinchConstants.Broadcast.clientStarted)
            sendBroadcast(tempIntent)
            sinchAppClientListener?.onClientStarted(client)
        }

        override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
            val jwt: String = SinchJwt.create(
                model?.key, model?.secret, model?.userID
            )
            clientRegistration.register(jwt)
        }

        override fun onLogMessage(level: Int, area: String, message: String) {
            sinchAppClientListener?.onLogMessage(level, area, message)
        }

        override fun onPushTokenRegistered() {
            sinchAppClientListener?.onPushTokenRegistered()
        }

        override fun onPushTokenRegistrationFailed(error: SinchError) {
            sinchAppClientListener?.onPushTokenRegistrationFailed(error)
        }

        override fun onPushTokenUnregistered() {
            sinchAppClientListener?.onPushTokenUnregistered()

        }

        override fun onPushTokenUnregistrationFailed(error: SinchError) {
            sinchAppClientListener?.onPushTokenUnregistrationFailed(error)
        }

        override fun onUserRegistered() {
            sinchAppClientListener?.onUserRegistered()
        }

        override fun onUserRegistrationFailed(error: SinchError) {
            sinchAppClientListener?.onUserRegistrationFailed(error)
        }
    }


    private val callController = object : CallControllerListener {
        override fun onIncomingCall(callController: CallController, call: Call) {
            SinchNotificationUtils.stopRingTone()
            val data = call.headers
            val notifications = SinchNotificationUtils(this@SinchService)
            SinchNotificationUtils.playRingTone()
            val callType = if (call.details.isVideoOffered) {
                SinchConstants.Calls.videoCall
            } else {
                SinchConstants.Calls.audioCall

            }
            val userCallModel = UserCallModel(
                call.callId,
                receiverName = data["receiverName"],
                callerName = data["callerName"]
            )
            val mainActivityIntent =
                Intent(this@SinchService, SinchCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(
                        SinchConstants.Calls.payload,
                        userCallModel
                    )
                    putExtra(SinchConstants.Calls.caller, false)
                    putExtra(SinchConstants.Calls.type, callType)

                }
            if (systemVersionDisallowsExplicitActivityStart && !checkIfInForeground()) {
                notifications.createNotification(
                    call,
                    mainActivityIntent, userCallModel
                )
            } else {
                startActivity(mainActivityIntent)
                val tempIntent = Intent(SinchConstants.Broadcast.connectionEstablished)
                tempIntent.putExtra(
                    SinchConstants.Broadcast.type,
                    SinchConstants.Broadcast.incomingCall
                )
                sendBroadcast(tempIntent)
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_NOT_STICKY

    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        if (sinchClientInstance != null && sinchClientInstance?.isStarted == true) {
            sinchClientInstance?.terminateGracefully()
            stopSelf()
        }
        super.onDestroy()
    }

    private fun checkIfInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses: List<ActivityManager.RunningAppProcessInfo> =
            activityManager.runningAppProcesses ?: return false
        return appProcesses.any { appProcess: ActivityManager.RunningAppProcessInfo ->
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName
        }
    }


}