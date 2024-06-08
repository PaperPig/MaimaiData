package com.paperpig.maimaidata.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.paperpig.maimaidata.utils.toDp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RaysView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lines = mutableListOf<Line>()
    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f // 设置线条宽度
    }

    init {
        startAnimation()
    }

    private fun startAnimation() {
        val interval = 250L // 添加新射线建个
        val duration = 1200L // 动画持续时间
        val maxRadius = 300.toDp()// 最大到达距离

        post(object : Runnable {
            override fun run() {
                val line = createLine()
                lines.add(line)
                line.startAnimation(duration, maxRadius)
                postDelayed(this, interval)
            }
        })
    }

    private fun createLine(): Line {
        val angle = Random.nextFloat() * Math.PI //只在下方射出光线
        return Line(angle)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        lines.forEach { line ->
            val startX = cx + line.startRadius * cos(line.angle).toFloat()
            val startY = cy + line.startRadius * sin(line.angle).toFloat()
            val endX = cx + line.endRadius * cos(line.angle).toFloat()
            val endY = cy + line.endRadius * sin(line.angle).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }

    private inner class Line(val angle: Double) {
        var startRadius = 0f
        var endRadius = 0f

        fun startAnimation(duration: Long, maxRadius: Float) {
            val startTime = System.currentTimeMillis()

            post(object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - startTime
                    val fraction = elapsed.toFloat() / duration
                    startRadius = fraction * maxRadius * 0.8f
                    endRadius = fraction * maxRadius
                    if (fraction < 1f) {
                        invalidate()
                        postDelayed(this, 16)
                    } else {
                        lines.remove(this@Line)
                    }
                }
            })
        }
    }
}