@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.databinding.constants.IMG_DRAWABLE_MARK
import cn.wj.android.cashbook.databinding.constants.IMG_MIPMAP_MARK
import cn.wj.android.databinding.adapter.getIdentifier

/*
 * ImageView DataBinding 适配器
 */

/** 根据资源id [resId] 给 [iv] 加载图片 */
@BindingAdapter("android:bind_src")
fun src(iv: ImageView, @DrawableRes resId: Int?) {
    if (null != resId && 0 != resId) {
        iv.setImageResource(resId)
    }
}

/**
 * 根据资源字符串 [res] 给 [iv] 加载图片
 * > [res]: [IMG_DRAWABLE_MARK]:resId or [IMG_MIPMAP_MARK]:resId
 */
@BindingAdapter("android:bind_src")
fun setImageResource(iv: ImageView, res: String?) {
    if (null != res && res.isNotBlank()) {
        if (res.startsWith(IMG_DRAWABLE_MARK)) {
            val realRes = res.split(":")[1]
            iv.setImageResource(realRes.getIdentifier(iv.context, "drawable"))
        } else if (res.startsWith(IMG_MIPMAP_MARK)) {
            val realRes = res.split(":")[1]
            iv.setImageResource(realRes.getIdentifier(iv.context, "mipmap"))
        }
    }
}