package com.paperpig.maimaidata.utils

import android.content.res.Resources
import android.util.TypedValue

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