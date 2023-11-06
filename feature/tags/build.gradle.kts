plugins {
    alias(libs.plugins.cashbook.android.library.feature)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.feature.tags"
}

dependencies {

    implementation(libs.androidx.constraintlayout.compose)
}
