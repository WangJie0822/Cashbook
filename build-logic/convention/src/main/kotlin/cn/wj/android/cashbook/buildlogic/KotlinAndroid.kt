@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * 配置 Kotlin Android 应用
 *
 * - 统一版本号及 Java、Kotlin 等配置
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: BaseAppModuleExtension,
) {
    commonExtension.apply {

        compileSdk = ApplicationSetting.Config.compileSdk

        defaultConfig {
            minSdk = ApplicationSetting.Config.minSdk
            targetSdk = ApplicationSetting.Config.targetSdk

            // 应用版本号
            versionCode = ApplicationSetting.Config.versionCode
            // 应用版本名
            versionName = ApplicationSetting.Config.versionName

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

/**
 * 配置 Kotlin Android 仓库
 *
 * - 统一版本号及 Java、Kotlin 等配置
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: LibraryExtension,
) {
    commonExtension.apply {

        compileSdk = ApplicationSetting.Config.compileSdk

        defaultConfig {
            minSdk = ApplicationSetting.Config.minSdk
            targetSdk = ApplicationSetting.Config.targetSdk

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

fun CommonExtension<*, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
