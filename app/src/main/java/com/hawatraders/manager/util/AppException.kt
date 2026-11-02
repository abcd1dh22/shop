package com.hawatraders.manager.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** An error whose message is already a friendly Bengali sentence safe to show to the user. */
class AppException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
