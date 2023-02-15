plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.core.data"
}

dependencies {

    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(libs.kotlinx.coroutines.android)
}