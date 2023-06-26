@file:Suppress("UnstableApiUsage")

import cn.wj.android.cashbook.buildlogic.configureOutputs

plugins {
    // Android Kotlin 应用
    id("cashbook.android.application")
    id("cashbook.android.application.compose")
    id("cashbook.android.application.flavors")
    id("cashbook.android.application.jacoco")
    id("cashbook.android.hilt")
    id("jacoco")
    // Kotlin 注解处理
    kotlin("kapt")
    // Kotlin json 转换
    kotlin("plugin.serialization")
    // Kotlin Parcelize 序列化
    id("kotlin-parcelize")
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

    buildFeatures.buildConfig = true

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
    implementation(project(":feature:tags"))
    implementation(project(":feature:types"))
    implementation(project(":feature:books"))
    implementation(project(":feature:assets"))
    implementation(project(":feature:records"))
    implementation(project(":feature:settings"))

    // 设计
    implementation(project(":core:design"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.material3.window.size)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.core.splashscreen)

    implementation(libs.google.accompanist.systemuicontroller)
}