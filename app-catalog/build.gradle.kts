plugins {
    alias(libs.plugins.cashbook.android.application)
    alias(libs.plugins.cashbook.android.application.compose)
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

    implementation(projects.core.design)
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
}