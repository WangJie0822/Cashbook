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

import cn.wj.android.cashbook.buildlogic.CashbookBuildType
import cn.wj.android.cashbook.buildlogic.CashbookFlavor
import cn.wj.android.cashbook.buildlogic.FileSystemOperationsInjected
import cn.wj.android.cashbook.buildlogic.TEST_INSTRUMENTATION_RUNNER
import cn.wj.android.cashbook.buildlogic.configureOutputs
import com.android.build.gradle.api.ApplicationVariant
import java.io.FileWriter

plugins {
    alias(conventionLibs.plugins.cashbook.android.application)
    alias(conventionLibs.plugins.cashbook.android.application.compose)
    alias(conventionLibs.plugins.cashbook.android.application.flavors)
    alias(conventionLibs.plugins.cashbook.android.application.jacoco)
    alias(conventionLibs.plugins.cashbook.android.hilt)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.takahirom.roborazzi)
    id("jacoco")
}

android {

    namespace = "cn.wj.android.cashbook"

    defaultConfig {
        // 应用 id
        applicationId = namespace

        // 开启 Dex 分包
        multiDexEnabled = true

        testInstrumentationRunner = TEST_INSTRUMENTATION_RUNNER

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    val signingLibs = runCatching {
        extensions.getByType<VersionCatalogsExtension>().named("signingLibs")
    }.getOrNull()

    signingConfigs {
        if (null != signingLibs) {
            create("release") {
                keyAlias = signingLibs.findVersion("keyAlias").get().toString()
                keyPassword = signingLibs.findVersion("keyPassword").get().toString()
                storeFile = file(signingLibs.findVersion("storeFile").get().toString())
                storePassword = signingLibs.findVersion("storePassword").get().toString()
            }
        }
    }

    buildTypes {
        val release = getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = CashbookBuildType.Release.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (null != signingLibs) {
                signingConfigs.findByName("release")
            } else {
                signingConfigs.findByName("debug")
            }
            // Release 版本自动构建使用最新基线配置
            baselineProfile.automaticGenerationDuringBuild = true
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = CashbookBuildType.Debug.applicationIdSuffix
            signingConfig = release.signingConfig
        }
    }

    productFlavors {
        CashbookFlavor.values().forEach { flavor ->
            // 配置权限，Offline 渠道没有网络相关权限
            val manifestPlaceholdersMap = if (flavor == CashbookFlavor.Offline) {
                mapOf(
                    "PERMISSION_1" to "NO_REQUEST_1",
                    "PERMISSION_2" to "NO_REQUEST_2",
                )
            } else {
                mapOf(
                    "PERMISSION_1" to "android.permission.INTERNET",
                    "PERMISSION_2" to "android.permission.ACCESS_NETWORK_STATE",
                )
            }
            getByName(flavor.name) {
                addManifestPlaceholders(manifestPlaceholdersMap)
            }
        }
    }

    sourceSets {
        CashbookFlavor.values().forEach { flavor ->
            // 配置资源路径
            getByName(flavor.name) {
                res.srcDirs("src/channel/res_${flavor.name}")
            }
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    configGenerateReleaseFile(applicationVariants)

    val sep = org.jetbrains.kotlin.konan.file.File.Companion.separator
    configureOutputs(
        condition = { buildTypeName ->
            buildTypeName.contains("release", true)
        },
        injected = project.objects.newInstance<FileSystemOperationsInjected>(),
        toPath = "${project.rootDir}${sep}outputs${sep}apk",
        include = "**/*.apk",
        renamer = { versionName ->
            "Cashbook_${versionName}.apk"
        },
    )
}

dependencies {

    // 功能
    implementation(projects.feature.tags)
    implementation(projects.feature.types)
    implementation(projects.feature.books)
    implementation(projects.feature.assets)
    implementation(projects.feature.records)
    implementation(projects.feature.settings)

    // 架构
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.design)
    implementation(projects.core.ui)
    implementation(projects.core.data)

    // 基线配置
    baselineProfile(projects.baselineProfile)
    implementation(libs.androidx.profileinstaller)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    // 协程
    implementation(libs.kotlinx.coroutines.android)
    // Json 序列化
    implementation(libs.kotlinx.serialization.json)

    // Dex 分包
    implementation(libs.androidx.multidex)

    // Androidx 基本依赖
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.google.material)

    // 数据同步
    implementation(projects.sync.work)

    // Markdown 解析
    implementation(libs.noties.markwon)

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.material3.window.size)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.core.splashscreen)

    implementation(libs.coil.kt)

    // 测试相关
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(projects.uiTestHiltManifest)

    kspTest(libs.androidx.hilt.compiler)

//    testImplementation(projects.core.dataTest)
    testImplementation(projects.core.testing)
    testImplementation(libs.google.accompanist.testharness)
    testImplementation(libs.google.hilt.android.testing)
    testImplementation(libs.androidx.work.testing)

    testImplementation(libs.robolectric)
    testImplementation(libs.takahirom.roborazzi)

    androidTestImplementation(projects.core.testing)
//    androidTestImplementation(projects.core.dataTest)
//    androidTestImplementation(projects.core.datastoreTest)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.google.accompanist.testharness)
    androidTestImplementation(libs.google.hilt.android.testing)
}

baselineProfile {
    // 仅 Release 版本使用自动生成
    automaticGenerationDuringBuild = false
    // 不区分多渠道，合并为一个基线配置
    mergeIntoMain = true
}

dependencyGuard {
    configuration("CanaryReleaseRuntimeClasspath")
    configuration("OfflineReleaseRuntimeClasspath")
    configuration("OnlineReleaseRuntimeClasspath")
}


/** 配置 CI 构建生成 RELEASE.md */
fun Project.configGenerateReleaseFile(applicationVariants: DomainObjectSet<ApplicationVariant>) {
    applicationVariants.all {
        mergeAssetsProvider.get().doFirst {
            val buildTagName = System.getenv("BUILD_TAG_NAME")
            if (!buildTagName.isNullOrBlank()) {
                // CI 构建流程，生成 RELEASE.md
                File(rootDir, "CHANGELOG.md").readText().lines().let { list ->
                    println("> Task :${project.name}:beforeMergeAssets generate RELEASE.md")
                    val start = if (buildTagName.endsWith("_pre")) {
                        // 预发布版本，使用 [Unreleased] 作为发布说明
                        list.indexOf("## [Unreleased]")
                    } else {
                        // 正式版本，使用 tag 对应版本作为发布说明
                        list.indexOf("## [${buildTagName.drop(1)}]")
                    }
                    if (start < 0) {
                        throw RuntimeException(
                            "Release info not found, make sure file CHANGELOG.md " +
                                    "contains '## [Unreleased]' if pre or contains " +
                                    "'## [${buildTagName.drop(1)}]' if release",
                        )
                    }
                    val content = with(StringBuilder()) {
                        for (i in (start + 1) until list.size) {
                            val line = list[i]
                            if (line.startsWith("## [")) {
                                break
                            } else {
                                appendLine(line)
                            }
                        }
                        toString()
                    }
                    println("> Task :${project.name}:beforeMergeAssets generate RELEASE.md content = <$content>")
                    val releaseFile = File(rootDir, "RELEASE.md")
                    if (releaseFile.exists()) {
                        releaseFile.delete()
                    }
                    releaseFile.createNewFile()
                    FileWriter(releaseFile).use {
                        it.write(content)
                        it.flush()
                    }
                }
            }
        }
    }
}