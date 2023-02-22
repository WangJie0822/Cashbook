plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    api(libs.orhanobut.logger)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
}