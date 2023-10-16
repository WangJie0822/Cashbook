plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.data"
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.ktx)
}