plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.sync"
}


dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.core.data)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    kapt(libs.androidx.hilt.compiler)
}