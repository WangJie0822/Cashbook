plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.domain"
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.data)
}