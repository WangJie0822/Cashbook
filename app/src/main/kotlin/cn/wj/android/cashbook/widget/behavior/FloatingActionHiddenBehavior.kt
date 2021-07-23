@file:Suppress("unused")

package cn.wj.android.cashbook.widget.behavior

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * [FloatingActionButton] 上滑隐藏，下滑显示
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/23
 */
class FloatingActionHiddenBehavior : FloatingActionButton.Behavior {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /** 是否正在动画 */
    private var isAnimateIng = false

    /** 是否已经显示 */
    private var isShow = true

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return (super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
                || axes == ViewCompat.SCROLL_AXIS_VERTICAL)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if ((dyConsumed > 0 || dyUnconsumed > 0) && !isAnimateIng && isShow) {
            hideFab(child, AnimateListener())
        } else if ((dyConsumed < 0 || dyUnconsumed < 0) && !isAnimateIng && !isShow) {
            showFab(child, AnimateListener())
        }
    }

    private val linearInterpolator = AccelerateDecelerateInterpolator()

    private fun showFab(view: View, vararg listener: AnimateListener?) {
        if (listener.isNotEmpty()) {
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(linearInterpolator)
                .setListener(listener[0])
                .start()
        } else {
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(linearInterpolator)
                .start()
        }
    }

    private fun hideFab(view: View, listener: AnimateListener?) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(linearInterpolator)
            .setListener(listener)
            .start()
    }

    inner class AnimateListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {
            isAnimateIng = true
            isShow = !isShow
        }

        override fun onAnimationEnd(animation: Animator) {
            isAnimateIng = false
        }

        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    }
}