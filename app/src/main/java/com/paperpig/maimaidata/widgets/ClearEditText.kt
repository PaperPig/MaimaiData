package com.paperpig.maimaidata.widgets

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.WindowsUtils

class ClearEditText(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs) {
    var mDrawable: Drawable? = null


    init {
        mDrawable = context.let {
            val drawable =
                ContextCompat.getDrawable(it, R.drawable.ic_delete)
            drawable?.setBounds(
                0,
                0,
                WindowsUtils.dp2px(context, 20f).toInt(),
                WindowsUtils.dp2px(context, 20f).toInt()
            )
            drawable
        }
}


    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        setClearIconVisible(hasFocus() && text!!.isNotEmpty())
    }


    override fun onFocusChanged(focused: Boolean, direction:  Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setClearIconVisible(focused && text!!.isNotEmpty())
    }


    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN ->
                performClick()
            MotionEvent.ACTION_UP ->
                if (compoundDrawables[2] != null && event.x <= (width - paddingRight) && event.x >= (width - paddingRight - compoundDrawables[2].bounds.width())) {
                    setText("")
                }
        }
        return super.onTouchEvent(event)

    }

    private fun setClearIconVisible(boolean: Boolean) {
        setCompoundDrawables(
            compoundDrawables[0],
            compoundDrawables[1],
            if (boolean) mDrawable else null,
            compoundDrawables[3]
        )
    }
}