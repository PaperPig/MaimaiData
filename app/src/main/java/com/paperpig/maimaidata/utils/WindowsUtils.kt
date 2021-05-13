package com.paperpig.maimaidata.utils

import android.content.Context

object WindowsUtils {
    fun getWindowWidth(context: Context?): Float {
        val dm = context?.resources?.displayMetrics ?: return 0F
        return dm.widthPixels.toFloat()
    }

    fun dp2px(context: Context, dp: Float): Float {
        val density = context.resources.displayMetrics.density
        return dp * density + 0.5f

    }
}