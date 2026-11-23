package com.multiclone.app.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PocLogger {
    private const val TAG = "MultiClonePoC"
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun info(message: String): String {
        val line = "[${formatter.format(Date())}] INFO: $message"
        Log.i(TAG, line)
        return line
    }

    fun warn(message: String): String {
        val line = "[${formatter.format(Date())}] WARN: $message"
        Log.w(TAG, line)
        return line
    }

    fun error(message: String, throwable: Throwable? = null): String {
        val detail = throwable?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: ""
        val line = "[${formatter.format(Date())}] ERR: $message $detail".trim()
        Log.e(TAG, line, throwable)
        return line
    }
}
