plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
}

android {
    namespace = "cn.wj.android.cashbook.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}