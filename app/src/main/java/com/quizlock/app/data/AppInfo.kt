package com.quizlock.app.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isLocked: Boolean = false,
    val isSystemApp: Boolean = false
)
