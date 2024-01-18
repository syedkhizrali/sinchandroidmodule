package com.sinchandroidmodule

import android.util.Log
import android.view.View

internal fun String.usernameFromCall(): String {
    Log.e("usernameFromCall", ": $this" )
    val name =
        try {
            val tempList = this.split("|")
            if (tempList.isEmpty()) {
                this
            } else {
                if (tempList.size > 1) {
                    tempList[1]
                } else {
                    tempList[0]
                }
            }

        } catch (_: Exception) {
            this
        }
    return name

}


internal fun View.ronVisible(value: Boolean) = if (value) {
    this.visibility = View.VISIBLE
} else {
    this.visibility = View.GONE

}


