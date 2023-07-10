plugins {
    // Android Kotlin 应用
    id("cashbook.android.application")
    id("cashbook.android.application.compose")
}

android {
    namespace = "cn.wj.android.cashbookcatalog"

    defaultConfig {
        // 应用 id
        applicationId = namespace

        // 开启 Dex 分包
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }
}

dependencies {

    implementation(project(":core:design"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.activity.compose)
}