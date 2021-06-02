@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.databinding.constants.RESOURCE_MARK
import cn.wj.android.databinding.adapter.getIdentifier
import coil.load
import java.io.File

/*
 * ImageView DataBinding 适配器
 */

/** 根据资源id [resId] 给 [ImageView] 加载图片 */
@BindingAdapter("android:bind_iv_srcResId")
fun ImageView.src(@DrawableRes resId: Int?) {
    if (null != resId && 0 != resId) {
        setImageResource(resId)
    }
}

/**
 * 根据资源字符串 [res] 给 [ImageView] 加载图片
 * > [res]: @drawable/xxx or @mipmap/xxx
 */
@BindingAdapter("android:bind_iv_srcResIdStr")
fun ImageView.setImageResource(res: String?) {
    if (res.isNullOrBlank() || !res.startsWith(RESOURCE_MARK)) {
        // 资源为空或者不以资源标识开头
        return
    }
    setImageResource(res.getIdentifier(context))
}


/**
 * 为 [ImageView] 设置网络图片 [imgUrl] 显示，占位图为 [placeholder]
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
fun ImageView.setImageViewUrl(
    imgUrl: String?,
    placeholder: Drawable?,
    default: Drawable?
) {
    load(imgUrl) {
        if (null != placeholder) {
            placeholder(placeholder)
        }
        if (null != default) {
            error(default)
        }
    }
}

/**
 * 为 [ImageView] 设置本地图片 [path] 显示，占位图为 [placeholder]
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
fun ImageView.setImageViewPath(
    path: String?,
    placeholder: Drawable?,
    default: Drawable?
) {

    if (path.isNullOrBlank()) {
        if (null != default) {
            setImageDrawable(default)
        }
        return
    }
    load(File(path)) {
        if (null != placeholder) {
            placeholder(placeholder)
        }
        if (null != default) {
            error(default)
        }
    }
}

/**
 * 为 [ImageView] 设置图片 [img] 显示，占位图为 [placeholder]
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
fun ImageView.setImageViewImg(
    img: String?,
    placeholder: Drawable?,
    default: Drawable?
) {
    if (img.isNullOrBlank()) {
        if (null != default) {
            setImageDrawable(default)
        }
        return
    }
    if (img.startsWith("http:") || img.contains("https:")) {
        // url
        setImageViewUrl(img, placeholder, default)
    } else if (img.startsWith(RESOURCE_MARK)) {
        // Resource
        setImageResource(img)
    } else {
        // path
        setImageViewPath(img, placeholder, default)
    }
}