package com.paperpig.maimaidata.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

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

fun View.setShrinkOnTouch(
    scale: Float = 0.9f,
    duration: Long = 100L,
    keepLongClick: Boolean = false
) {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(duration)
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .start()
            }
        }
        keepLongClick
    }
}

fun View.setCopyOnLongClick(
    textToCopy: String,
    label: String = "Copied Text",
    copiedMessage: String = "已复制：$textToCopy",
    errorMessage: String = "无法访问剪贴板"
) {
    setOnLongClickListener {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText(label, textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
        true
    }
}