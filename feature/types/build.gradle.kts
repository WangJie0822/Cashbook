plugins {
    id("cashbook.android.library.feature")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
}

android {
    namespace = "cn.wj.android.cashbook.feature.types"
}

dependencies {

    implementation(libs.androidx.constraintlayout.compose)
}
