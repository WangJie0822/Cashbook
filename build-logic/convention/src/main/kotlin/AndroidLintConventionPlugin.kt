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
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            when {
                pluginManager.hasPlugin(ProjectSetting.Plugin.PLUGIN_ANDROID_APPLICATION) ->
                    configure<ApplicationExtension> { lint(Lint::configure) }

                pluginManager.hasPlugin(ProjectSetting.Plugin.PLUGIN_ANDROID_LIBRARY) ->
                    configure<LibraryExtension> { lint(Lint::configure) }

                else -> {
                    pluginManager.apply(ProjectSetting.Plugin.PLUGIN_ANDROID_LINT)
                    configure<Lint>(Lint::configure)
                }
            }
        }
    }
}

private fun Lint.configure() {
    xmlReport = true
    checkDependencies = true
}
