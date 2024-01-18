package com.sinchapp.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sinchandroidmodule.SinchApp
import com.sinchandroidmodule.callbacks.PushTokenUnregisterCallback
import com.sinchandroidmodule.models.SinchCallResult
import com.sinchandroidmodule.models.UserCallModel
import com.sinchapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var sp: SharedPreferences? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sp = getSharedPreferences(this.packageName, MODE_PRIVATE)
        val userID: String? = sp!!.getString("userId", "")
        initListeners()

        binding.yourUserId.text = "Your User ID :$userID"

    }

    private fun initListeners() {
        binding.btnVoiceCall.setOnClickListener {
            SinchApp(this).placeVoiceCall(
                UserCallModel(
                    binding.etTargetID.text.toString(),
                    receiverName = binding.etUserName.text.toString(),
                ),
                launcher = launcher,
                seconds = 11
            )
        }
        binding.btnVideoCall.setOnClickListener {
            if (binding.etTargetID.text.toString().length < 5) {
                Toast.makeText(
                    this,
                    "UserID must be valid(length greater then 5)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (binding.etUserName.text.toString().length < 5) {
                Toast.makeText(
                    this,
                    "etUserName must be valid(length greater then 5)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            SinchApp(this).placeVideoCall(
                UserCallModel(
                    binding.etTargetID.text.toString(),
                    receiverName = binding.etUserName.text.toString()
                ), seconds = 111
            )
        }

        binding.logout.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Alert").setMessage("Are u Sure u wanna Logout")
                .setPositiveButton("Yes") { _, _ ->
                    logoutFromSinch()
                }.setNegativeButton("Cancel") { _, _ ->
                }.show()
        }
    }

    private fun logoutFromSinch() {
        SinchApp(this).signOut(object : PushTokenUnregisterCallback {
            override fun onPushTokenUnregistered() {
                Toast.makeText(
                    this@MainActivity,
                    "User Logged out Successfully",
                    Toast.LENGTH_SHORT
                ).show()
                sp?.edit()?.clear()?.apply()
                startActivity(Intent(this@MainActivity, SplashActivity::class.java))
                finishAffinity()
            }

            override fun onPushTokenUnRegistrationFailed(error: String?) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                Log.e("onPushTokenUnregistered", ":ERROR ->  $error")
            }
        })
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("result", SinchCallResult::class.java)
            } else {
                result.data?.getSerializableExtra("result") as SinchCallResult
            }

            Log.e("RonSinchCallResul", ": $data")
        }


    }

}