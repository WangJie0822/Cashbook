plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
}

android {
    namespace = "cn.wj.android.cashbook.core.design"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.runtime)

    api(libs.google.material)
}
