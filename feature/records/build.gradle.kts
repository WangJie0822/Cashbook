plugins {
    alias(libs.plugins.cashbook.android.library.feature)
    alias(libs.plugins.cashbook.android.library.flavors)
    alias(libs.plugins.cashbook.android.library.compose)
    alias(libs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.feature.records"
}

dependencies {

    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.paging.compose)

    implementation(projects.repos.mpChartLib)
}
