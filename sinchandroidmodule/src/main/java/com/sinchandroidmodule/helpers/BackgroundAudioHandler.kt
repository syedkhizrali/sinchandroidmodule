package com.sinchandroidmodule.helpers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

internal class BackgroundAudioHandler(context: Context) {
    private var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    }


    fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    // Handle audio focus changes here
                }
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                abandonAudioFocus()
            } else {
                // Audio focus request denied
            }
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            val afChangeListener =
                AudioManager.OnAudioFocusChangeListener { focusChange: Int -> }
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }

}