import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "cn.wj.android.cashbook.buildlogic"

val javaVersion = JavaVersion.VERSION_17

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}
kotlin {
    jvmToolchain(javaVersion.ordinal + 1)
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
        register("androidTest") {
            id = "cashbook.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidLint") {
            id = "cashbook.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "cashbook.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidApplicationGenerateFlavors") {
            id = "cashbook.android.application.flavors.generate"
            implementationClass = "AndroidApplicationGenerateFlavorsConventionPlugin"
        }
        register("androidLibraryFlavors") {
            id = "cashbook.android.library.flavors"
            implementationClass = "AndroidLibraryFlavorsConventionPlugin"
        }
        register("androidLibraryGenerateFlavors") {
            id = "cashbook.android.library.flavors.generate"
            implementationClass = "AndroidLibraryGenerateFlavorsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "cashbook.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
