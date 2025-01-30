package com.paperpig.maimaidata.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.utils.toDp
import kotlin.math.max

class ChartBarView(
    context: Context, attrs: AttributeSet
) : View(context, attrs) {

    private val labels = listOf("Total", "Tap", "Hold", "Slide", "Touch", "Break")
    private val values = mutableListOf(0, 0, 0, 0, 0, 0)
    private val maxValues = mutableListOf(100, 100, 100, 100, 100, 100)

    private val padding = 12.toDp()
    private val barHeight = 20.toDp()
    private var noteWidth = 0f
    private var valueWidth = 0f
    private var maxBarWidth = 0f

    // 确保圆角不会超过柱状图的高度的一半
    private val cornerRadius = 8.toDp()

    // 设置一个最小的柱状图宽度，确保圆角能够显示
    private val minBarWidth = 16.toDp() // 最小柱状图宽度，根据需要调整

    private val bgRect = RectF()
    private val barRect = RectF()


    private val noteTypePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            resources.displayMetrics
        )
        textAlign = Paint.Align.RIGHT
    }

    private val noteValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            resources.displayMetrics
        )
        textAlign = Paint.Align.LEFT
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        Color.BLUE
    }

    private val barBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
    }

    fun setMaxValues(maxList: List<Int>) {
        for (i in maxValues.indices) {
            maxValues[i] = max(1, maxList.getOrNull(i) ?: 100)
        }
        requestLayout()
        invalidate()
    }


    init {

        noteWidth = noteTypePaint.measureText("total")
        valueWidth = noteValuePaint.measureText("1400")
    }

    fun setValues(newValues: List<Int>) {
        for (i in values.indices) {
            values[i] = max(0, newValues.getOrNull(i) ?: 0)
        }
        invalidate()
    }


    fun setBarColor(@ColorRes colorRes: Int) {
        barPaint.color = ContextCompat.getColor(context, colorRes)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (barHeight + padding) * labels.size + padding   // Bar height + padding
        val height = resolveSize(desiredHeight.toInt(), heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Reserve space for value text
        maxBarWidth = width - noteWidth - paddingLeft - paddingRight - valueWidth - padding * 2

        val textOffset = (noteTypePaint.descent() + noteTypePaint.ascent()) / 2


        for (i in labels.indices) {
            val centerY = padding + (barHeight + padding) * i + barHeight / 2
            canvas.drawText(labels[i], paddingLeft + noteWidth, centerY - textOffset, noteTypePaint)

            // 计算实际的柱状图宽度
            val barWidth =
                if (maxValues[i] > 0) (values[i].toFloat() / maxValues[i] * maxBarWidth) else 0f
            // 如果柱状图宽度小于最小宽度，则使用最小宽度
            val adjustedBarWidth = barWidth.coerceIn(minBarWidth, maxBarWidth)
            val barTop = centerY - barHeight / 2
            val barBottom = centerY + barHeight / 2
            val barEndX = noteWidth + paddingLeft + padding + adjustedBarWidth

            // Draw rounded bar background
            bgRect.set(
                paddingLeft + noteWidth + padding,
                barTop,
                paddingLeft + noteWidth + padding + maxBarWidth,
                barBottom
            )
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, barBackgroundPaint)

            // Draw rounded bar with specific color
            if (values[i] != 0) {
                barRect.set(paddingLeft + noteWidth + padding, barTop, barEndX, barBottom)
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }
            // Draw value text at the end of the bar
            canvas.drawText(
                values[i].toString(),
                paddingLeft + noteWidth + padding + maxBarWidth + padding,
                centerY - textOffset,
                noteValuePaint
            )
        }
    }
}
