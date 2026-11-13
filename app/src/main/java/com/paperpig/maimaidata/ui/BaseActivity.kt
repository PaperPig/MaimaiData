package com.paperpig.maimaidata.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.paperpig.maimaidata.utils.applyBottomSystemBarsPadding
import com.paperpig.maimaidata.utils.applyTopSystemBarsPadding
import com.paperpig.maimaidata.utils.enableAppEdgeToEdge

/**
 * 全应用 Activity 基类：统一初始化 ViewBinding，并默认开启透明状态栏与手势条（Edge-to-Edge）适配。
 */
abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T
        private set

    protected open val lightNavigationBar: Boolean = true
    protected open val lightStatusBar: Boolean = false
    protected open val includeImeInBottomInset: Boolean = false
    protected open val includeHorizontalInBottomInset: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableAppEdgeToEdge(
            lightNavigationBar = lightNavigationBar,
            lightStatusBar = lightStatusBar
        )
        onBeforeSetContentView()
        binding = inflateBinding()
        setContentView(binding.root)
        applyDefaultEdgeToEdgeInsets()
    }

    protected open fun onBeforeSetContentView() = Unit

    protected abstract fun inflateBinding(): T

    /**
     * 返回需要自动适配顶部状态栏与刘海屏安全区的视图（通常是 Toolbar）。
     */
    protected open fun topInsetTargetView(): View? = null

    /**
     * 返回需要自动适配底部手势条/导航栏安全区的视图（通常是滚动容器或根布局）。
     */
    protected open fun bottomInsetTargetView(): View? = null

    /**
     * 供需要精细控制多个视图 Insets 的页面覆写（例如主页动态底栏、看图页悬浮按钮）。
     */
    protected open fun onApplyCustomEdgeToEdgeInsets() = Unit

    private fun applyDefaultEdgeToEdgeInsets() {
        topInsetTargetView()?.applyTopSystemBarsPadding()
        bottomInsetTargetView()?.applyBottomSystemBarsPadding(
            includeIme = includeImeInBottomInset,
            includeHorizontal = includeHorizontalInBottomInset
        )
        onApplyCustomEdgeToEdgeInsets()
    }
}
