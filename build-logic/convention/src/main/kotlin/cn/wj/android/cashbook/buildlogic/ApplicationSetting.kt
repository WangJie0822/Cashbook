package cn.wj.android.cashbook.buildlogic

/**
 * 应用配置数据
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2022/9/5
 */
object ApplicationSetting {

    object Config {
        const val compileSdk = 32
        const val minSdk = 21
        const val targetSdk = 30
        val versionCode = generateVersionCode()
        val versionName = "v0.5.5_$versionCode"

        /** 根据日期时间获取对应版本号 */
        private fun generateVersionCode(): Int {
            val sdf = java.text.SimpleDateFormat("yyMMddHH", java.util.Locale.CHINA)
            val formatDate = sdf.format(java.util.Date())
            val versionCode = formatDate.toIntOrNull() ?: 10001
            println("> Task :build-logic:getVersionCode formatDate: $formatDate versionCode: $versionCode")
            return versionCode
        }
    }

    object Plugin {
        const val PLUGIN_ANDROID_APPLICATION = "com.android.application"
        const val PLUGIN_ANDROID_LIBRARY = "com.android.library"
        const val PLUGIN_KOTLIN_ANDROID = "org.jetbrains.kotlin.android"
    }
}