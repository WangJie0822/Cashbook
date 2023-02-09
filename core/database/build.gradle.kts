plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.room")
}

android {
    namespace = "cn.wj.android.cashbook.core.database"
}

dependencies {

    implementation(project(":core:model"))

    implementation(libs.insert.koin.android)
}