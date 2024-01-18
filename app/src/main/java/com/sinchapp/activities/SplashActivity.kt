package com.sinchapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sinchapp.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var sp: SharedPreferences? = null

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sp = getSharedPreferences(this.packageName, MODE_PRIVATE)
        val userId = sp!!.getString("userId", "")
        val intent = if (userId.isNullOrEmpty()) {
            Intent(this, RegisterActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(intent)
            finishAffinity()
        }, 2500)

    }
}