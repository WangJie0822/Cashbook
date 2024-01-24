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

import cn.wj.android.cashbook.buildlogic.ProjectSetting
import cn.wj.android.cashbook.buildlogic.configureGradleManagedDevices
import cn.wj.android.cashbook.buildlogic.configureKotlinAndroid
import com.android.build.gradle.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 测试插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/13
 */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ProjectSetting.Plugin.PLUGIN_ANDROID_TEST)
                apply(ProjectSetting.Plugin.PLUGIN_KOTLIN_ANDROID)
            }

            extensions.configure<TestExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = ProjectSetting.Config.TARGET_SDK
                configureGradleManagedDevices(this)
            }
        }
    }
}
