plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.library.flavors.generate)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.common"

    buildFeatures.buildConfig = true
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    api(libs.orhanobut.logger)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
}