plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.flavors)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.testing"
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)

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