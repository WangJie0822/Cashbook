@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * 配置 Kotlin Android 应用
 *
 * - 统一版本号及 Java、Kotlin 等配置
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *>,
) {

    commonExtension.apply {

        compileSdk = ApplicationSetting.Config.compileSdk

        defaultConfig {
            minSdk = ApplicationSetting.Config.minSdk
        }

        packaging {
            resources {
                excludes.add("/META-INF/{AL2.0,LGPL2.1}")
                excludes.add("/META-INF/{DEPENDENCIES,LICENSE.md,NOTICE.md,INDEX.LIST}")
            }
        }

        compileOptions {
            sourceCompatibility = ApplicationSetting.Config.javaVersion
            targetCompatibility = ApplicationSetting.Config.javaVersion
            isCoreLibraryDesugaringEnabled = true
        }

        configureKotlin()

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            add("coreLibraryDesugaring", libs.findLibrary("android.desugarJdkLibs").get())
        }
    }
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // Up to Java 11 APIs are available through desugaring
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = ApplicationSetting.Config.javaVersion
        targetCompatibility = ApplicationSetting.Config.javaVersion
    }

    configureKotlin()
}

private fun Project.configureKotlin() {
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            // Set JVM target to [ApplicationSetting.Config.javaVersion]
            jvmTarget = ApplicationSetting.Config.javaVersion.toString()
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors = warningsAsErrors.toBoolean()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                // Enable experimental coroutines APIs, including Flow
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }
}