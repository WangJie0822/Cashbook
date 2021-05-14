@file:Suppress("unused")
@file:JvmName("AnyExt")

package cn.wj.android.cashbook.ext.base

import cn.wj.android.cashbook.tools.funLogger
import com.orhanobut.logger.Printer

/**
 * 任意对象拓展
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/3/25
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
