@file:Suppress("UnstableApiUsage")

plugins {
    id("cashbook.android.library.feature")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
}

android {
    namespace = "cn.wj.android.cashbook.feature.settings"
}

dependencies {

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // okhttp
    implementation(libs.squareup.okhttp3)

    // Markdown 解析
    implementation(libs.noties.markwon)

    // WebView
    implementation(libs.google.accompanist.webview)

    // Constraintlayout
    implementation(libs.androidx.constraintlayout.compose)

    // 权限申请
    implementation(libs.google.accompanist.permissions)
}
