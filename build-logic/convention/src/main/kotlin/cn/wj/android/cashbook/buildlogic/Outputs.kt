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

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.file.FileSystemOperations
import javax.inject.Inject

interface FileSystemOperationsInjected {
    @get:Inject
    val fs: FileSystemOperations
}

/**
 * 使用 [injected]，将满足 [condition] 条件的编译生成的**apk**，通过 [renamer] 重命名后，复制到 [toPath] 路径
 * - [condition] 参数 [String] 为 `buildTypeName`，返回参数 [Boolean] 为是否满足条件
 * - [injected] 为自定义接口 [FileSystemOperationsInjected]，通过 `project.objects.newInstance<FileSystemOperationsInjected>()` 获取
 *
 * - Sample:
 * ```
 * // 配置 APK 输出路径
 * val sep = org.jetbrains.kotlin.konan.file.File.Companion.separator
 * configureOutputs(
 *     condition = { buildTypeName ->
 *         buildTypeName.contains("release", true)
 *     },
 *     injected = project.objects.newInstance<FileSystemOperationsInjected>(),
 *     toPath = "${project.rootDir}${sep}outputs${sep}app",
 *     include = "*.apk",
 *     renamer = { versionName ->
 *         "Cashbook_${versionName}.apk"
 *     }
 * )
 * ```
 */
fun BaseAppModuleExtension.configureOutputs(
    condition: (String) -> Boolean,
    injected: FileSystemOperationsInjected,
    toPath: String,
    include: String,
    renamer: (String) -> String,
) {
    applicationVariants.all {
        val buildTypeName = this@all.buildType.name
        if (condition(buildTypeName)) {
            val versionName = this.versionName
            val fromPath = packageApplicationProvider.get().outputDirectory.asFile.get().toString()
            assembleProvider.get().doLast {
                println("> Task :build-logic:configureOutputs start copy apk from $fromPath to $toPath")
                injected.fs.copy {
                    from(fromPath)
                    into(toPath)
                    include(include)
                    rename { renamer(versionName) }
                }
            }
        }
    }
}
