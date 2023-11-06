package cn.wj.android.cashbook.core.common.ext

import cn.wj.android.cashbook.core.common.Symbol

const val SCHEME_HTTPS = "https://"
const val SCHEME_HTTP = "http://"
const val SCHEME_DAVS = "davs://"
const val SCHEME_DAV = "dav://"
const val SCHEME_CONTENT = "content://"

val String.isWebUri: Boolean
    get() = startsWith(SCHEME_HTTPS) || startsWith(SCHEME_HTTP)

val String.isContentUri: Boolean
    get() = startsWith(SCHEME_CONTENT)

/** 给字符串添加上 CNY 符号 */
fun String.withCNY(): String {
    val source = if (this.contains(Symbol.CNY)) {
        this.replace(Symbol.CNY, "")
    } else {
        this
    }
    val negative = source.startsWith("-")
    return if (negative) {
        "-${Symbol.CNY}${source.replace("-", "")}"
    } else {
        "${Symbol.CNY}$source"
    }
}