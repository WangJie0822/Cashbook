plugins {
    alias(conventionLibs.plugins.cashbook.android.application)
    alias(conventionLibs.plugins.cashbook.android.application.compose)
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

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(libs.google.material)

    implementation(projects.core.design)
    implementation(projects.core.ui)

    implementation(projects.repos.mpChartLib)
}