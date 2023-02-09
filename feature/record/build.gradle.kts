plugins {
    id("cashbook.android.library.feature")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
}

android {
    namespace = "cn.wj.android.cashbook.feature.record"
}

dependencies {

    implementation(project(":core:design"))

    // Koin
    implementation(libs.insert.koin.android)
    implementation(libs.insert.koin.androidx.compose)
}
