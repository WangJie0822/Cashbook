package cn.wj.android.cashbook.widget.textview

import android.graphics.drawable.Drawable
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * 修复的TextVie我包装类
 *
 * @param textView 目标 textview 对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/25
 */
class FixedTextViewWrapper(private val textView: TextView) {

    val start = FixedDrawableInfo()
    val end = FixedDrawableInfo()
    val top = FixedDrawableInfo()
    val bottom = FixedDrawableInfo()

    fun setTextViewDrawable() {
        textView.setCompoundDrawables(
            getDrawable(start),
            getDrawable(top),
            getDrawable(end),
            getDrawable(bottom)
        )

    }

    private fun getDrawable(info: FixedDrawableInfo): Drawable? {
        return info.run {
            if (show) {
                if (width > 0f && height > 0f) {
                    drawable?.setBounds(0, 0, width.roundToInt(), height.roundToInt())
                }
                drawable
            } else {
                null
            }
        }
    }
}

data class FixedDrawableInfo(
    var drawable: Drawable? = null,
    var width: Float = 0f,
    var height: Float = 0f,
    var show: Boolean = true
)

interface IFixedTextView {

    val wrapper: FixedTextViewWrapper

    /**
     * 设置 TextView 左侧图片
     *
     * @param drawable 图片
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun setTextViewDrawableStart(drawable: Drawable?, width: Float = 0f, height: Float = 0f) {
        wrapper.start.drawable = drawable
        wrapper.start.width = width
        wrapper.start.height = height
        wrapper.start.show = true
        wrapper.setTextViewDrawable()
    }

    fun showDrawableStart(show: Boolean) {
        wrapper.start.show = show
        wrapper.setTextViewDrawable()
    }

    /**
     * 设置 TextView 右侧图片
     *
     * @param drawable 图片
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun setTextViewDrawableEnd(drawable: Drawable?, width: Float = 0f, height: Float = 0f) {
        wrapper.end.drawable = drawable
        wrapper.end.width = width
        wrapper.end.height = height
        wrapper.end.show = true
        wrapper.setTextViewDrawable()
    }

    /**
     * 设置 TextView 顶部图片
     *
     * @param drawable 图片
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun setTextViewDrawableTop(drawable: Drawable?, width: Float = 0f, height: Float = 0f) {
        wrapper.top.drawable = drawable
        wrapper.top.width = width
        wrapper.top.height = height
        wrapper.top.show = true
        wrapper.setTextViewDrawable()
    }

    /**
     * 设置 TextView 下方图片
     *
     * @param drawable 图片
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun setTextViewDrawableBottom(drawable: Drawable?, width: Float = 0f, height: Float = 0f) {
        wrapper.bottom.drawable = drawable
        wrapper.bottom.width = width
        wrapper.bottom.height = height
        wrapper.bottom.show = true
        wrapper.setTextViewDrawable()
    }
}