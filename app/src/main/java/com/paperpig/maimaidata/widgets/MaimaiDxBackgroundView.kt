package com.paperpig.maimaidata.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import com.paperpig.maimaidata.R
import kotlin.math.hypot
import kotlin.math.max

/**
 * Maimai DX International version animated background view.
 * Recreates the official maimai DX website (https://maimai.sega.com/) background styling and animations.
 * Completely borderless full-bleed rendering without edge insets or cutoffs.
 */
class MaimaiDxBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Whether to draw the 4 cabinet corner border frame decorations.
     * Web mobile layout displays these 4 frame decorations on the screen edges.
     */
    var showCorners: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Reusable geometry objects (avoid allocations during onDraw)
    private val drawRect = RectF()
    private val drawMatrix = Matrix()

    // Shaders
    private var radialGradient: RadialGradient? = null

    // Bitmaps
    private val patternBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_pattern) }
    private val circleColorfulBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_circle_colorful) }
    private val circleWhiteBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_circle_white) }
    private val circleYellowBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_circle_yellow) }

    // Floating tiles & stars bitmaps
    private val tileGreenBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_tile_green) }
    private val tilePurpleLeftBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_tile_purple_left) }
    private val tilePurpleRightBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_tile_purple_right) }
    private val starPinkBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_star_pink) }
    private val starYellowBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_star_yellow) }

    // 3D items bitmaps
    private val cubeBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_cube) }
    private val starSmallBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_star_small) }
    private val starsBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_stars) }
    private val gloveBlueBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_glove_blue) }
    private val glovePinkBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_glove_pink) }
    private val circlePinkBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_pink) }
    private val circleOrangeBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_3d_orange) }

    // Corner bitmaps
    private val topLeftBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_top_left) }
    private val topRightBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_top_right) }
    private val bottomLeftBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_bottom_left) }
    private val bottomRightBmp: Bitmap? by lazy { decodeRes(R.drawable.bg_bottom_right) }

    // Colorful circle interpolator: cubic-bezier(.01,.99,.28,.99)
    private val colorfulInterpolator = PathInterpolator(0.01f, 0.99f, 0.28f, 0.99f)

    // Animation state
    private var animator: ValueAnimator? = null
    private var startTimeMs = 0L
    private var pauseTimeMs = 0L
    private var totalPausedMs = 0L
    private var isAnimationPaused = false

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.MaimaiDxBackgroundView)
            showCorners =
                typedArray.getBoolean(R.styleable.MaimaiDxBackgroundView_showCorners, true)
            typedArray.recycle()
        }

        patternPaint.alpha = (255 * 0.42f).toInt()
        initAnimator()
    }

    private fun decodeRes(resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(resources, resId)
        } catch (e: Throwable) {
            null
        }
    }

    private fun initAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // Radial Gradient seamlessly filling the entire screen edge-to-edge:
        // radial-gradient(circle, #ffbcd7 27%, #fc9ccc 35%, #ff97ef 56%, #bf9cff 77%, #55e5fd 98%)
        val cx = w / 2f
        val cy = h / 2f
        val radius = hypot(w / 2.0, h / 2.0).toFloat() * 1.05f
        val radialColors = intArrayOf(
            0xFFFFBCD7.toInt(),
            0xFFFFBCD7.toInt(),
            0xFFFC9CCC.toInt(),
            0xFFFF97EF.toInt(),
            0xFFBF9CFF.toInt(),
            0xFF55E5FD.toInt()
        )
        val radialPositions = floatArrayOf(
            0.00f, 0.27f, 0.35f, 0.56f, 0.77f, 0.98f
        )
        radialGradient = RadialGradient(
            cx, cy, radius,
            radialColors, radialPositions,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = radialGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (startTimeMs == 0L) {
            startTimeMs = SystemClock.uptimeMillis()
        }
        val elapsed = SystemClock.uptimeMillis() - startTimeMs - totalPausedMs
        val timeSec = if (elapsed < 0) 0.0 else elapsed / 1000.0

        val cx = w / 2f
        val cy = h / 2f

        // 1. Draw borderless radial gradient background covering entire view (0, 0, w, h)
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        // 2. Draw rotating background geometric pattern (rotate 500s linear infinite)
        patternBmp?.let { bmp ->
            val patternSize = max(w, h) * 1.15f
            val scale = patternSize / bmp.width
            val angle = ((timeSec / 500.0 * 360.0) % 360.0).toFloat()
            drawMatrix.reset()
            drawMatrix.postTranslate(-bmp.width / 2f, -bmp.height / 2f)
            drawMatrix.postScale(scale, scale)
            drawMatrix.postRotate(angle)
            drawMatrix.postTranslate(cx, cy)
            canvas.drawBitmap(bmp, drawMatrix, patternPaint)
        }

        // 3. Draw floating tiles & stars (riseAndFade animation, full width coverage)
        drawFloatingElements(canvas, w, cy, timeSec)

        // 4. Draw center concentric circles
        drawCenterCircles(canvas, w, cx, cy, timeSec)

        // 5. Draw 3D Orbiting Items (group rotate 70s linear infinite)
        drawOrbitingItems(canvas, w, cx, cy, timeSec)

        // 6. Draw the 4 fixed screen corners (only if explicitly enabled)
        if (showCorners) {
            drawCorners(canvas, w, h)
        }
    }

    /**
     * Floating tiles and stars with CSS riseAndFade animation.
     * Elements with normal orientation (scale: 1) move UPWARDS (rising).
     * Elements with inverted orientation (scale: 1 -1 in CSS) move DOWNWARDS (falling shooting stars).
     *
     * In CSS:
     * riseAndFade keyframes: 0% translateY(150%), 10% opacity 1, 90% opacity 1, 100% translateY(-150%).
     * For elements with scale: 1 -1, the Y-axis inversion inverts the displacement vector,
     * so translateY(150%) is at the top (-150% in screen coordinates) and moves down to +150% (bottom).
     */
    private fun drawFloatingElements(canvas: Canvas, w: Float, cy: Float, timeSec: Double) {
        // 1. Green tile: right side, rises UP
        drawFloatingItem(
            canvas,
            tileGreenBmp,
            0.20f * w,
            0.75f * w,
            0.75f * w,
            cy - 0.20f * w,
            12f,
            0f,
            false,
            timeSec
        )

        // 2. Purple tile left: left side, rises UP
        drawFloatingItem(
            canvas,
            tilePurpleLeftBmp,
            0.175f * w,
            0.625f * w,
            0.0375f * w,
            cy - 0.30f * w,
            15f,
            3f,
            false,
            timeSec
        )

        // 3. Purple tile right: right side, rises UP
        drawFloatingItem(
            canvas,
            tilePurpleRightBmp,
            0.125f * w,
            0.375f * w,
            0.675f * w,
            cy - 0.375f * w,
            10f,
            1.5f,
            false,
            timeSec
        )

        // 4. Pink star left: left side, falls DOWN (scale: 1 -1 in CSS)
        drawFloatingItem(
            canvas,
            starPinkBmp,
            0.08f * w,
            0.26f * w,
            0.12f * w,
            cy - 0.10f * w,
            6f,
            4f,
            true,
            timeSec
        )

        // 5. Pink star right: right side, falls DOWN (scale: 1 -1 in CSS)
        drawFloatingItem(
            canvas,
            starPinkBmp,
            0.125f * w,
            0.375f * w,
            0.55f * w,
            cy - 0.34f * w,
            8f,
            4f,
            true,
            timeSec
        )

        // 6. Yellow star left: left-center, rises UP
        drawFloatingItem(
            canvas,
            starYellowBmp,
            0.0925f * w,
            0.275f * w,
            0.295f * w,
            cy + 0.375f * w,
            7f,
            0.5f,
            false,
            timeSec
        )

        // 7. Yellow star right: right-center, rises UP
        drawFloatingItem(
            canvas,
            starYellowBmp,
            0.075f * w,
            0.25f * w,
            0.90f * w,
            cy + 0.125f * w,
            10f,
            5f,
            false,
            timeSec
        )
    }

    private fun drawFloatingItem(
        canvas: Canvas,
        bmp: Bitmap?,
        itemW: Float,
        itemH: Float,
        baseX: Float,
        baseY: Float,
        duration: Float,
        delay: Float,
        moveDownward: Boolean,
        timeSec: Double
    ) {
        if (bmp == null) return
        val t = ((timeSec + delay) % duration).toFloat()
        val progress = t / duration

        // If moveDownward (scale: 1 -1 in CSS), starts from top (-1.5 * itemH) and moves down to bottom (+1.5 * itemH).
        // If moving upward, starts from bottom (+1.5 * itemH) and moves up to top (-1.5 * itemH).
        val currentY = if (moveDownward) {
            baseY + (-1.5f + 3.0f * progress) * itemH
        } else {
            baseY + (1.5f - 3.0f * progress) * itemH
        }

        val alpha = when {
            progress < 0.10f -> progress / 0.10f
            progress > 0.90f -> (1.0f - progress) / 0.10f
            else -> 1.0f
        }

        itemPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)

        val itemCx = baseX + itemW / 2f
        val itemCy = currentY + itemH / 2f

        canvas.save()
        canvas.translate(itemCx, itemCy)
        if (moveDownward) {
            // Inverted orientation: star head points downward in direction of travel, tail trails upward
            canvas.scale(1f, -1f)
        }
        drawRect.set(-itemW / 2f, -itemH / 2f, itemW / 2f, itemH / 2f)
        canvas.drawBitmap(bmp, null, drawRect, itemPaint)
        canvas.restore()
    }

    /**
     * Center concentric circles:
     * - Yellow glow (static)
     * - Colorful rotating ring (counter-clockwise 100s with cubic-bezier easing)
     * - White decorative ring (counter-clockwise 110s linear)
     */
    private fun drawCenterCircles(canvas: Canvas, w: Float, cx: Float, cy: Float, timeSec: Double) {
        // Yellow center glow
        circleYellowBmp?.let { bmp ->
            val circleW = 1.0f * w
            val circleH = 0.5f * w
            drawRect.set(cx - circleW / 2f, cy - circleH / 2f, cx + circleW / 2f, cy + circleH / 2f)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        }

        // Colorful ring (rotateReverse 100s cubic-bezier(.01,.99,.28,.99) infinite)
        circleColorfulBmp?.let { bmp ->
            val circleSize = 1.165f * w
            val cycle = ((timeSec % 100.0) / 100.0).toFloat()
            val eased = colorfulInterpolator.getInterpolation(cycle)
            val angle = -(eased * 360f)

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(angle)
            drawRect.set(-circleSize / 2f, -circleSize / 2f, circleSize / 2f, circleSize / 2f)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
            canvas.restore()
        }

        // White ring (rotateReverse 110s linear infinite)
        circleWhiteBmp?.let { bmp ->
            val circleSize = 0.885f * w
            val angle = -((timeSec / 110.0 * 360.0) % 360.0).toFloat()

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(angle)
            drawRect.set(-circleSize / 2f, -circleSize / 2f, circleSize / 2f, circleSize / 2f)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
            canvas.restore()
        }
    }

    /**
     * 3D Orbiting Items:
     * Group rotates clockwise at 70s period around (cx, cy).
     * Inside group, each item has its own self-rotation.
     */
    private fun drawOrbitingItems(canvas: Canvas, w: Float, cx: Float, cy: Float, timeSec: Double) {
        val groupAngle = ((timeSec / 70.0 * 360.0) % 360.0).toFloat()

        canvas.save()
        canvas.rotate(groupAngle, cx, cy)

        // 1. 3D cube small (18s clockwise)
        drawOrbitItem(
            canvas,
            cubeBmp,
            0.0875f * w,
            0.0875f * w,
            0.075f * w,
            0.25f * w,
            w,
            cx,
            cy,
            18f,
            1f,
            timeSec
        )

        // 2. 3D cube (25s counter-clockwise)
        drawOrbitItem(
            canvas,
            cubeBmp,
            0.1125f * w,
            0.10f * w,
            0.7875f * w,
            0.625f * w,
            w,
            cx,
            cy,
            25f,
            -1f,
            timeSec
        )

        // 3. 3D star small (15s clockwise)
        drawOrbitItem(
            canvas,
            starSmallBmp,
            0.0375f * w,
            0.045f * w,
            0.5125f * w,
            0.35f * w,
            w,
            cx,
            cy,
            15f,
            1f,
            timeSec
        )

        // 4. 3D stars (28s clockwise)
        drawOrbitItem(
            canvas,
            starsBmp,
            0.10f * w,
            0.0875f * w,
            0.675f * w,
            1.20f * w,
            w,
            cx,
            cy,
            28f,
            1f,
            timeSec
        )

        // 5. 3D glove blue (20s counter-clockwise)
        drawOrbitItem(
            canvas,
            gloveBlueBmp,
            0.075f * w,
            0.08f * w,
            0.625f * w,
            0.15f * w,
            w,
            cx,
            cy,
            20f,
            -1f,
            timeSec
        )

        // 6. 3D glove pink (16s counter-clockwise)
        drawOrbitItem(
            canvas,
            glovePinkBmp,
            0.14f * w,
            0.165f * w,
            0.50f * w,
            0.10f * w,
            w,
            cx,
            cy,
            16f,
            -1f,
            timeSec
        )

        // 7. 3D circle pink (30s counter-clockwise)
        drawOrbitItem(
            canvas,
            circlePinkBmp,
            0.125f * w,
            0.125f * w,
            0.20f * w,
            0.75f * w,
            w,
            cx,
            cy,
            30f,
            -1f,
            timeSec
        )

        // 8. 3D circle orange (30s clockwise)
        drawOrbitItem(
            canvas,
            circleOrangeBmp,
            0.125f * w,
            0.125f * w,
            0.675f * w,
            1.00f * w,
            w,
            cx,
            cy,
            30f,
            1f,
            timeSec
        )

        canvas.restore()
    }

    private fun drawOrbitItem(
        canvas: Canvas,
        bmp: Bitmap?,
        itemW: Float,
        itemH: Float,
        left: Float,
        top: Float,
        w: Float,
        cx: Float,
        cy: Float,
        selfDuration: Float,
        direction: Float,
        timeSec: Double
    ) {
        if (bmp == null) return

        // Position relative to group center (cx, cy)
        val dx = (left + itemW / 2f) - cx
        val dy = (top + itemH / 2f) - 0.5f * w

        val selfAngle = ((timeSec / selfDuration * 360.0 * direction) % 360.0).toFloat()

        canvas.save()
        canvas.translate(cx + dx, cy + dy)
        canvas.rotate(selfAngle)
        drawRect.set(-itemW / 2f, -itemH / 2f, itemW / 2f, itemH / 2f)
        canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        canvas.restore()
    }

    /**
     * Four fixed cabinet corners (Web mobile layout <769px):
     * - top-right: 50vw wide, right: 0, top: 0, z-index: 0 (drawn first)
     * - top-left: 100% wide, left: 0, top: 0, z-index: 1 (drawn over top-right)
     * - bottom-left: 61.25vw wide, left: 0, bottom: 0, z-index: 0 (drawn first)
     * - bottom-right: 100% wide, right: 0, bottom: 0, z-index: 1 (drawn over bottom-left)
     */
    private fun drawCorners(canvas: Canvas, w: Float, h: Float) {
        // 1. Top Right (z-index: 0, 50vw wide, top: 0, right: 0)
        topRightBmp?.let { bmp ->
            val cornerW = 0.50f * w
            val cornerH = cornerW * (bmp.height.toFloat() / bmp.width.toFloat())
            drawRect.set(w - cornerW, 0f, w, cornerH)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        }

        // 2. Top Left (z-index: 1, 100% wide, top: 0, left: 0)
        topLeftBmp?.let { bmp ->
            val cornerW = 1.00f * w
            val cornerH = cornerW * (bmp.height.toFloat() / bmp.width.toFloat())
            drawRect.set(0f, 0f, cornerW, cornerH)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        }

        // 3. Bottom Left (z-index: 0, 61.25vw wide, bottom: 0, left: 0)
        bottomLeftBmp?.let { bmp ->
            val cornerW = 0.6125f * w
            val cornerH = cornerW * (bmp.height.toFloat() / bmp.width.toFloat())
            drawRect.set(0f, h - cornerH, cornerW, h)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        }

        // 4. Bottom Right (z-index: 1, 100% wide, bottom: 0, right: 0)
        bottomRightBmp?.let { bmp ->
            val cornerW = 1.00f * w
            val cornerH = cornerW * (bmp.height.toFloat() / bmp.width.toFloat())
            drawRect.set(0f, h - cornerH, cornerW, h)
            canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isAnimationPaused && visibility == VISIBLE) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && !isAnimationPaused) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && !isAnimationPaused) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    fun pauseAnimation() {
        if (!isAnimationPaused) {
            isAnimationPaused = true
            pauseTimeMs = SystemClock.uptimeMillis()
            stopAnimation()
        }
    }

    fun resumeAnimation() {
        if (isAnimationPaused) {
            isAnimationPaused = false
            if (pauseTimeMs > 0L) {
                totalPausedMs += (SystemClock.uptimeMillis() - pauseTimeMs)
                pauseTimeMs = 0L
            }
            if (isAttachedToWindow && visibility == VISIBLE) {
                startAnimation()
            }
        }
    }

    private fun startAnimation() {
        if (animator?.isRunning != true) {
            animator?.start()
        }
    }

    private fun stopAnimation() {
        if (animator?.isRunning == true) {
            animator?.cancel()
        }
    }
}
