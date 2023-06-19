@file:Suppress("unused")

package cn.wj.android.cashbook.core.common.tools

import java.util.regex.Pattern


/**
 * 判断字符序列是否匹配正则表达式
 *
 * @param regex 正则表达式
 *
 * @return 匹配正则表达式：true 不匹配正则表达式：false
 */
fun CharSequence?.isMatch(regex: String): Boolean {
    return !isNullOrBlank() && Pattern.matches(regex, this)
}