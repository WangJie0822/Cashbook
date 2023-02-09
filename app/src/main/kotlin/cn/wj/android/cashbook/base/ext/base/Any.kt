@file:Suppress("unused")
@file:JvmName("AnyExt")

package cn.wj.android.cashbook.base.ext.base

import cn.wj.android.cashbook.base.tools.funLogger
import com.orhanobut.logger.Printer
import java.text.DecimalFormat

/**
 * 任意对象拓展
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/3/25
 */

/** 使用类名作为日志打印 TAG */
val Any.tag: String
    get() = javaClass.simpleName

/** 当对象为 null 时使用备用对象 [t] */
fun <T> T?.orElse(t: T): T {
    return this ?: t
}

/** 使用 [tag] 或者为空时使用 [Any.tag] 作为日志打印 TAG 获取 [Printer] 对象进行日志打印 */
fun Any.logger(tag: String? = null): Printer {
    return funLogger(tag ?: this.tag)
}

/** 如果满足 [condition] 返回 [defaultValue] 的值，否则返回当前值 */
fun <T> T.ifCondition(condition: Boolean, defaultValue: () -> T): T {
    return if (condition) {
        defaultValue.invoke()
    } else {
        this
    }
}

/** 对任意格式数字数据进行格式化并返回 [String] */
fun <T> T?.decimalFormat(pattern: String = "#.##"): String {
    return DecimalFormat(pattern).format(this?.toString()?.toBigDecimalOrNull() ?: return "")
}
