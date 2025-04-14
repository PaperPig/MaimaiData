package com.paperpig.maimaidata.utils

import android.content.res.Resources
import android.util.TypedValue
import android.view.View

fun String.getInt(): Int {
    return if (this.isEmpty()) {
        0
    } else {
        this.toInt()
    }
}

fun List<String>.versionCheck(string: String): Boolean {
    for (i in this) {
        if (i == "maimai") {
            if (string == "maimai" || string == "maimai PLUS") return true
        } else if (i == "舞萌DX") {
            if (string == "舞萌DX") return true
        } else if (string.contains(i)) return true
    }
    return false
}

fun Int.toDp(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )
}

fun View.setDebouncedClickListener(debounceTime: Long = 2000L, action: (view: View) -> Unit) {
    var lastClickTime = 0L
    this.setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            action(view)
        }
        lastClickTime = currentTime
    }
}