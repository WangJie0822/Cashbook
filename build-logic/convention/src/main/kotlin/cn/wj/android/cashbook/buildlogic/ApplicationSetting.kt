package cn.wj.android.cashbook.buildlogic

import org.gradle.api.JavaVersion

/**
 * 应用配置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/7
 */
object ApplicationSetting {

    object Config {
        /** SDK 编译版本 */
        const val COMPILE_SDK = 34

        /** SDK 最小支持版本 */
        const val MIN_SDK = 23

        /** SDK 目标版本 */
        const val TARGET_SDK = 30

        /** 大版本名 */
        private const val VERSION_NAME = "v1.0.1"

        /** 版本号，动态生成 */
        val versionCode = getVersionCodeFromVersionName()

        /** 版本名，大版本号+版本号 */
        val versionName = generateVersionName()

        /** 源码 jdk 版本 */
        val javaVersion = JavaVersion.VERSION_11

        /** 从环境变量中获取版本名，若没有则根据当前时间生成 */
        private fun generateVersionName(): String {
            val buildTagName = System.getenv("BUILD_TAG_NAME") ?: ""
            val versionName = buildTagName.ifBlank {
                "${VERSION_NAME}_${generateVersionCode()}"
            }.replace("_pre", "")
            println("> Task :build-logic:generateVersionName buildTagName = <$buildTagName>, versionName = <$versionName>")
            return versionName
        }

        /** 根据日期时间获取对应版本号 */
        private fun generateVersionCode(): Int {
            val sdf = java.text.SimpleDateFormat("yyMMddHH", java.util.Locale.CHINA)
            val formatDate = sdf.format(java.util.Date())
            val versionCode = formatDate.toIntOrNull() ?: 10001
            println("> Task :build-logic:generateVersionCode formatDate: $formatDate versionCode: $versionCode")
            return versionCode
        }

        private fun getVersionCodeFromVersionName(): Int {
            val versionCode = runCatching {
                versionName.split("_")[1].toInt()
            }.getOrElse { generateVersionCode() }
            println("> Task :build-logic:getVersionCodeFromVersionName versionCode: $versionCode")
            return versionCode
        }
    }

    object Plugin {
        const val PLUGIN_ANDROID_APPLICATION = "com.android.application"
        const val PLUGIN_ANDROID_LIBRARY = "com.android.library"
        const val PLUGIN_ANDROID_TEST = "com.android.test"
        const val PLUGIN_KOTLIN_ANDROID = "org.jetbrains.kotlin.android"
        const val PLUGIN_KOTLIN_KAPT = "org.jetbrains.kotlin.kapt"
        const val PLUGIN_KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        const val PLUGIN_JACOCO = "org.gradle.jacoco"
        const val PLUGIN_GOOGLE_KSP = "com.google.devtools.ksp"
        const val PLUGIN_GOOGLE_HILT = "dagger.hilt.android.plugin"
    }
}

val JavaVersion.version: Int
    get() = ordinal + 1