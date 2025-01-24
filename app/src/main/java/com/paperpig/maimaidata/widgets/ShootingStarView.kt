package com.paperpig.maimaidata.widgets

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.paperpig.maimaidata.R

class ShootingStarView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    //流星头bitmap
    private val starHeadBitmap =
        AppCompatResources.getDrawable(context, R.drawable.mmd_shooting_star_head)?.toBitmap()

    //流星尾bitmap
    private val starTailBitmap =
        AppCompatResources.getDrawable(context, R.drawable.mmd_shooting_star_tail)?.toBitmap()

    //流星头绘制区域
    private var starHeadRect = RectF()

    //流星尾绘制区域
    private var starTailRect = RectF()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 旋转动画对象
    private var rotationAnimator: ObjectAnimator? = null

    // 旋转角度
    private var rotationAngle = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        // 使用 ObjectAnimator 实现旋转动画
        rotationAnimator = ObjectAnimator.ofFloat(this, "rotationAngle", 0f, 360f).apply {
            duration = 2000  // 旋转一圈的时间
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 保存当前的canvas状态
        canvas.save()
        // 设置旋转中心为流星头的中心点
        val centerX = (starHeadRect.left + starHeadRect.right) / 2
        val centerY = (starHeadRect.top + starHeadRect.bottom) / 2
        // 旋转整个画布
        canvas.rotate(rotationAngle, centerX, centerY)
        // 绘制旋转后的流星头
        canvas.drawBitmap(starHeadBitmap!!, null, starHeadRect, paint)
        // 恢复canvas状态，避免影响流星尾的绘制
        canvas.restore()
        canvas.drawBitmap(starTailBitmap!!, null, starTailRect, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val starHeadSideLength = width * 0.14
        val starTailSideLength = width * 0.91

        starHeadRect.set(
            0f,
            (height - starHeadSideLength).toFloat(),
            starHeadSideLength.toFloat(),
            height.toFloat()
        )

        starTailRect.set(
            (width - starTailSideLength).toFloat(),
            0f,
            width.toFloat(),
            starTailSideLength.toFloat()
        )

    }


    fun pauseAnimation() {
        rotationAnimator?.pause()
    }

    fun resumeAnimation() {
        rotationAnimator?.resume()
    }
}