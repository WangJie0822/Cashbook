@file:Suppress("unused", "DEPRECATION")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

/**
 * 配置 APK 输出
 */
fun BaseAppModuleExtension.configureOutputs(
    outputPath: String,
    conditions: (ApplicationVariant) -> Boolean,
    rename: (ApplicationVariant, String) -> String
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