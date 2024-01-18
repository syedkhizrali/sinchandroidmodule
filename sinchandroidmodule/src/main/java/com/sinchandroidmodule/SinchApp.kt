package com.sinchandroidmodule

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.PushConfiguration
import com.sinch.android.rtc.PushTokenRegistrationCallback
import com.sinch.android.rtc.PushTokenUnregistrationCallback
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.SinchPush
import com.sinch.android.rtc.SinchPush.queryPushNotificationPayload
import com.sinch.android.rtc.UserController
import com.sinch.android.rtc.UserRegistrationCallback
import com.sinchandroidmodule.callbacks.PushTokenRegisterCallback
import com.sinchandroidmodule.callbacks.PushTokenUnregisterCallback
import com.sinchandroidmodule.callbacks.UserRegisterCallbacks
import com.sinchandroidmodule.helpers.BackgroundAudioHandler
import com.sinchandroidmodule.helpers.SinchConstants
import com.sinchandroidmodule.helpers.SinchJwt
import com.sinchandroidmodule.helpers.SinchSharedPrefUtils
import com.sinchandroidmodule.models.SinchUserModel
import com.sinchandroidmodule.models.UserCallModel
import com.sinchandroidmodule.services.SinchService

class SinchApp(private val context: Context) {
    private val preference by lazy { SinchSharedPrefUtils(context) }

    companion object {
        fun isSinchPayload(payload: Map<String, String>): Boolean {
            return SinchPush.isSinchPushPayload(payload)
        }
    }

    fun registerUser(
        model: SinchUserModel,
        userRegisterCallbacks: UserRegisterCallbacks? = null,
        pushTokenRegisterCallback: PushTokenRegisterCallback? = null
    ) {

        preference.setUserModel(model)

        UserController.builder().context(context).userId(model.userID).applicationKey(model.key)
            .environmentHost(model.environment).pushConfiguration(
                PushConfiguration.fcmPushConfigurationBuilder().senderID(model.fcmSenderID)
                    .registrationToken(model.fcmToken).build()
            ).build().also { it ->
                it.registerUser(object : UserRegistrationCallback {
                    override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
                        val jwt: String = SinchJwt.create(
                            model.key, model.secret, model.userID
                        )
                        clientRegistration.register(jwt)
                    }

                    override fun onUserRegistered() {
                        userRegisterCallbacks?.onUserRegistered()

                    }

                    override fun onUserRegistrationFailed(error: SinchError) {
                        userRegisterCallbacks?.onUserRegistrationFailed(error.message)
                    }
                }, object : PushTokenRegistrationCallback {
                    override fun onPushTokenRegistered() {
                        pushTokenRegisterCallback?.onPushTokenRegistered()
                    }

                    override fun onPushTokenRegistrationFailed(error: SinchError) {
                        pushTokenRegisterCallback?.onPushTokenRegistrationFailed(error.message)
                    }
                })
            }

    }

    fun signOut(
        pushTokenUnregisterCallback: PushTokenUnregisterCallback? = null
    ) {

        val model = preference.getUserModel()
        UserController.builder().context(context).applicationKey(model?.key ?: "")
            .userId(model?.userID ?: "")
            .environmentHost(model?.environment ?: "")
            .pushConfiguration(
                PushConfiguration.fcmPushConfigurationBuilder().senderID(model?.fcmSenderID ?: "")
                    .registrationToken(model?.fcmToken ?: "").build()
            )
            .build()
            .unregisterPushToken(object : PushTokenUnregistrationCallback {
                override fun onPushTokenUnregistered() {
                    preference.setUserModel(null)
                    pushTokenUnregisterCallback?.onPushTokenUnregistered()
                }

                override fun onPushTokenUnregistrationFailed(error: SinchError) {
                    pushTokenUnregisterCallback?.onPushTokenUnRegistrationFailed(error.message)
                }
            })
    }

    fun handelCall(data: MutableMap<String, String>) {
        if (SinchPush.isSinchPushPayload(data)) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                BackgroundAudioHandler(context).requestAudioFocus()
                preference.getUserModel()?.let {
                    context.startService(Intent(context, SinchService::class.java).apply {
                        putExtra(SinchConstants.Calls.payload, preference.getUserModel())
                    })
                }
                context.bindService(
                    Intent(context, SinchService::class.java).apply {
                        putExtra(
                            SinchConstants.Calls.payload,
                            SinchSharedPrefUtils(context).getUserModel()
                        )
                    },
                    object : ServiceConnection {
                        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                            if (p1 is SinchService.SinchClientServiceBinder) {
                                val client: SinchClient? = p1.sinchClient
                                if (client != null) {
                                    val result = queryPushNotificationPayload(
                                        context,
                                        data
                                    )
                                    client.relayRemotePushNotification(result)
                                }
                            }
                            context.unbindService(this)
                        }

                        override fun onServiceDisconnected(p0: ComponentName?) {
                            Log.e("onServiceDisconnected", "onServiceDisconnected: ")
                        }
                    },
                    Context.BIND_AUTO_CREATE
                )
            } else {
                Looper.prepare()
                Toast.makeText(
                    context.applicationContext,
                    "Permission of READ_PHONE_STATE is missing",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    fun placeVoiceCall(
        userCallModel: UserCallModel,
        launcher: ActivityResultLauncher<Intent>? = null,
        min: Int? = null,
        seconds: Int? = null
    ) {
        if (preference.getUserModel() != null) {
            userCallModel.callerName = preference.getUserModel()?.userName
            BackgroundAudioHandler(context).requestAudioFocus()
            val intent = Intent(context, SinchCallActivity::class.java).apply {
                putExtra(SinchConstants.Calls.type, SinchConstants.Calls.audioCall)
                putExtra(SinchConstants.Calls.payload, userCallModel)
                seconds?.let {
                    putExtra(SinchConstants.Calls.seconds, it)
                }
                min?.let {
                    putExtra(SinchConstants.Calls.min, it)
                }
                putExtra(SinchConstants.Calls.caller, true)
            }
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                context.startActivity(intent)
            }
        } else {
            Toast.makeText(context, "User Registration is not done yet!!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun placeVideoCall(
        userCallModel: UserCallModel,
        launcher: ActivityResultLauncher<Intent>? = null,
        min: Int? = null,
        seconds: Int? = null
    ) {
        if (preference.getUserModel() != null) {
            userCallModel.callerName = preference.getUserModel()?.userName
            BackgroundAudioHandler(context).requestAudioFocus()
            val intent = Intent(context, SinchCallActivity::class.java).apply {
                putExtra(SinchConstants.Calls.type, SinchConstants.Calls.videoCall)
                putExtra(SinchConstants.Calls.payload, userCallModel)
                seconds?.let {
                    putExtra(SinchConstants.Calls.seconds, it)
                }
                min?.let {
                    putExtra(SinchConstants.Calls.min, it)
                }
                putExtra(SinchConstants.Calls.caller, true)
            }

            if (launcher != null) {
                launcher.launch(intent)
            } else {
                context.startActivity(intent)
            }
        } else {
            Toast.makeText(context, "User Registration is not done yet!!", Toast.LENGTH_SHORT)
                .show()
        }
    }


}