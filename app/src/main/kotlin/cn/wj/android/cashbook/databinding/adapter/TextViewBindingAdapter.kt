@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.graphics.Paint
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.tools.parseColorFromString
import cn.wj.android.cashbook.base.tools.parseHtmlFromString

/*
 * TextView DataBinding 适配器
 */

/** 给 [tv] 设置文本变化监听 [before] & [on] & [after] */
@BindingAdapter(
    "android:bind_tv_textChange_before",
    "android:bind_tv_textChange_on",
    "android:bind_tv_textChange_after",
    requireAll = false
)
fun addTextChangedListener(
    tv: TextView,
    before: ((CharSequence?, Int, Int, Int) -> Unit)?,
    on: ((CharSequence?, Int, Int, Int) -> Unit)?,
    after: ((Editable?) -> Unit)?
) {
    tv.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            before?.invoke(s, start, count, after)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            on?.invoke(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            after?.invoke(s)
        }
    })
}

/** 根据颜色值 [color] 给 [tv] 设置文本颜色，颜色值 `0x000000` */
@BindingAdapter("android:bind_tv_textColor")
fun setTextColor(tv: TextView, @ColorInt color: Int?) {
    if (null == color) {
        return
    }
    tv.setTextColor(color)
}

/** 根据颜色字符串 [colorStr] 给 [tv] 设置文本颜色，颜色值 `"#FFFFFF"` */
@BindingAdapter("android:bind_tv_textColor")
fun setTextColor(tv: TextView, colorStr: String?) {
    if (colorStr.isNullOrBlank()) {
        return
    }
    val color = parseColorFromString(colorStr) ?: return
    tv.setTextColor(color)
}

/** 根据资源id [resId] 为 [tv] 设置文本 */
@BindingAdapter("android:bind_tv_text")
fun setText(tv: TextView, @StringRes resId: Int?) {
    if (null != resId || resId == 0) {
        return
    }
    tv.text = resId
}

/** 将 [tv] 文本设置为 [cs] 并执行 [textSpan] 方法块设置富文本 */
@BindingAdapter("android:bind_tv_text", "android:bind_tv_text_span", requireAll = false)
fun setText(tv: TextView, cs: CharSequence?, textSpan: (SpannableString.() -> Unit)?) {
    if (null != cs && null != textSpan) {
        tv.movementMethod = LinkMovementMethod.getInstance()
        val ss = SpannableString(cs)
        ss.textSpan()
        tv.text = ss
    } else {
        tv.text = cs
    }
}

/** 将 [html] 解析为 **Html** 格式并设置 [tv] 显示 */
@BindingAdapter("android:bind_tv_html")
fun setText(tv: TextView, html: String?) {
    tv.text = parseHtmlFromString(html.orEmpty())
}

/** 将 [tv] 的最大行数设置为 [maxLines] */
@BindingAdapter("android:bind_tv_maxLines")
fun setMaxLines(tv: TextView, maxLines: Int?) {
    if (null == maxLines) {
        return
    }
    tv.maxLines = maxLines
}

/** 将 [tv] 文本重心设置为 [gravity] */
@BindingAdapter("android:bind_tv_gravity")
fun setGravity(tv: TextView, gravity: Int?) {
    if (null == gravity) {
        return
    }
    tv.gravity = gravity
}

/** 设置 [tv] 是否显示中划线 [show] */
@BindingAdapter("android:bind_tv_show_strike_thru")
fun showStrikeThru(tv: TextView, show: Boolean?) {
    if (show.condition) {
        tv.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        tv.paintFlags = 0
    }
}