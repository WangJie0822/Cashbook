@file:Suppress("unused")
@file:JvmName("TimeTools")

package cn.wj.android.cashbook.base.tools

import cn.wj.android.cashbook.base.ext.base.logger
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ----------------------------------------------------------------------------------------- */
/* |                                        时间相关                                        | */
/* ----------------------------------------------------------------------------------------- */

/** 默认时间格式化 */
const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss"

/** 备份名称时间格式化 */
const val DATE_FORMAT_BACKUP = "yyyyMMddHHmmss"

/** 日期时间格式化 */
const val DATE_FORMAT_DATE = "yyyy-MM-dd"

/** 日期时间格式化 */
const val DATE_FORMAT_MONTH_DAY = "MM.dd"

/** 日期时间格式化 */
const val DATE_FORMAT_YEAR_MONTH = "yyyy-MM"

/** 时间格式化 */
const val DATE_FORMAT_TIME = "HH:mm"

/** 没有秒的时间格式化 */
const val DATE_FORMAT_NO_SECONDS = "yyyy-MM-dd HH:mm"

/** 根据[format]格式化时间，[format]默认[DATE_FORMAT_DEFAULT] */
@JvmOverloads
fun <N : Number> N.dateFormat(format: String = DATE_FORMAT_DEFAULT): String {
    return try {
        SimpleDateFormat(format, Locale.getDefault()).format(this)
    } catch (e: ParseException) {
        logger().e(e, "dateFormat")
        ""
    }
}

/** 根据[format]格式化时间，[format]默认[DATE_FORMAT_DEFAULT] */
@JvmOverloads
fun Date.dateFormat(format: String = DATE_FORMAT_DEFAULT): String {
    return try {
        SimpleDateFormat(format, Locale.getDefault()).format(this)
    } catch (e: ParseException) {
        logger().e(e, "dateFormat")
        ""
    }
}

/** 根据[format]格式化时间，[format]默认[DATE_FORMAT_DEFAULT] */
@JvmOverloads
fun String.parseDate(format: String = DATE_FORMAT_DEFAULT): Date? {
    return try {
        SimpleDateFormat(format, Locale.getDefault()).parse(this)
    } catch (e: ParseException) {
        logger().e(e, "paresDate")
        null
    }
}

fun String.parseDateLong(format: String = DATE_FORMAT_DEFAULT): Long {
    return try {
        parseDate(format)?.time ?: System.currentTimeMillis()
    } catch (throwable: Throwable) {
        logger().e(throwable, "parseDate")
        System.currentTimeMillis()
    }
}

/** 将字符串时间转换为 [Long] 类型时间 */
@JvmOverloads
fun String?.toLongTime(format: String = DATE_FORMAT_DEFAULT): Long? {
    return this?.parseDate(format)?.time
}