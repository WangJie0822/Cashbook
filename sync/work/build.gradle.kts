plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.sync"
}


dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:model"))

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.ext.work)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)

    kapt(libs.androidx.hilt.ext.compiler)
}