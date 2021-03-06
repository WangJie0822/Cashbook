@file:Suppress("unused")

package cn.wj.android.cashbook.widget.textview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import cn.wj.android.cashbook.R

/**
 * 优化的 [AppCompatTextView]
 *
 * - 可设置 [setCompoundDrawables] 尺寸
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/3/17
 */
class FixedTextView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr),
    IFixedTextView {

    override val wrapper = FixedTextViewWrapper(this)

    init {
        if (null != attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.FixedTextView)

            // 左侧 Drawable
            val dStart = a.getDrawable(R.styleable.FixedTextView_ftv_drawableStart)
            wrapper.start.show = a.getBoolean(R.styleable.FixedTextView_ftv_showDrawableStart, true)
            if (dStart != null) {
                wrapper.start.drawable = dStart
                wrapper.start.width = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableStartWidth,
                    dStart.intrinsicWidth.toFloat()
                )
                wrapper.start.height = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableStartHeight,
                    dStart.intrinsicHeight.toFloat()
                )
            }

            // 右侧 Drawable
            val dEnd = a.getDrawable(R.styleable.FixedTextView_ftv_drawableEnd)
            wrapper.end.show = a.getBoolean(R.styleable.FixedTextView_ftv_showDrawableEnd, true)
            if (dEnd != null) {
                wrapper.end.drawable = dEnd
                wrapper.end.width = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableEndWidth,
                    dEnd.intrinsicWidth.toFloat()
                )
                wrapper.end.height = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableEndHeight,
                    dEnd.intrinsicHeight.toFloat()
                )
            }

            // 顶部 Drawable
            val dTop = a.getDrawable(R.styleable.FixedTextView_ftv_drawableTop)
            wrapper.top.show = a.getBoolean(R.styleable.FixedTextView_ftv_showDrawableTop, true)
            if (dTop != null) {
                wrapper.top.drawable = dTop
                wrapper.top.width = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableTopWidth,
                    dTop.intrinsicWidth.toFloat()
                )
                wrapper.top.height = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableTopHeight,
                    dTop.intrinsicHeight.toFloat()
                )
            }

            // 底部 Drawable
            val dBottom = a.getDrawable(R.styleable.FixedTextView_ftv_drawableBottom)
            wrapper.bottom.show =
                a.getBoolean(R.styleable.FixedTextView_ftv_showDrawableBottom, true)
            if (dBottom != null) {
                wrapper.bottom.drawable = dBottom
                wrapper.bottom.width = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableBottomWidth,
                    dBottom.intrinsicWidth.toFloat()
                )
                wrapper.bottom.height = a.getDimension(
                    R.styleable.FixedTextView_ftv_drawableBottomHeight,
                    dBottom.intrinsicHeight.toFloat()
                )
            }
            wrapper.setTextViewDrawable()

            a.recycle()
        }
    }
}