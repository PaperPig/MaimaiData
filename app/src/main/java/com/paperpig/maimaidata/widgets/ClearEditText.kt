package com.paperpig.maimaidata.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.toDp

class ClearEditText(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs) {
    private var clearTextDrawable: Drawable? = null
    private var searchLensDrawable: Drawable? = null


    init {
        clearTextDrawable = context.let {
            val drawable =
                ContextCompat.getDrawable(it, R.drawable.ic_delete)
            drawable?.setBounds(
                0,
                0,
                20.toDp().toInt(),
                20.toDp().toInt()
            )
            drawable
        }
        searchLensDrawable = context.let {
            val drawable =
                ContextCompat.getDrawable(it, R.drawable.mmd_search_lens)
            drawable?.setBounds(
                0,
                0,
                20.toDp().toInt(),
                20.toDp().toInt()
            )
            drawable
        }
        setCompoundDrawables(
            searchLensDrawable,
            compoundDrawables[1],
            compoundDrawables[2],
            compoundDrawables[3]
        )
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


    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setClearIconVisible(focused && text!!.isNotEmpty())
    }


    @SuppressLint("ClickableViewAccessibility")
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
            if (boolean) clearTextDrawable else null,
            compoundDrawables[3]
        )
    }
}