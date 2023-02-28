plugins {
    id("cashbook.android.library.feature")
    id("cashbook.android.library.compose")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.feature.record"
}

dependencies {

    implementation(project(":core:design"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.hilt.navigation.compose)

    // navigation 动画
    implementation(libs.google.accompanist.navigation.animation)
    // 流式布局
    implementation(libs.google.accompanist.flowlayout)
}
