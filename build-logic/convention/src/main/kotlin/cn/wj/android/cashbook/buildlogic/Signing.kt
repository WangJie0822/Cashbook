@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * 配置项目签名
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
fun Project.configureSigning(
    commonExtension: BaseAppModuleExtension,
) {
    commonExtension.apply {

        val libs = extensions.getByType<VersionCatalogsExtension>().named("signingLibs")

        signingConfigs {
            // 签名配置
            getByName("debug") {
                keyAlias = libs.findVersion("keyAlias").get().toString()
                keyPassword = libs.findVersion("keyPassword").get().toString()
                storeFile = file(libs.findVersion("storeFile").get().toString())
                storePassword = libs.findVersion("storePassword").get().toString()
            }
            create("release") {
                keyAlias = libs.findVersion("keyAlias").get().toString()
                keyPassword = libs.findVersion("keyPassword").get().toString()
                storeFile = file(libs.findVersion("storeFile").get().toString())
                storePassword = libs.findVersion("storePassword").get().toString()
            }
        }

        buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.findByName("debug")
            }
            getByName("release") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.findByName("release")
            }
        }
    }
}