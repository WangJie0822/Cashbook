package cn.wj.android.cashbook.base.ext.base

import okio.use
import java.io.Closeable

/**
 * Closeable 相关拓展方法
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/29
 */

/** 在 [block] 方法块使用使用 [T] 对象之后自动关闭 */
inline fun <T : Closeable?, R> T.useWith(block: T.() -> R): R {
    return this.use(block)
}