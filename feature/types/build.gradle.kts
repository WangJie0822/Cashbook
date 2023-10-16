plugins {
    alias(libs.plugins.cashbook.android.library.feature)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.feature.types"
}

dependencies {

    implementation(libs.androidx.constraintlayout.compose)
}
