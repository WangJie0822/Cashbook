plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9 KMP 库插件：提供 androidLibrary target（AGP 9 禁止 com.android.library + kotlin.multiplatform 联用）
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "cn.wj.android.cashbook.shared"
        compileSdk = 36
        minSdk = 24
        // 启用 JVM host 单元测试（使 commonTest 在本机 JVM 上运行，无需设备）
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // api：DateSelectionEntity 公共字段暴露 kotlinx.datetime.LocalDate/YearMonth，消费方需传递可见
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
