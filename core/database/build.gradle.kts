plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.room)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.database"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

dependencies {

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(projects.core.testing)

    implementation(projects.core.common)
    implementation(projects.core.model)
}