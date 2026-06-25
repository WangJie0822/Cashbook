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
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * 配置 Android Compose
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/7
 */
@Suppress("UnstableApiUsage")
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        buildFeatures.apply {
            // 开启 compose
            compose = true
        }

        dependencies {
            // 导入 compose bom，使用 bom 管理 compose 的统一版本
            // BOM 用 api 暴露：compose 依赖以 api 导出（如 core:ui），消费方（core:data）须继承 BOM 版本约束。
            // Gradle 9 约束传播更严格，implementation 作用域的 platform 不再泄漏给消费方（AGP8.12 曾容忍）。
            val bom = libs.findLibrary("androidx-compose-bom").get()
            add("api", platform(bom))
            add("androidTestImplementation", platform(bom))
            // 导入 compose ui 工具
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }

        testOptions.apply {
            unitTests {
                // unit 测试包含 Android 资源，用于 Robolectric 测试
                isIncludeAndroidResources = true
            }
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
        fun Provider<*>.relativeToRootProject(dir: String) = flatMap {
            rootProject.layout.buildDirectory.dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)

        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
    }
}
