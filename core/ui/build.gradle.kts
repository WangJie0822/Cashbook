plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.core.ui"
}

dependencies {

    implementation(projects.core.model)
    implementation(projects.core.design)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.navigation.runtime.ktx)

    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
}