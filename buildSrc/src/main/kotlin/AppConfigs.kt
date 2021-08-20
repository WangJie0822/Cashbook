@file:Suppress("MemberVisibilityCanBePrivate")

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用配置
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/10
 */
object AppConfigs {

    /** 编译 SDK 版本 */
    const val compileSdk = 30

    /** 最小支持版本 */
    const val minSdk = 21

    /** 目标支持版本 */
    const val targetSdk = 30

    /** 应用 id */
    const val applicationId = "cn.wj.android.cashbook"

    /** 备份版本号 - 用于兼容性控制 */
    const val backupVersion = 1

    /** 应用版本号 */
    val versionCode = getVersionCode()

    /** 应用版本名 */
    val versionName = "v0.5.0_$versionCode"
}

/** 根据日期时间获取对应版本号 */
private fun getVersionCode(): Int {
    val sdf = SimpleDateFormat("yyMMddHH", Locale.CHINA)
    val formatDate = sdf.format(Date())
    val versionCode = formatDate.toIntOrNull() ?: 10001
    println("> Task :buildSrc:AppConfigs getVersionCode formatDate: $formatDate versionCode: $versionCode")
    return versionCode
}