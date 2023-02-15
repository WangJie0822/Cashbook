plugins {
    id("cashbook.android.library.feature")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.feature.settings"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:design"))

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
}
