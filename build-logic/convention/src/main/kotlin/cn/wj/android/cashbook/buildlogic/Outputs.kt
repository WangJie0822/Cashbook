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
 * 将满足 [condition]（参数 `buildTypeName`）的变体编译生成的 **apk**，通过 [renamer]（参数 `versionName`、`flavorName`）重命名后复制到 [toPath]。
 *
 * `flavorName` 取自 `variant.flavorName`（多渠道枚举常量名，如 `Online`），用于在命名中区分渠道：
 * AGP9 `variant.outputs[].versionName` 不含 flavor 后缀，省略渠道段会致 online/offline 同名互相覆盖。
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
 *         renamer = { versionName, flavorName ->
 *             buildReleaseApkName(versionName, flavorName)
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
    renamer: (versionName: String, flavorName: String) -> String,
) {
    onVariants { variant ->
        val buildTypeName = variant.buildType ?: return@onVariants
        if (!condition(buildTypeName)) return@onVariants
        val apkDir = variant.artifacts.get(SingleArtifact.APK)
        val versionNameProvider = variant.outputs.firstOrNull()?.versionName
        // config-cache：配置阶段捕获 flavorName 为局部 String，rename 闭包（执行阶段）只用捕获值，不访问 variant/project
        val flavorName = variant.flavorName.orEmpty()
        val capName = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = project.tasks.register<Copy>("copy${capName}ApkToOutputs") {
            from(apkDir) {
                include(include)
            }
            into(toPath)
            // 假设每 variant 单 APK（项目无 ABI/density splits）：rename 忽略原文件名、对目录内每个 .apk 返回同名。
            // 若将来引入 splits，须改为基于原文件名派生，否则同 variant 多 APK 会被重命名为同名而互相覆盖。
            rename { renamer(versionNameProvider?.orNull ?: "", flavorName) }
        }
        // onVariants 回调时 assemble<Variant> 任务尚未创建，用 configureEach 懒式挂 finalizedBy（对未来创建的任务生效）
        project.tasks.configureEach {
            if (name == "assemble$capName") {
                finalizedBy(copyTask)
            }
        }
    }
}

/**
 * 构造 release APK 文件名 `Cashbook_<versionName>_<flavor>.apk`。
 *
 * flavor 段强制小写：AGP `variant.flavorName` 返回多渠道枚举常量名（首字母大写 `Online`/`Offline`/`Canary`/`Dev`），
 * 而历史发布命名与应用内"检查更新"（SettingRepositoryImpl.syncLatestVersion 的大小写敏感 `contains("_online")` 匹配）
 * 均按小写约定——渠道段一旦大写（`_Online`）会让 in-app 更新静默拉不到对应渠道 APK。
 *
 * AGP9 新 Variant API 的 `variant.outputs[].versionName` 不含 flavor 的 versionNameSuffix，
 * 故 online/offline 两渠道若仅按 versionName 命名会同名互相覆盖（2 个 APK 退化为 1 个）；
 * 在此显式拼入 flavor 段消除冲突，恢复每渠道独立产物。
 *
 * 不变式：渠道段（`flavorName.lowercase()`）须与 `CashbookFlavor.versionNameSuffix`（去前导下划线后）一致；
 * 新增 flavor 时若其枚举名小写 ≠ versionNameSuffix，须同步核对此处与 in-app 更新匹配字面量。
 */
fun buildReleaseApkName(versionName: String, flavorName: String): String =
    "Cashbook_${versionName}_${flavorName.lowercase()}.apk"
