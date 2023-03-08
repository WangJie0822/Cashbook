plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.compose")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.core.testing"
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    api(libs.junit)
    api(libs.androidx.test.core)
    api(libs.kotlinx.coroutines.test)

    api(libs.androidx.test.espresso.core)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.rules)
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.google.hilt.android.testing)

    debugApi(libs.androidx.compose.ui.test.manifest)
}