package com.paperpig.maimaidata.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.databinding.MmdMainStyleBgLayoutBinding
import com.paperpig.maimaidata.utils.WindowsUtils
import com.paperpig.maimaidata.utils.toDp


/**
 * 复刻官网动画
 *
 * @property layoutInflater
 */
class AnimationHelper(
    private val layoutInflater: LayoutInflater,
) {
    private lateinit var rootView: ViewGroup
    private lateinit var bind: MmdMainStyleBgLayoutBinding
    private var animationList = mutableListOf<Animator>()

    private val animatorSet: AnimatorSet by lazy {
        AnimatorSet().apply {
            playTogether(animationList)
        }
    }

    /**
     * 加载动画布局
     */
    fun loadLayout(): ViewGroup {
        bind = MmdMainStyleBgLayoutBinding.inflate(layoutInflater)
        rootView = bind.root
        loadAnime()
        return rootView
    }

    private fun loadAnime() {
        with(bind) {
            //设置云朵动画
            cloudBackLeftView.apply {
                setTansXAnimation(this, 12000L, 253.toDp())
                setAlphaAnimation(this, 12000L)
            }
            cloudBackRightView.apply {
                setTansXAnimation(this, 14000L, 185.toDp())
                setAlphaAnimation(this, 14000L)
            }
            cloudBackCenterView.apply {
                setTansXAnimation(this, 13000L, 115.toDp())
                setAlphaAnimation(this, 13000L)
            }
            cloudFrontLeftView.apply {
                setTansXAnimation(this, 18000L, 148.toDp())
                setAlphaAnimation(this, 18000L)
            }
            cloudFrontRightView.apply {
                setTansXAnimation(this, 20000L, 178.toDp())
                setAlphaAnimation(this, 20000L)
            }
            cloudFrontCenterView.apply {
                setTansXAnimation(this, 19000L, 127.toDp())
                setAlphaAnimation(this, 19000L)
            }

            //设置极光动画
            auroraBackView.apply {
                setTansXAnimation(
                    this,
                    90000L,
                    -(1010.toDp() - WindowsUtils.getWindowWidth(MaimaiDataApplication.instance))
                )
                setAlphaAnimation(
                    this, 90000L, 0.01f,
                    0.99f
                )
            }

            auroraFrontView.apply {
                setTansXAnimation(
                    this,
                    120000L,
                    1010.toDp() - WindowsUtils.getWindowWidth(MaimaiDataApplication.instance)
                )

                setAlphaAnimation(
                    this, 120000L, 0.01f,
                    0.99f
                )
            }

            //设置流星动画
            shootingStarView1.apply {
                setTansXAnimation(this, 2000L, (-77).toDp())
                setTansYAnimation(this, 2000L, 77.toDp())
                setAlphaAnimation(this, 2000L)
            }
            shootingStarView2.apply {
                setTansXAnimation(this, 2800L, (-77).toDp())
                setTansYAnimation(this, 2800L, 77.toDp())
                setAlphaAnimation(this, 2800L)
            }

            shootingStarView3.apply {
                setTansXAnimation(this, 2400L, (-154).toDp())
                setTansYAnimation(this, 2400L, 154.toDp())
                setAlphaAnimation(this, 2400L)
            }
            shootingStarView4.apply {
                setTansXAnimation(this, 3000L, (-232).toDp())
                setTansYAnimation(this, 3000L, 232.toDp())
                setAlphaAnimation(this, 3000L)
            }

            //设置闪光动画
            starYellowLeftView.apply {
                setRotationAnimation(this)
                setAlphaAnimation(this, 2000L, 0.45f, 0.55f)
            }

            starYellowRightView.apply {
                setRotationAnimation(this)
                setAlphaAnimation(this, 3000L, 0.45f, 0.55f)
            }

            starWhiteView.apply {
                setRotationAnimation(this)
                setAlphaAnimation(this, 2500L, 0.45f, 0.55f)
            }

            setFloatAnimation(charaView)
            setFloatAnimation(laundryView)
        }

    }

    /**
     * 横向平移动画
     *
     * @param view 目标view
     * @param time 动画时长
     * @param distance 平移距离
     */
    private fun setTansXAnimation(view: View, time: Long, distance: Float) {
        val translationAnimation =
            ObjectAnimator.ofFloat(view, "translationX", 0f, distance)
                .apply {
                    duration = time
                    repeatCount = ValueAnimator.INFINITE
                }

        animationList.add(translationAnimation)
    }

    /**
     * 纵向平移动画
     *
     * @param view 目标view
     * @param time 动画时长
     * @param distance 平移距离
     */
    private fun setTansYAnimation(view: View, time: Long, distance: Float) {
        val translationAnimation =
            ObjectAnimator.ofFloat(view, "translationY", 0f, distance)
                .apply {
                    duration = time
                    repeatCount = ValueAnimator.INFINITE
                }

        animationList.add(translationAnimation)
    }

    /**
     * 旋转动画
     *
     * @param view 目标view
     */
    private fun setRotationAnimation(view: View) {
        val translationAnimation =
            ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
                .apply {
                    interpolator = LinearInterpolator()
                    duration = 2000L
                    repeatCount = ValueAnimator.INFINITE
                }

        animationList.add(translationAnimation)
    }

    /**
     * 浮动动画
     *
     * @param view 目标view
     */
    private fun setFloatAnimation(view: View){
        //角色浮动动画
        val floatAnimator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            0f,
            20.toDp(),
            0f,
            (-20).toDp(),
            0f
        ).apply {
            interpolator = LinearInterpolator()
            duration = 20000L
            repeatCount = ValueAnimator.INFINITE
        }
        animationList.add(floatAnimator)
    }

    /**
     * 透明度动画
     *
     * @param view 目标view
     * @param time 动画时长
     * @param startAlphaFraction 从透明到显示时动画完成度
     * @param endAlphaFraction  从显示到透明时动画完成度
     */
    private fun setAlphaAnimation(
        view: View,
        time: Long,
        startAlphaFraction: Float = 0.2f,
        endAlphaFraction: Float = 0.8f
    ) {
        val alphaAnimation =
            ObjectAnimator.ofObject(
                view, "alpha",
                { fraction, _, _ ->
                    if (fraction < startAlphaFraction) {
                        fraction / startAlphaFraction
                    } else if (fraction > endAlphaFraction) {
                        1 - (fraction - endAlphaFraction) / startAlphaFraction
                    } else 1f
                }, 0f, 1f
            ).apply {
                duration = time
                repeatCount = ValueAnimator.INFINITE
            }

        animationList.add(alphaAnimation)
    }

    fun startAnimation() {
        animatorSet.start()
    }

    fun resumeAnimation() {
        if (animatorSet.isPaused) {
            animatorSet.resume()
            bind.tilingImageview.resumeAnimation()
            bind.shootingStarView1.resumeAnimation()
            bind.shootingStarView2.resumeAnimation()
            bind.shootingStarView3.resumeAnimation()
            bind.shootingStarView4.resumeAnimation()
        }
    }

    fun pauseAnimation() {
        animatorSet.pause()
        bind.tilingImageview.pauseAnimation()
        bind.shootingStarView1.pauseAnimation()
        bind.shootingStarView2.pauseAnimation()
        bind.shootingStarView3.pauseAnimation()
        bind.shootingStarView4.pauseAnimation()
    }

    fun stopAnimation() {
        animatorSet.end()
        animationList.clear()
        bind.tilingImageview.pauseAnimation()
    }
}



