@file:Suppress("unused")
@file:JvmName("IntExt")

package cn.wj.android.cashbook.base.ext.base

import android.graphics.drawable.Drawable
import cn.wj.android.cashbook.base.tools.getColorById
import cn.wj.android.cashbook.base.tools.getDrawableById
import cn.wj.android.cashbook.base.tools.getStringById

/**
 * Int 相关拓展
 *
 * - 创建时间：2019/11/15
 *
 * @author 王杰
 */

/** String 字符串 */
val Int.string: String
    get() = getStringById(this)

/** 颜色值 */
val Int.color: Int
    get() = getColorById(this)

/** 图片资源 */
val Int.drawable: Drawable?
    get() = getDrawableById(this)