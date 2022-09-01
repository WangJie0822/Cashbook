@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * 配置 Kotlin Android 应用
 *
 * - 统一版本号
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: BaseAppModuleExtension,
) {
    commonExtension.apply {

        compileSdk = 32

        defaultConfig {
            minSdk = 21
            targetSdk = 30

            // 应用版本号
            val vCode = getVersionCode()
            versionCode = vCode
            // 应用版本名
            versionName = "v0.5.5_$vCode"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            vectorDrawables {
                // 运行时绘制向量图
                useSupportLibrary = true
            }
        }

        packagingOptions {
            resources {
                excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        kotlinOptions {
            // Treat all Kotlin warnings as errors (disabled by default)
            allWarningsAsErrors = properties["warningsAsErrors"] as? Boolean ?: false

            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                // Enable experimental coroutines APIs, including Flow
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-opt-in=kotlin.Experimental",
                // Enable experimental kotlinx serialization APIs
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            )

            // Set JVM target to 11
            jvmTarget = JavaVersion.VERSION_11.toString()
        }

        // 源文件路径设置
        sourceSets {
            getByName("main") {
                java.srcDirs("src/main/java", "src/main/kotlin")
                res.srcDirs("src/main/res")
            }
        }
    }
}

/** 根据日期时间获取对应版本号 */
fun getVersionCode(): Int {
    val sdf = java.text.SimpleDateFormat("yyMMddHH", java.util.Locale.CHINA)
    val formatDate = sdf.format(java.util.Date())
    val versionCode = formatDate.toIntOrNull() ?: 10001
    println("> Task :build-logic:KotlinAndroid getVersionCode formatDate: $formatDate versionCode: $versionCode")
    return versionCode
}

fun CommonExtension<*, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
