@file:Suppress("unused")
@file:JvmName("TextTools")

package cn.wj.android.cashbook.base.tools

import android.graphics.Color
import android.text.Html
import android.text.Spanned

/* ----------------------------------------------------------------------------------------- */
/* |                                      字符串相关                                        | */
/* ----------------------------------------------------------------------------------------- */

/** 从字符串[str]中解析[Html]返回[Spanned]对象 */
fun parseHtmlFromString(str: String): Spanned? {
    @Suppress("DEPRECATION")
    return Html.fromHtml(str)
}

/** 从字符串[str]中解析并返回颜色值[Int] */
fun parseColorFromString(str: String): Int? {
    return try {
        Color.parseColor(str)
    } catch (e: Exception) {
        funLogger("TextTools").e(e, "parseColorFromString")
        null
    }
}