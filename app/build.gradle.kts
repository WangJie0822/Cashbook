import cn.wj.android.cashbook.buildlogic.CashbookFlavor
import cn.wj.android.cashbook.buildlogic.configureOutputs

plugins {
    alias(libs.plugins.cashbook.android.application)
    alias(libs.plugins.cashbook.android.application.compose)
    alias(libs.plugins.cashbook.android.application.flavors)
    alias(libs.plugins.cashbook.android.application.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
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
        resValues = true
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
                "proguard-rules.pro"
            )
        }
    }

    productFlavors {
        @Suppress("EnumValuesSoftDeprecate")
        CashbookFlavor.values().forEach { flavor ->
            val value = if (flavor == CashbookFlavor.Dev) {
                "@string/app_name_dev"
            } else {
                "@string/app_name_online"
            }
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
                resValue("string", "app_name", value)
                addManifestPlaceholders(manifestPlaceholdersMap)
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
        })
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