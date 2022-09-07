@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * 签名枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/7
 */
enum class Signing {
    Android
}

/**
 * 配置项目签名
 */
fun Project.configureSigningConfigs(
    commonExtension: BaseAppModuleExtension,
) {
    commonExtension.apply {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("signingLibs")

        signingConfigs {
            // 签名配置
            Signing.values().forEach {
                create(it.name) {
                    keyAlias = libs.findVersion("keyAlias").get().toString()
                    keyPassword = libs.findVersion("keyPassword").get().toString()
                    storeFile = file(libs.findVersion("storeFile").get().toString())
                    storePassword = libs.findVersion("storePassword").get().toString()
                }
            }
        }
    }
}