plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "cn.wj.android.cashbook.core.network"
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)

    // kotlin 协程
    implementation(libs.kotlinx.coroutines.android)
    // kotlin 序列化
    implementation(libs.kotlinx.serialization.json)

    // OkHttp
    implementation(libs.squareup.okhttp3)

    // Retrofit
    implementation(libs.squareup.retrofit2)
    implementation(libs.jakewharton.retrofit2.converter.kotlin)

    // HTML 解析
    implementation(libs.jsoup)
}