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
import cn.wj.android.cashbook.buildlogic.TEST_INSTRUMENTATION_RUNNER

plugins {
    alias(conventionLibs.plugins.cashbook.android.library)
    alias(conventionLibs.plugins.cashbook.android.library.compose)
    alias(conventionLibs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.takahirom.roborazzi)
}

android {
    namespace = "cn.wj.android.cashbook.core.design"

    defaultConfig {
        testInstrumentationRunner = TEST_INSTRUMENTATION_RUNNER
    }
}

dependencies {

    lintPublish(projects.lint)

    implementation(projects.core.common)

    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.google.material)

    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.google.accompanist.testharness)
    testImplementation(libs.google.hilt.android.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.takahirom.roborazzi)
    testImplementation(projects.core.testing)

    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(projects.core.testing)
}
