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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 项目配置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/7
 */
object ProjectSetting {

    object Config {
        /** SDK 编译版本 */
        const val COMPILE_SDK = 34

        /** SDK 最小支持版本 */
        const val MIN_SDK = 24

        /** SDK 目标版本 */
        const val TARGET_SDK = 34

        /** 大版本名 */
        private const val VERSION_NAME = "v1.0.6"

        private var versionCodeTemp = -1

        /** 版本号，动态生成 */
        val versionCode: Int
            get() {
                if (versionCodeTemp == -1) {
                    versionCodeTemp = getVersionCodeFromVersionName()
                }
                return versionCodeTemp
            }

        private var versionNameTemp = ""

        /** 版本名，大版本名+版本号 */
        val versionName: String
            get() {
                if (versionNameTemp.isBlank()) {
                    versionNameTemp = generateVersionName()
                }
                return versionNameTemp
            }

        /** 源码 jdk 版本 */
        val javaVersion = JavaVersion.VERSION_11

        /** 从环境变量中获取版本名，若没有则根据当前时间生成 */
        private fun generateVersionName(): String {
            val buildTagName = System.getenv("BUILD_TAG_NAME") ?: ""
            if (buildTagName.isNotBlank()) {
                // CI 构建流程，判断 tag 名称是否合规
                val dateString = buildTagName.split("_")[1]
                runCatching {
                    SimpleDateFormat("yyMMddHH", Locale.getDefault()).run {
                        isLenient = false
                        parse(dateString)
                    }
                }.getOrElse {
                    throw RuntimeException("Tag name is off-spec, make sure it's date match 'yyMMddHH' pattern")
                }
            }
            val versionName = buildTagName.ifBlank {
                "${VERSION_NAME}_${generateVersionCode()}"
            }.replace("_pre", "")
            println("> Task :build-logic:generateVersionName buildTagName = <$buildTagName>, versionName = <$versionName>")
            return versionName
        }

        /** 根据日期时间获取对应版本号 */
        private fun generateVersionCode(): Int {
            val sdf = SimpleDateFormat("yyMMddHH", Locale.getDefault())
            val formatDate = sdf.format(Date())
            val versionCode = formatDate.toIntOrNull() ?: 10001
            println("> Task :build-logic:generateVersionCode formatDate: $formatDate versionCode: $versionCode")
            return versionCode
        }

        private fun getVersionCodeFromVersionName(): Int {
            val versionCode = runCatching {
                versionName.split("_")[1].toInt()
            }.getOrElse { generateVersionCode() }
            println("> Task :build-logic:getVersionCodeFromVersionName versionName = <$versionName>, versionCode: $versionCode")
            return versionCode
        }
    }

    object Plugin {
        const val PLUGIN_ANDROID_APPLICATION = "com.android.application"
        const val PLUGIN_ANDROID_LIBRARY = "com.android.library"
        const val PLUGIN_ANDROID_TEST = "com.android.test"
        const val PLUGIN_ANDROID_LINT = "com.android.lint"
        const val PLUGIN_ANDROIDX_ROOM = "androidx.room"
        const val PLUGIN_KOTLIN_ANDROID = "org.jetbrains.kotlin.android"
        const val PLUGIN_KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        const val PLUGIN_JACOCO = "org.gradle.jacoco"
        const val PLUGIN_GOOGLE_KSP = "com.google.devtools.ksp"
        const val PLUGIN_GOOGLE_HILT = "dagger.hilt.android.plugin"
        const val PLUGIN_TAKAHIROM_ROBORAZZI = "io.github.takahirom.roborazzi"
        const val PLUGIN_DEPENDENCY_GUARD = "com.dropbox.dependency-guard"

        const val PLUGIN_CASHBOOK_LIBRARY = "cashbook.android.library"
        const val PLUGIN_CASHBOOK_LINT = "cashbook.android.lint"
        const val PLUGIN_CASHBOOK_HILT = "cashbook.android.hilt"
    }
}

/** Android 仪器化测试 Runner */
const val TEST_INSTRUMENTATION_RUNNER = "cn.wj.android.cashbook.core.testing.CashbookTestRunner"

val JavaVersion.version: Int
    get() = ordinal + 1

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
