@file:Suppress("unused")
@file:JvmName("NumberExt")

package cn.wj.android.cashbook.base.ext.base

import cn.wj.android.cashbook.base.tools.dip2px
import cn.wj.android.cashbook.base.tools.sp2px
import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * Number 相关拓展
 *
 * - 创建时间：2019/11/15
 *
 * @author 王杰
 */

/** 单位标记 - DP 单位 */
val Number.dp: Float
    get() = dip2px(this)

/** 单位标记 - SP */
val Number.sp: Float
    get() = sp2px(this)

/** 单位标记 - DP 单位 */
val Number.dpi: Int
    get() = dip2px(this).roundToInt()

fun String?.toBigDecimalOrZero(): BigDecimal {
    return this?.toBigDecimalOrNull() ?: "0".toBigDecimal()
}

fun Number?.toBigDecimalOrZero(): BigDecimal {
    return this?.toString().toBigDecimalOrZero()
}

fun Int.completeZero(): String {
    return if (this >= 10) {
        this.toString()
    } else {
        "0$this"
    }
}
