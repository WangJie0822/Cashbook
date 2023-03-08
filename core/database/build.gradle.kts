plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.room")
    id("cashbook.android.hilt")
}

android {
    namespace = "cn.wj.android.cashbook.core.database"
}

dependencies {

    androidTestImplementation(project(":core:testing"))

    implementation(project(":core:model"))
    implementation(project(":core:common"))
}