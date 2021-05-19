@file:JvmName("LoggerTools")

package cn.wj.android.cashbook.base.tools

import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer

/**
 * 日志打印相关
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/4/9
 */

/** 使用临时 [tag] 获取 [Printer] 对象打印日志 */
fun funLogger(tag: String? = null): Printer {
    return Logger.t(tag)
}