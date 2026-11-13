package com.paperpig.maimaidata.utils

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * 统一启用 Edge-to-Edge 透明状态栏与手势条/导航栏沉浸适配。
 * 默认配置适配「主题色顶栏 + 浅色底/白底页面」。
 */
fun AppCompatActivity.enableAppEdgeToEdge(
    lightNavigationBar: Boolean = true,
    lightStatusBar: Boolean = false
) {
    val statusBarStyle = if (lightStatusBar) {
        SystemBarStyle.light(Color.TRANSPARENT, Color.argb(0x33, 0x00, 0x00, 0x00))
    } else {
        SystemBarStyle.dark(Color.TRANSPARENT)
    }
    val navigationBarStyle = if (lightNavigationBar) {
        SystemBarStyle.light(Color.TRANSPARENT, Color.argb(0x33, 0x00, 0x00, 0x00))
    } else {
        SystemBarStyle.dark(Color.TRANSPARENT)
    }
    enableEdgeToEdge(
        statusBarStyle = statusBarStyle,
        navigationBarStyle = navigationBarStyle
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

/**
 * 为顶部栏（如 Toolbar）添加状态栏与刘海屏安全区 padding，保留初始 padding 且不消耗 bottom inset。
 */
fun View.applyTopSystemBarsPadding(includeHorizontal: Boolean = true) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = if (includeHorizontal) initialLeft + bars.left else initialLeft,
            top = initialTop + bars.top,
            right = if (includeHorizontal) initialRight + bars.right else initialRight
        )
        insets
    }
    requestApplyInsetsWhenAttached()
}

/**
 * 为底部或滚动容器添加手势条/导航栏安全区 padding，保留初始 padding。
 * @param includeIme 弹出软键盘时是否同步避让键盘高度
 * @param includeHorizontal 横屏时是否同步避让左右导航栏与刘海区
 * @param useRootInsets 当父容器（如 CollapsingToolbarLayout）已消费 insets 时，从根窗口读取完整 insets
 */
fun View.applyBottomSystemBarsPadding(
    includeIme: Boolean = false,
    includeHorizontal: Boolean = false,
    useRootInsets: Boolean = false
) {
    val initialLeft = paddingLeft
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    val typeMask = WindowInsetsCompat.Type.navigationBars() or
        WindowInsetsCompat.Type.displayCutout() or
        if (includeIme) WindowInsetsCompat.Type.ime() else 0

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val effectiveInsets = if (useRootInsets) {
            ViewCompat.getRootWindowInsets(view) ?: insets
        } else {
            insets
        }
        val bars = effectiveInsets.getInsets(typeMask)
        view.updatePadding(
            left = if (includeHorizontal) initialLeft + bars.left else initialLeft,
            right = if (includeHorizontal) initialRight + bars.right else initialRight,
            bottom = initialBottom + bars.bottom
        )
        insets
    }
    requestApplyInsetsWhenAttached()
}

/**
 * 为悬浮按钮等视图添加手势条/导航栏外边距，避免被系统底部栏或圆角遮挡。
 */
fun View.applyBottomSystemBarsMargin(includeHorizontal: Boolean = true) {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val initialLeft = lp.leftMargin
    val initialRight = lp.rightMargin
    val initialBottom = lp.bottomMargin

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = if (includeHorizontal) initialLeft + bars.left else initialLeft
            rightMargin = if (includeHorizontal) initialRight + bars.right else initialRight
            bottomMargin = initialBottom + bars.bottom
        }
        insets
    }
    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
