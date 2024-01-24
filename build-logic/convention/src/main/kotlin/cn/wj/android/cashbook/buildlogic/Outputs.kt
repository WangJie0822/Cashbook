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

@file:Suppress("DEPRECATION")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

/** 根据条件 [conditions] 判断，将编译生成的 apk 重命名 [rename] 之后输出到 [outputPath] */
fun BaseAppModuleExtension.configureOutputs(
    outputPath: String,
    conditions: (ApplicationVariant) -> Boolean,
    rename: (ApplicationVariant, String) -> String,
) {
    applicationVariants.all {
        if (conditions(this)) {
            assembleProvider.get().doLast {
                project.copy {
                    println("> Task :build-logic:configureOutputs start copy apk")
                    val fromDir =
                        packageApplicationProvider.get().outputDirectory.asFile.get().toString()
                    println("> Task :build-logic:configureOutputs start copy from $fromDir into $outputPath")
                    from(fromDir)
                    into(outputPath)
                    include("**/*.apk")
                    rename {
                        rename(this@all, it)
                    }
                    println("> Task :build-logic:configureOutputs copyApk finish")
                }
            }
        }
    }
}
