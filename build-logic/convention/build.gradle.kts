plugins {
    `kotlin-dsl`
}

group = "cn.wj.android.cashbook.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.google.devtools.ksp.gradle.plugin)
    compileOnly(libs.squareup.javapoet)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "cashbook.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "cashbook.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplicationJacoco") {
            id = "cashbook.android.application.jacoco"
            implementationClass = "AndroidApplicationJacocoConventionPlugin"
        }
        register("androidLibrary") {
            id = "cashbook.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "cashbook.android.library.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "cashbook.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "cashbook.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "cashbook.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibraryJacoco") {
            id = "cashbook.android.library.jacoco"
            implementationClass = "AndroidLibraryJacocoConventionPlugin"
        }
    }
}
