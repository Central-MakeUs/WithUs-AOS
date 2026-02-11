package org.withus.app

import android.util.Log
import kotlin.run

private const val TAG = "WithUs"

private val isEnableLogging = true

private fun getCallerInfo(cls: Class<*>? = null): String {
    val ste = Throwable().stackTrace

    return "[${cls?.simpleName ?: ste[3].fileName}]" +
            "[${ste[3].methodName}][${ste[3].lineNumber}]"
}

fun verbose(vararg any: Any) {
    if (!isEnableLogging) return
    kotlin.text.StringBuilder().run {
        for (msg in any) {
            this.append(msg)
        }

        Log.v(TAG, "$this ${getCallerInfo()}")
    }
}

fun debug(msg: String) {
    if (!isEnableLogging) return
    Log.d(TAG, "$msg ${getCallerInfo()}")
}

fun debug(cls: Class<*>, msg: String) {
    if (!isEnableLogging) return
    Log.d(TAG, "$msg ${getCallerInfo(cls)}")
}

fun debug(vararg any: Any) {
    if (!isEnableLogging) return
    kotlin.text.StringBuilder().run {
        for (msg in any) {
            this.append(msg)
        }

        Log.d(TAG, "$this ${getCallerInfo()}")
    }
}

fun info(msg: String) {
    if (!isEnableLogging) return
    Log.i(TAG, "$msg ${getCallerInfo()}")
}

fun info(cls: Class<*>, msg: String) {
    if (!isEnableLogging) return
    Log.i(TAG, "$msg ${getCallerInfo(cls)}")
}

fun info(vararg any: Any) {
    if (!isEnableLogging) return
    kotlin.text.StringBuilder().run {
        for (msg in any) {
            this.append(msg)
        }

        Log.i(TAG, "$this ${getCallerInfo()}")
    }
}

fun warn(msg: String) {
    Log.w(TAG, "$msg ${getCallerInfo()}")
}

fun warn(vararg any: Any) {
    kotlin.text.StringBuilder().run {
        for (msg in any) {
            this.append(msg)
        }

        Log.w(TAG, "$this ${getCallerInfo()}")
    }
}

fun errorLog(msg: String) {
    Log.e(TAG, "$msg ${getCallerInfo()}")
}

fun errorLog(vararg any: Any) {
    kotlin.text.StringBuilder().run {
        for (msg in any) {
            this.append(msg)
        }

        Log.e(TAG, "$this ${getCallerInfo()}")
    }
}
