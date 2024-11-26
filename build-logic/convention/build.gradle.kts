/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.androidx.room.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.compose.gradle.plugin)
    compileOnly(libs.google.devtools.ksp.gradle.plugin)
    compileOnly(libs.squareup.javapoet)
    implementation(libs.google.truth)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
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
            implementationClass = "HiltConventionPlugin"
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
        register("androidGenerateFlavors") {
            id = "cashbook.android.flavors.generate"
            implementationClass = "AndroidGenerateFlavorsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "cashbook.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
