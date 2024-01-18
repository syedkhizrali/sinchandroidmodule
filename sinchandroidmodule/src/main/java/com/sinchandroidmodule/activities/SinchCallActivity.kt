package com.sinchandroidmodule


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.MediaConstraints
import com.sinch.android.rtc.video.VideoCallListener
import com.sinch.android.rtc.video.VideoController
import com.sinch.android.rtc.video.VideoScalingType
import com.sinchandroidmodule.databinding.ActivitySinchCallBinding
import com.sinchandroidmodule.helpers.SinchConstants
import com.sinchandroidmodule.helpers.SinchNotificationUtils
import com.sinchandroidmodule.helpers.SinchProximitySensor
import com.sinchandroidmodule.models.SinchCallResult
import com.sinchandroidmodule.models.UserCallModel
import com.sinchandroidmodule.services.SinchService


internal class SinchCallActivity : AppCompatActivity() {
    private var seconds: Int? = null
    private var minutes: Int? = null
    private var timeProvided: Int? = null
    private var resultModel = SinchCallResult()
    private lateinit var screenManager: SinchProximitySensor

    private var permissionStatus: MutableLiveData<Boolean> = MutableLiveData()
    private val callType by lazy {
        intent.getStringExtra(SinchConstants.Calls.type) ?: SinchConstants.Calls.audioCall
    }
    private var speakerEnabled = false
    private var onMute = false
    private var actionButtons: Boolean? = null
    private val userCallModel by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(SinchConstants.Calls.payload, UserCallModel::class.java)
        } else {
            intent.getSerializableExtra(SinchConstants.Calls.payload) as UserCallModel
        }
    }
    private val callerID by lazy { userCallModel?.callerID }
    private val caller by lazy { intent.getBooleanExtra(SinchConstants.Calls.caller, false) }
    private var sinchClient: SinchClient? = null
    private var ongoingCall: Call? = null
    private val binding by lazy { ActivitySinchCallBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        screenManager = SinchProximitySensor(this)
        checkPermissions()
        if (intent.hasExtra(SinchConstants.NotificationConstants.actionButtons)) {
            actionButtons =
                intent.getBooleanExtra(SinchConstants.NotificationConstants.actionButtons, true)
        }
        if (intent.hasExtra(SinchConstants.Calls.min)) {
            minutes = intent.getIntExtra(SinchConstants.Calls.min, 0)
        }
        if (intent.hasExtra(SinchConstants.Calls.seconds)) {
            seconds = intent.getIntExtra(SinchConstants.Calls.seconds, 0)
        }
        timeProvided = (seconds ?: 0) + ((minutes ?: 0) * 60)


        if (sinchClient == null) {
            permissionStatus.observe(this@SinchCallActivity) {
                if (it) {
                    bindService(
                        Intent(application, SinchService::class.java),
                        serviceConnector,
                        Context.BIND_AUTO_CREATE
                    )
                }
            }
        }
        binding.imgRejectCall.setOnClickListener {
            rejectCall()
        }
        binding.imgAcceptCall.setOnClickListener {
            acceptCall()
        }
        binding.hangup.setOnClickListener {
            rejectCall()
        }
        binding.speaker.setOnClickListener {
            handelSpeaker()
        }
        binding.mute.setOnClickListener {

            handelMute()
        }
    }

    private val enableStates =
        arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf(-android.R.attr.state_pressed))
    private val disableStates =
        arrayOf(intArrayOf(-android.R.attr.state_pressed), intArrayOf(android.R.attr.state_pressed))

    private var enableColors = intArrayOf(
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#00FF00")
    )

    private var enableColorStateList = ColorStateList(enableStates, enableColors)
    private var disableColorStateList = ColorStateList(disableStates, enableColors)

    private fun handelSpeaker() {
        if (speakerEnabled) {
            screenManager.releaseWakeLock()
            sinchClient?.audioController?.enableSpeaker()
            binding.speaker.backgroundTintList = enableColorStateList
        } else {
            screenManager.acquireWakeLock()
            sinchClient?.audioController?.disableSpeaker()
            binding.speaker.backgroundTintList = disableColorStateList
        }
        speakerEnabled = !speakerEnabled

    }

    private fun handelMute() {
        if (onMute) {
            binding.mute.setImageResource(R.drawable.ic_mic_lib)
            sinchClient?.audioController?.unmute()
        } else {
            binding.mute.setImageResource(R.drawable.ic_mute_lib)
            sinchClient?.audioController?.mute()
        }
        onMute = !onMute

    }


    private fun establishCall(id: String?) {

        id?.let {
            binding.actionsLayout.ronVisible(true)
            binding.bottomLayout.ronVisible(false)
            binding.callState.text = "Dialling call"
            binding.callerName.text = userCallModel?.receiverName
            when (callType) {
                SinchConstants.Calls.videoCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(true),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVideoCallListener)
                }

                SinchConstants.Calls.audioCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(false),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }

                else -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(false),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }
            }
            onMute = true
            handelMute()
        }
    }


    private fun startListeningIncomingCall() {
        if (sinchClient == null) {
            Log.d("classTAG", "startListeningIncomingCall: NULL")
        }
        ongoingCall = sinchClient?.callController?.getCall(callerID ?: "")
        if (!caller) {
            binding.bottomLayout.ronVisible(true)
        }
        binding.actionsLayout.ronVisible(false)
        if (actionButtons == true) {
            acceptCall()
        } else if (actionButtons == false) {
            rejectCall()
        }
        binding.callerName.text = userCallModel?.callerName
        binding.callState.text = callType
        if (ongoingCall?.details?.isVideoOffered == true) {
            binding.audioInfoLayout.ronVisible(true)
            ongoingCall?.addCallListener(ongoingVideoCallListener)
        } else {
            ongoingCall?.addCallListener(ongoingVoiceCallListener)
        }
    }

    private fun rejectCall() {
        if (caller) {
            resultModel.callEndBy = SinchConstants.UserType.caller
        } else {
            resultModel.callEndBy = SinchConstants.UserType.receiver
        }
        SinchNotificationUtils.stopRingTone()
        ongoingCall?.hangup()
    }

    private fun acceptCall() {
        SinchNotificationUtils.stopRingTone()
        onMute = true
        handelMute()
        ongoingCall?.answer()
    }


    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(SinchConstants.Broadcast.connectionEstablished)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra(SinchConstants.Broadcast.type)) {
                permissionStatus.observe(this@SinchCallActivity) {
                    if (it) {
                        establishCall(callerID)
                    }
                }
            }
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    fun disconnectService() {
        binding.duration.ronVisible(false)
        binding.callState.text = getString(R.string.call_ended)
        screenManager.releaseWakeLock()
        SinchNotificationUtils.stopRingTone()
        stopService(Intent(this, SinchService::class.java))
        setResult(Activity.RESULT_OK, Intent().also {
            it.putExtra("result", resultModel)
        })
        finishAndRemoveTask()

    }


    private val serviceConnector = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            if (p1 is SinchService.SinchClientServiceBinder) {
                handelUI()
                sinchClient = p1.sinchClient
                if (sinchClient?.isStarted == true && caller) {
                    establishCall(callerID)

                } else {
                    startListeningIncomingCall()
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.e("onServiceDisconnected", ": ${p0.toString()}")

        }
    }

    private fun handelUI() {
        if (callType == SinchConstants.Calls.audioCall) {
            speakerEnabled = false
            binding.audioInfoLayout.ronVisible(true)
            binding.localView.ronVisible(false)
            binding.remoteView.ronVisible(false)
        } else {
            binding.speaker.ronVisible(false)
            speakerEnabled = true
            binding.imgAcceptCall.setImageResource(R.drawable.ic_video_answer_lib)
            binding.localView.ronVisible(true)
            binding.remoteView.ronVisible(true)
        }
        handelSpeaker()
    }


    private val ongoingVoiceCallListener = object : CallListener {
        override fun onCallProgressing(p0: Call) {
//            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.callState.text = getString(R.string.ringing)
            binding.duration.ronVisible(false)


        }

        override fun onCallEstablished(p0: Call) {
            SinchNotificationUtils.stopRingTone()
            binding.callState.text = getString(R.string.connected)
            binding.bottomLayout.ronVisible(false)
            binding.actionsLayout.ronVisible(true)
            binding.duration.ronVisible(true)
            startCallTimer()

        }

        override fun onCallEnded(p0: Call) {
            disconnectService()

        }

    }

    private val ongoingVideoCallListener = object : VideoCallListener {
        override fun onCallProgressing(p0: Call) {
//            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.audioInfoLayout.ronVisible(true)
            binding.callState.text = getString(R.string.ringing)
        }

        override fun onCallEstablished(p0: Call) {
            sinchClient?.audioController?.enableSpeaker()
            binding.bottomLayout.ronVisible(false)
            binding.actionsLayout.ronVisible(true)
            binding.audioInfoLayout.ronVisible(false)
            startCallTimer()
        }

        override fun onCallEnded(p0: Call) {
            binding.audioInfoLayout.ronVisible(true)
            binding.localView.removeAllViews()
            binding.remoteView.removeAllViews()
            disconnectService()

        }


        override fun onVideoTrackAdded(p0: Call) {
            val vc: VideoController? = sinchClient?.videoController
            vc?.let {
                binding.localView.ronVisible(true)
                binding.remoteView.ronVisible(true)
                binding.localView.addView(it.localView)
                binding.remoteView.addView(it.remoteView)
                it.setResizeBehaviour(VideoScalingType.ASPECT_FILL)
            }
        }

        override fun onVideoTrackPaused(p0: Call) {
            //PAUSED
        }

        override fun onVideoTrackResumed(p0: Call) {
            //RESUMED
        }
    }

    private val callTimerHandler = Handler(Looper.getMainLooper())
    private var callRunnableHandler: Runnable? = null
    private fun startCallTimer() {
        callRunnableHandler = object : Runnable {
            override fun run() {
                ongoingCall?.details?.duration?.let {
                    resultModel.callDurationInSec = it
                    splitToComponentTimes(it)
                    if (timeProvided != null && (timeProvided ?: 0) > 1) {
                        val counter = timeProvided!! - it
                        if (counter <= 0) {
                            rejectCall()
                        }
                    }

                }
                callTimerHandler.postDelayed(this, 1000)
            }
        }
        callRunnableHandler?.let {
            callTimerHandler.post(it)
        }
    }

    @SuppressLint("SetTextI18n")
    fun splitToComponentTimes(counter: Int) {
        val longVal: Int = counter
        val hours = longVal / 3600
        var remainder = longVal - hours * 3600
        val minutes = remainder / 60
        remainder -= minutes * 60
        val secs = remainder
        if (hours > 0) {
            binding.duration.text =
                "${String.format("%02d", hours)}:${
                    String.format(
                        "%02d",
                        minutes
                    )
                }:${String.format("%02d", secs)}"
        } else if (minutes > 0) {
            binding.duration.text =
                "${String.format("%02d", minutes)}:${String.format("%02d", secs)}"

        } else {
//            binding.duration.text = "0:$secs"
            binding.duration.text = String.format("00:%02d", secs)
        }
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE
            )
        }
        if (!hasPermissions(this, *permissions)) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                1000
            )
        } else {
            permissionStatus.postValue(true)
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (hasPermissions(this, *permissions)) {
                permissionStatus.postValue(true)
            } else {
                permissionStatus.postValue(false)
            }
        }
    }

    override fun onDestroy() {
        rejectCall()
        super.onDestroy()
    }



}


