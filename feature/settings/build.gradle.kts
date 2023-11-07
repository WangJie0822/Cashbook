plugins {
    alias(libs.plugins.cashbook.android.library.feature)
    alias(libs.plugins.cashbook.android.library.flavors)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.feature.settings"
}

dependencies {

    // Markdown 解析
    implementation(libs.noties.markwon)

    // Constraintlayout
    implementation(libs.androidx.constraintlayout.compose)

    // 权限申请
    implementation(libs.google.accompanist.permissions)
}
