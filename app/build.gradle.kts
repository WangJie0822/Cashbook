import cn.wj.android.cashbook.buildlogic.configureOutputs

plugins {
    // Android Kotlin 应用
    id("cashbook.android.application")
    // Kotlin 注解处理
    kotlin("kapt")
    // Kotlin json 转换
    kotlin("plugin.serialization")
    // Kotlin Parcelize 序列化
    id("kotlin-parcelize")
}

android {

    defaultConfig {
        // 应用 id
        applicationId = "cn.wj.android.cashbook"

        // 开启 Dex 分包
        multiDexEnabled = true
    }

    buildFeatures {
        dataBinding {
            isEnabled = true
        }
    }

    sourceSets {
        getByName("androidTest") {
            // room 测试使用资源
            assets.srcDirs("$projectDir/schemas")
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

    lint {
        // 出现错误不终止编译
        abortOnError = false
    }
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espressoCore)
    androidTestImplementation(libs.androidx.test.ext.junit)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    // 协程
    implementation(libs.kotlinx.coroutines.android)
    // Json 序列化
    implementation(libs.kotlinx.serialization)

    // Dex 分包
    implementation(libs.androidx.multidex)

    // v4
    implementation(libs.androidx.legacy)
    // v7
    implementation(libs.androidx.appcompat)
    // RecyclerView
    implementation(libs.androidx.recyclerview)
    // 约束性布局
    implementation(libs.androidx.constraintlayout)

    // activity
    implementation(libs.androidx.activityKtx)
    // fragment
    implementation(libs.androidx.fragmentKtx)

    // core-ktx
    implementation(libs.androidx.coreKtx)

    // LifeCycle 拓展
    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.extensions)
    // ViewModel 拓展
    implementation(libs.androidx.lifecycle.viewmodelKtx)
    // LiveData 拓展
    implementation(libs.androidx.lifecycle.livedataKtx)

    // Room
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtimeKtx)

    // WorkManager
    implementation(libs.androidx.work.runtimeKtx)

    // Material
    implementation(libs.google.material)

    // Logger
    implementation(libs.orhanobut.logger)

    // LiveEventBus
    implementation(libs.jeremyliao.liveEventBus)

    // Koin
    implementation(libs.insert.koin.android)

    // OkHttp
    implementation(libs.squareup.okhttp3)

    // Retrofit
    implementation(libs.squareup.retrofit2)
    implementation(libs.jakeWharton.retrofit2ConverterKotlin)

    // Coil
    implementation(libs.coil)

    // 状态栏工具
    implementation(libs.geyifeng.immersionbar)
    implementation(libs.geyifeng.immersionbarKtx)

    // MMKV 数据存储
    implementation(libs.tencent.mmkv)

    // ARouter 路由
    implementation(libs.alibaba.arouter.api)
    kapt(libs.alibaba.arouter.compiler)

    // DoraemonKit
    debugImplementation(libs.didi.doraemonkit.debug)
    releaseImplementation(libs.didi.doraemonkit.release)

    // Markdown 解析
    implementation(libs.noties.markwon)

    // HTML 解析
    implementation(libs.jsoup)

    // 日历控件
    implementation(libs.haibin.calendarview)

    // 图表控件
    implementation(libs.philJay.mpAndroidChart)
}