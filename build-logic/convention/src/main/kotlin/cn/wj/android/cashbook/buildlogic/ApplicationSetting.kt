package cn.wj.android.cashbook.buildlogic

import org.gradle.api.Project

/**
 * 应用配置数据
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2022/9/5
 */
object ApplicationSetting {

    object Config {
        const val compileSdk = 33
        const val minSdk = 21
        const val targetSdk = 30
        val versionCode = generateVersionCode()
        val versionName = "v0.6.0_$versionCode"

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

/** 拓展变量数据存储集合 */
private val kvMap: HashMap<String, Any> = HashMap()

/** 标记 - 是否生成枚举类文件 */
var Project.generateFlavorFile: Boolean
    get() {
        return (kvMap["${this.name}-generateFlavorFileKey"] as? Boolean) ?: false
    }
    set(value) {
        kvMap["${this.name}-generateFlavorFileKey"] = value
    }