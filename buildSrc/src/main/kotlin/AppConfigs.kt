@file:Suppress("MemberVisibilityCanBePrivate")

import java.text.SimpleDateFormat
import java.util.*

/**
 * 应用配置
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/10
 */
object AppConfigs {

    /** 编译 SDK 版本 */
    const val compileSdkVersion = 30

    /** 编译工具版本 */
    const val buildToolsVersion = "30.0.3"

    /** 最小支持版本 */
    const val minSdkVersion = 21

    /** 目标支持版本 */
    const val targetSdkVersion = 30

    /** 应用版本号 */
    val versionCode = formatDate.toIntOrNull() ?: 10001

    /** 应用版本名 */
    val versionName = "$versionCode"
}

/** 格式化后的日期字符串，用于生成应用版本号 */
internal val formatDate: String
    get() {
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }