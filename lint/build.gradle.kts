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
import cn.wj.android.cashbook.buildlogic.ProjectSetting
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    alias(conventionLibs.plugins.cashbook.android.lint)
}

java {
    // Up to Java 11 APIs are available through desugaring
    // https://developer.android.com/studio/write/java11-minimal-support-table
    sourceCompatibility = ProjectSetting.Config.javaVersion
    targetCompatibility = ProjectSetting.Config.javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = ProjectSetting.Config.javaVersion.toString()
    }
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.android.lint.api)
    testImplementation(libs.android.lint.checks)
    testImplementation(libs.android.lint.tests)
    testImplementation(kotlin("test"))
}
