/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * 配置 Kotlin Android 应用
 *
 * - 统一版本号及 Java、Kotlin 等配置
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = ProjectSetting.Config.COMPILE_SDK

        defaultConfig.apply {
            minSdk = ProjectSetting.Config.MIN_SDK
        }

        compileOptions.apply {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            sourceCompatibility = ProjectSetting.Config.javaVersion
            targetCompatibility = ProjectSetting.Config.javaVersion
            isCoreLibraryDesugaringEnabled = true
        }

        configureKotlin<KotlinAndroidProjectExtension>()

        dependencies {
            add("coreLibraryDesugaring", libs.findLibrary("android-desugarJdkLibs").get())
        }
    }

    configureTestJavaLauncher()
}

/**
 * 配置非 Android 的 JVM 项目的 Kotlin 基本配置
 */
fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // Up to Java 11 APIs are available through desugaring
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = ProjectSetting.Config.javaVersion
        targetCompatibility = ProjectSetting.Config.javaVersion
    }

    configureKotlin<KotlinJvmProjectExtension>()

    configureTestJavaLauncher()
}

/**
 * 为 Test 任务显式指定 fork JVM 版本为 [ProjectSetting.Config.TEST_JVM_VERSION]
 *
 * 不显式声明时 Test fork JVM 跟随环境隐式选择，CI 上曾被解析为 Java 17，触发 Robolectric
 * `Android SDK 36 requires Java 21 (have Java 17)` 沙箱创建失败；显式固定后不再受环境影响
 */
internal fun Project.configureTestJavaLauncher() {
    val javaToolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<Test>().configureEach {
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(
                    JavaLanguageVersion.of(ProjectSetting.Config.TEST_JVM_VERSION),
                )
            },
        )
    }
}

/**
 * Configure base Kotlin options
 */
inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Treat all Kotlin warnings as errors (disabled by default)
    // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
    val warningsAsErrors: String? by project
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> throw RuntimeException("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget.set(ProjectSetting.Config.javaVersion.target)
        allWarningsAsErrors.set(warningsAsErrors.toBoolean())
        freeCompilerArgs.add(
            // Enable experimental coroutines APIs, including Flow
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}
