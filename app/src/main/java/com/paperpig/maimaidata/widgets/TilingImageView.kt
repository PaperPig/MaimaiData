package com.paperpig.maimaidata.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.paperpig.maimaidata.R

class TilingImageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var shader: BitmapShader? = null
    private val paint = Paint()
    private var offsetX = 0f
    private var offsetY = 0f
    private var animator:ValueAnimator? = null

    init {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.mmd_main_bg_pattern)
        shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        paint.shader = shader

        // 使用 ValueAnimator 实现滚动效果
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 50000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                offsetX = progress * width
                offsetY = (1f - progress) * height
                invalidate() // 重绘 View
            }
        }
        animator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(-offsetX, -offsetY)
        canvas.drawPaint(paint)
        canvas.restore()
    }

    fun pauseAnimation() {
        animator?.pause()
    }

    fun resumeAnimation() {
        animator?.resume()
    }
}