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
plugins {
    alias(conventionLibs.plugins.cashbook.android.library)
    alias(conventionLibs.plugins.cashbook.android.library.compose)
    alias(conventionLibs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.testing"
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.design)

    api(libs.junit)
    api(libs.google.truth)

    api(libs.kotlinx.coroutines.test)

    api(libs.androidx.test.core)
    api(libs.androidx.test.espresso.core)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.rules)

    api(libs.androidx.activity.compose)
    api(libs.androidx.compose.ui.test.junit4)

    api(libs.google.hilt.android.testing)
    api(libs.google.accompanist.testharness)

    api(libs.takahirom.roborazzi)
    api(libs.robolectric.shadows)

    debugApi(libs.androidx.compose.ui.test.manifest)
}

// core:testing 是测试基础设施模块（对下游提供 Fake/测试工具），自身不含单元测试。
// Gradle 9 起 Test.failOnNoDiscoveredTests 默认 true：本模块 test 源集因 junit/Hilt testing 依赖
// 非 NO-SOURCE，但无任何 @Test → testDebugUnitTest 会判 FAILED。CI 聚合的 testDebugUnitTest
// （无前缀，覆盖所有库模块）会命中本模块，故就地关闭该检查（仅本模块，不影响其他模块的空测试报警）。
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    failOnNoDiscoveredTests = false
}