@file:Suppress("unused")
@file:JvmName("IntExt")

package cn.wj.android.cashbook.core.common.ext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import cn.wj.android.cashbook.core.common.tools.getColorById
import cn.wj.android.cashbook.core.common.tools.getColorStateListById
import cn.wj.android.cashbook.core.common.tools.getDrawableById
import cn.wj.android.cashbook.core.common.tools.getStringById

/**
 * Int 相关拓展
 *
 * - 创建时间：2019/11/15
 *
 * @author 王杰
 */

/** String 字符串 */
fun Int.string(context: Context): String = getStringById(this, context)

/** 颜色值 */
fun Int.color(context: Context): Int = getColorById(this, context)

/** 颜色值 */
fun Int.colorStateList(context: Context): ColorStateList? = getColorStateListById(this, context)

/** 图片资源 */
fun Int.drawable(context: Context): Drawable? = getDrawableById(this, context)