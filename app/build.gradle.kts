import cn.wj.android.cashbook.buildlogic.CashbookFlavor
import cn.wj.android.cashbook.buildlogic.configureOutputs
import java.io.FileWriter

plugins {
    alias(conventionLibs.plugins.cashbook.android.application)
    alias(conventionLibs.plugins.cashbook.android.application.compose)
    alias(conventionLibs.plugins.cashbook.android.application.flavors)
    alias(conventionLibs.plugins.cashbook.android.application.jacoco)
    alias(conventionLibs.plugins.cashbook.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.takahirom.roborazzi)
}

android {

    namespace = "cn.wj.android.cashbook"

    defaultConfig {
        // 应用 id
        applicationId = namespace

        // 开启 Dex 分包
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = getByName("release").signingConfig
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
                val srcDir = if (flavor == CashbookFlavor.Dev) {
                    "src/channel/res_Dev"
                } else {
                    "src/channel/res"
                }
                res.srcDirs(srcDir)
            }
        }
    }

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

    // 配置 APK 输出路径
    val sep = org.jetbrains.kotlin.konan.file.File.Companion.separator
    configureOutputs(
        "${project.rootDir}${sep}outputs${sep}apk",
        { variant ->
            variant.buildType.name == "release"
        },
        { variant, _ ->
            "Cashbook_${variant.versionName}.apk"
        },
    )
}

dependencies {

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    // 协程
    implementation(libs.kotlinx.coroutines.android)
    // Json 序列化
    implementation(libs.kotlinx.serialization.json)

    // Dex 分包
    implementation(libs.androidx.multidex)

    // Androidx 基本依赖，包含 v4 v7 core-ktx activity-ktx fragment-ktx
    implementation(libs.bundles.androidx.base.ktx)

    implementation(libs.google.material)

    // 功能
    implementation(projects.feature.tags)
    implementation(projects.feature.types)
    implementation(projects.feature.books)
    implementation(projects.feature.assets)
    implementation(projects.feature.records)
    implementation(projects.feature.settings)

    // 设计
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.design)
    implementation(projects.core.ui)
    implementation(projects.core.data)

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

    debugImplementation(libs.didi.dokit.core)
    releaseImplementation(libs.didi.dokit.noop)
}