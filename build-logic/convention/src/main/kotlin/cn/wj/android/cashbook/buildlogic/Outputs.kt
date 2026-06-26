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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

/**
 * 将满足 [condition]（参数 `buildTypeName`）的变体编译生成的 **apk**，通过 [renamer]（参数 `versionName`）重命名后复制到 [toPath]。
 *
 * AGP 9 移除 `applicationVariants` / `BaseAppModuleExtension`，改用 `androidComponents.onVariants` + `SingleArtifact.APK` + Copy task；
 * Copy task `finalizedBy` 对应变体的 `assemble` 任务，复刻原 `assembleProvider.doLast` 的执行时机。
 *
 * - Sample:
 * ```
 * val sep = org.jetbrains.kotlin.konan.file.File.Companion.separator
 * androidComponents {
 *     configureOutputs(
 *         project = project,
 *         condition = { buildTypeName ->
 *             buildTypeName.contains("release", true)
 *         },
 *         toPath = "${project.rootDir}${sep}outputs${sep}apk",
 *         include = "*.apk",
 *         renamer = { versionName ->
 *             "Cashbook_${versionName}.apk"
 *         },
 *     )
 * }
 * ```
 */
fun ApplicationAndroidComponentsExtension.configureOutputs(
    project: Project,
    condition: (String) -> Boolean,
    toPath: String,
    include: String,
    renamer: (String) -> String,
) {
    onVariants { variant ->
        val buildTypeName = variant.buildType ?: return@onVariants
        if (!condition(buildTypeName)) return@onVariants
        val apkDir = variant.artifacts.get(SingleArtifact.APK)
        val versionNameProvider = variant.outputs.firstOrNull()?.versionName
        val capName = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = project.tasks.register<Copy>("copy${capName}ApkToOutputs") {
            from(apkDir) {
                include(include)
            }
            into(toPath)
            rename { renamer(versionNameProvider?.orNull ?: "") }
        }
        // onVariants 回调时 assemble<Variant> 任务尚未创建，用 configureEach 懒式挂 finalizedBy（对未来创建的任务生效）
        project.tasks.configureEach {
            if (name == "assemble$capName") {
                finalizedBy(copyTask)
            }
        }
    }
}
