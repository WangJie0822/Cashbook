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

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * 签名枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/7
 */
enum class Signing {
    Android,
}

/**
 * 配置项目签名
 */
fun Project.configureSigningConfigs(
    commonExtension: ApplicationExtension,
) {
    commonExtension.apply {
        val signingLibs = runCatching {
            extensions.getByType<VersionCatalogsExtension>().named("signingLibs")
        }.getOrNull()

        if (null != signingLibs) {
            signingConfigs {
                // 签名配置
                @Suppress("EnumValuesSoftDeprecate")
                Signing.values().forEach {
                    create(it.name) {
                        keyAlias = signingLibs.findVersion("keyAlias").get().toString()
                        keyPassword = signingLibs.findVersion("keyPassword").get().toString()
                        storeFile = file(signingLibs.findVersion("storeFile").get().toString())
                        storePassword = signingLibs.findVersion("storePassword").get().toString()
                    }
                }
            }
        }
    }
}
