@file:Suppress("unused")
@file:JvmName("DensityTools")

package cn.wj.android.cashbook.tools

import android.content.Context
import cn.wj.android.cashbook.manager.AppManager
import kotlin.math.roundToInt

/* ----------------------------------------------------------------------------------------- */
/* |                                        密度相关                                        | */
/* ----------------------------------------------------------------------------------------- */

/** 通过 [context] 对象获取并返回 **px** 单位的设备屏幕宽度[Int] */
@JvmOverloads
fun getDeviceScreenWidth(context: Context = AppManager.getContext()): Int {
    return context.resources.displayMetrics.widthPixels
}

/** 通过 [context] 对象获取并返回 **px** 单位的设备屏幕高度[Int] */
@JvmOverloads
fun getDeviceScreenHeight(context: Context = AppManager.getContext()): Int {
    return context.resources.displayMetrics.heightPixels
}

/** 将**dp**单位的值[dpValue]转换为**px**单位的值[Float] */
@JvmOverloads
fun <N : Number> dip2px(dpValue: N, context: Context = AppManager.getContext()): Float {
    val density = context.resources.displayMetrics.density
    return dpValue.toFloat() * density
}

/** 将**px**单位的值[pxValue]转换为**dp**单位的值[Float] */
@JvmOverloads
fun <N : Number> px2dp(pxValue: N, context: Context = AppManager.getContext()): Float {
    val density = context.resources.displayMetrics.density
    return pxValue.toFloat() / density
}

/** 将**px**单位的值[pxValue]转换为**dp**单位的值[Int] */
@JvmOverloads
fun <N : Number> px2dpi(pxValue: N, context: Context = AppManager.getContext()): Int {
    return px2dp(pxValue, context).roundToInt()
}

/** 将**sp**单位的值[spValue]转换为**px**单位的值[Float] */
@JvmOverloads
fun <N : Number> sp2px(spValue: N, context: Context = AppManager.getContext()): Float {
    val density = context.resources.displayMetrics.scaledDensity
    return spValue.toFloat() * density
}

/** 将**px**单位的值[pxValue]转换为**sp**单位的值[Float] */
@JvmOverloads
fun <N : Number> px2sp(pxValue: N, context: Context = AppManager.getContext()): Float {
    val density = context.resources.displayMetrics.scaledDensity
    return pxValue.toFloat() / density
}
