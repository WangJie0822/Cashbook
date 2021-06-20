plugins {
    // Android 应用
    id("com.android.application")
    // Android Kotlin 支持
    kotlin("android")
    // Kotlin 注解处理
    kotlin("kapt")
    // Kotlin json 转换
    kotlin("plugin.serialization") version Dependencies.Kotlin.version
    // Kotlin Parcelize 序列化
    id("kotlin-parcelize")
}

android {

    // 编译 SDK 版本
    compileSdkVersion(AppConfigs.compileSdkVersion)
    // 编译工具版本
    buildToolsVersion(AppConfigs.buildToolsVersion)

    defaultConfig {
        // 应用 id
        applicationId = "cn.wj.android.cashbook"

        // 最低支持版本
        minSdkVersion(AppConfigs.minSdkVersion)
        // 目标 SDK 版本
        targetSdkVersion(AppConfigs.targetSdkVersion)

        // 应用版本号
        versionCode = AppConfigs.versionCode
        // 应用版本名
        versionName = AppConfigs.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 开启 Dex 分包
        multiDexEnabled = true
    }

    signingConfigs {
        // 签名配置
        getByName("debug") {
            keyAlias = SigningConfigs.keyAlias
            keyPassword = SigningConfigs.keyPassword
            storeFile = file(SigningConfigs.storeFile)
            storePassword = SigningConfigs.storePassword
        }
        create("release") {
            keyAlias = SigningConfigs.keyAlias
            keyPassword = SigningConfigs.keyPassword
            storeFile = file(SigningConfigs.storeFile)
            storePassword = SigningConfigs.storePassword
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            // 应用包名后缀
            applicationIdSuffix = ".debug"
            // 版本名后缀
            versionNameSuffix = "_debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            // 应用包名后缀
            applicationIdSuffix = ""
            // 版本名后缀
            versionNameSuffix = "_release"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // 源文件路径设置
    sourceSets {
        named("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }

    buildFeatures {
        // DataBinding 开启
        dataBinding = true
    }

    lintOptions {
        // 出现错误不终止编译
        isAbortOnError = false
    }

    // 配置 APK 输出路径
    applicationVariants.all {
        if (buildType.name == "release") {
            packageApplicationProvider?.get()?.outputDirectory?.set(
                File(
                    project.rootDir,
                    "/outputs/apk"
                )
            )
            outputs.all {
                if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                    this.outputFileName = "Cashbook_${versionName}.apk"
                }
            }
        }
    }

    // Java 版本配置
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // kotlin Jvm 版本
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs =
            freeCompilerArgs + arrayOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
    }
}

dependencies {

    // Kotlin
    implementation(Dependencies.Kotlin.stdlib)
    // 协程
    implementation(Dependencies.Kotlinx.coroutines)
    // Json 序列化
    implementation(Dependencies.Kotlinx.serialization)

    // Dex 分包
    implementation(Dependencies.Androidx.multidex)

    // v4
    implementation(Dependencies.Androidx.legacy)
    // v7
    implementation(Dependencies.Androidx.appcompat)
    // RecyclerView
    implementation(Dependencies.Androidx.recyclerview)
    // 约束性布局
    implementation(Dependencies.Androidx.constraint)

    // activity
    implementation(Dependencies.Androidx.Activity.ktx)
    // fragment
    implementation(Dependencies.Androidx.Fragment.ktx)

    // core-ktx
    implementation(Dependencies.Androidx.Core.ktx)

    // LifeCycle 拓展
    implementation(Dependencies.Androidx.Lifecycle.runtimeKtx)
    implementation(Dependencies.Androidx.Lifecycle.extensions)
    // ViewModel 拓展
    implementation(Dependencies.Androidx.Lifecycle.viewModelKtx)
    // LiveData 拓展
    implementation(Dependencies.Androidx.Lifecycle.liveDataKtx)

    // Room
    implementation(Dependencies.Androidx.Room.common)
    implementation(Dependencies.Androidx.Room.ktx)
    kapt(Dependencies.Androidx.Room.compiler)

    // Navigation
    implementation(Dependencies.Androidx.Navigation.uiKtx)
    implementation(Dependencies.Androidx.Navigation.fragmentKtx)

    // Paging
    implementation(Dependencies.Androidx.Paging.runtimeKtx)

    // WorkManager
    implementation(Dependencies.Androidx.Work.runtimeKtx)

    // Material
    implementation(Dependencies.Google.material)

    // Logger
    implementation(Dependencies.logger)

    // LiveEventBus
    implementation(Dependencies.liveEventBus)

    // Koin
    implementation(Dependencies.Koin3.android)
    implementation(Dependencies.Koin3.androidExt)

    // OkHttp
    implementation(Dependencies.Squareup.OkHttp.okhttp)

    // Retrofit
    implementation(Dependencies.Squareup.Retrofit.retrofit)
    implementation(Dependencies.Squareup.Retrofit.converterKt)

    // Coil
    implementation(Dependencies.Coil.coil)

    // 状态栏工具
    implementation(Dependencies.ImmersionBar.immersionBar)
    implementation(Dependencies.ImmersionBar.ktx)

    // MMKV 数据存储
    implementation(Dependencies.Tencent.mmkv)

    // ARouter 路由
    implementation(Dependencies.Alibaba.ARouter.api)
    kapt(Dependencies.Alibaba.ARouter.compiler)

    // DoraemonKit
    debugImplementation(Dependencies.Didi.DoraemonKit.debug)
    releaseImplementation(Dependencies.Didi.DoraemonKit.release)

    // 测试
    testImplementation(Dependencies.testJunit)
    androidTestImplementation(Dependencies.Androidx.Test.rules)
    androidTestImplementation(Dependencies.Androidx.Test.runner)
    androidTestImplementation(Dependencies.Androidx.Test.Espresso.core)
    androidTestImplementation(Dependencies.Androidx.Test.Ext.junit)
}