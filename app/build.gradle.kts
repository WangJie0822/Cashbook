plugins {
    // Android 应用
    id("com.android.application")
    // Android Kotlin 支持
    kotlin("android")
    // Kotlin 注解处理
    kotlin("kapt")
    // Kotlin json 转换
    kotlin("plugin.serialization")
    // Kotlin Parcelize 序列化
    id("kotlin-parcelize")
}

android {

    // 编译 SDK 版本
    compileSdk = configLibs.versions.compileSdk.get().toInt()

    defaultConfig {
        // 应用 id
        applicationId = "cn.wj.android.cashbook"

        // 最低支持版本
        minSdk = configLibs.versions.minSdk.get().toInt()
        // 目标 SDK 版本
        targetSdk = configLibs.versions.targetSdk.get().toInt()

        // 应用版本号
        versionCode = configLibs.versions.versionCode.get().toInt()
        // 应用版本名
        versionName = configLibs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 开启 Dex 分包
        multiDexEnabled = true

        vectorDrawables {
            // 运行时绘制向量图
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // 签名配置
        getByName("debug") {
            keyAlias = signingLibs.versions.keyAlias.get()
            keyPassword = signingLibs.versions.keyPassword.get()
            storeFile = file(signingLibs.versions.storeFile.get())
            storePassword = signingLibs.versions.storePassword.get()
        }
        create("release") {
            keyAlias = signingLibs.versions.keyAlias.get()
            keyPassword = signingLibs.versions.keyPassword.get()
            storeFile = file(signingLibs.versions.storeFile.get())
            storePassword = signingLibs.versions.storePassword.get()
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

    // 维度
    flavorDimensions.add("version")

    productFlavors {
        // 正式线上版本
        create("online") {
            dimension = "version"
            // 版本名后缀
            versionNameSuffix = "_online"
            // 备份版本号
            buildConfigField("int", "BACKUP_VERSION", configLibs.versions.backupVersion.get())
        }

        // 开发版本
        create("dev") {
            dimension = "version"
            // 应用包名后缀
            applicationIdSuffix = ".dev"
            // 版本名后缀
            versionNameSuffix = "_dev"
            // 备份版本号
            buildConfigField("int", "BACKUP_VERSION", configLibs.versions.backupVersion.get())
        }
    }

    // 源文件路径设置
    sourceSets {
        named("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
            res.srcDirs("src/main/res")
        }
        named("online") {
            res.srcDirs("src/main/res-online")
        }
        named("dev") {
            res.srcDirs("src/main/res-dev")
        }
    }

    buildFeatures {
        // DataBinding 开启
        dataBinding = true
    }

    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    lint {
        // 出现错误不终止编译
        abortOnError = false
    }

    // 配置 APK 输出路径
    applicationVariants.all {
        if (buildType.name == "release") {
            assembleProvider.get().doLast {
                copy {
                    println("> Task :doLast copyApk")
                    val separator = org.jetbrains.kotlin.konan.file.File.Companion.separator
                    val fromDir =
                        packageApplicationProvider.get().outputDirectory.asFile.get().toString()
                    val intoDir = "${project.rootDir}${separator}outputs${separator}apk"
                    println("> Task :doLast copyApk start copy from $fromDir into $intoDir")
                    from(fromDir)
                    into(intoDir)
                    include("**/*.apk")
                    rename {
                        "Cashbook_${versionName}.apk"
                    }
                    println("> Task :doLast copyApk finish")
                }
            }
        }
    }

    // Java 版本配置
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // kotlin Jvm 版本
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs =
            freeCompilerArgs + arrayOf("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {

    // Kotlin
    implementation(libs.kotlin.stdlib)
    // 协程
    implementation(libs.kotlinx.coroutines)
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
    implementation(libs.androidx.activity.ktx)
    // fragment
    implementation(libs.androidx.fragment.ktx)

    // core-ktx
    implementation(libs.androidx.core.ktx)

    // LifeCycle 拓展
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    // ViewModel 拓展
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // LiveData 拓展
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Material
    implementation(libs.google.material)

    // Logger
    implementation(libs.logger)

    // LiveEventBus
    implementation(libs.liveEventBus)

    // Koin
    implementation(libs.koin3.android)

    // OkHttp
    implementation(libs.squareup.okhttp3)

    // Retrofit
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.converter.kotlin)

    // Coil
    implementation(libs.coil)

    // 状态栏工具
    implementation(libs.immersionbar)
    implementation(libs.immersionbar.ktx)

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
    implementation(libs.mpChart)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}