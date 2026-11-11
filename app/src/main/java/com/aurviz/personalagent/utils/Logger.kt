package com.aurviz.personalagent.utils

import android.util.Log

object Logger {
    private const val TAG = "AURA"

    fun d(component: String, message: String) {
        Log.d("$TAG.$component", message)
    }

    fun i(component: String, message: String) {
        Log.i("$TAG.$component", message)
    }

    fun w(component: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG.$component", message, throwable)
    }

    fun e(component: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG.$component", message, throwable)
    }
}
