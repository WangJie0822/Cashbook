@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.databinding.constants.IMG_RESOURCE_MARK
import cn.wj.android.cashbook.databinding.constants.IMG_RESOURCE_SPLIT
import cn.wj.android.databinding.adapter.getIdentifier
import coil.load
import java.io.File

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
 * > [res]: @drawable/xxx or @mipmap/xxx
 */
@BindingAdapter("android:bind_src")
fun setImageResource(iv: ImageView, res: String?) {
    if (res.isNullOrBlank() || !res.startsWith(IMG_RESOURCE_MARK)) {
        // 资源为空或者不以资源标识开头
        return
    }
    val params = res.replace(IMG_RESOURCE_MARK, "").split(IMG_RESOURCE_SPLIT)
    if (params.size != 2) {
        // 非有效格式
        return
    }
    iv.setImageResource(params[1].getIdentifier(iv.context, params[0]))
}


/**
 * 为 [iv] 设置网络图片 [imgUrl] 显示，占位图为 [placeholder]
 * > 当 [imgUrl] 为空或者加载失败时，显示 [default]
 *
 * > [placeholder] [default] 可使用资源类型 android:bind_params="@{@drawable/app_drawable_id}"
 */
@BindingAdapter(
    "android:bind_iv_img_url",
    "android:bind_iv_img_placeholder",
    "android:bind_iv_img_default",
    requireAll = false
)
fun setImageViewUrl(
    iv: ImageView,
    imgUrl: String?,
    placeholder: Drawable?,
    default: Drawable?
) {
    iv.load(imgUrl) {
        if (null != placeholder) {
            placeholder(placeholder)
        }
        if (null != default) {
            error(default)
        }
    }
}

/**
 * 为 [iv] 设置本地图片 [path] 显示，占位图为 [placeholder]
 * > 当 [path] 为空或者加载失败时，显示 [default]
 *
 * > [placeholder] [default] 可使用资源类型 android:bind_params="@{@drawable/app_drawable_id}"
 */
@BindingAdapter(
    "android:bind_iv_img_path",
    "android:bind_iv_img_placeholder",
    "android:bind_iv_img_default",
    requireAll = false
)
fun setImageViewPath(
    iv: ImageView,
    path: String?,
    placeholder: Drawable?,
    default: Drawable?
) {

    if (path.isNullOrBlank()) {
        if (null != default) {
            iv.setImageDrawable(default)
        }
        return
    }
    iv.load(File(path)) {
        if (null != placeholder) {
            placeholder(placeholder)
        }
        if (null != default) {
            error(default)
        }
    }
}

/**
 * 为 [iv] 设置图片 [img] 显示，占位图为 [placeholder]
 * > 根据 [img] 数据类型加载不同类型数据
 *
 * > [placeholder] [default] 可使用资源类型 android:bind_params="@{@drawable/app_drawable_id}"
 *
 * @see setImageViewUrl
 * @see setImageViewPath
 * @see setImageResource
 */
@BindingAdapter(
    "android:bind_iv_img",
    "android:bind_iv_img_placeholder",
    "android:bind_iv_img_default",
    requireAll = false
)
fun setImageViewImg(
    iv: ImageView,
    img: String?,
    placeholder: Drawable?,
    default: Drawable?
) {
    if (img.isNullOrBlank()) {
        if (null != default) {
            iv.setImageDrawable(default)
        }
        return
    }
    if (img.startsWith("http:") || img.contains("https:")) {
        // url
        setImageViewUrl(iv, img, placeholder, default)
    } else if (img.startsWith(IMG_RESOURCE_MARK)) {
        // Resource
        setImageResource(iv, img)
    } else {
        // path
        setImageViewPath(iv, img, placeholder, default)
    }
}