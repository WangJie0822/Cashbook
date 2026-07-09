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

// core:testing 是测试基础设施模块（对下游提供 Fake/测试工具），无 src/test、自身 0 @Test。
// Gradle 9 起 Test.failOnNoDiscoveredTests 默认 true：实测本模块 testDebugUnitTest task 非 NO-SOURCE
// （会实际执行 test 检测）但发现 0 @Test → 判 FAILED；对照 core:database 等同为 0 测试的模块，其
// testDebugUnitTest 是 NO-SOURCE 被 skip、不触发。（注：test 源集本身为空——compileDebugUnitTestKotlin
// 亦 NO-SOURCE；本模块 test task 非 NO-SOURCE 的确切机制未完全隔离，疑与其作为测试基础设施库的
// 插件/classpath 配置有关，不影响此处结论。）CI 聚合的无前缀 testDebugUnitTest（覆盖所有库模块）会命中
// 本模块，故就地关闭该检查（仅本模块，不用 build-logic 全局关，保留其他模块「0 测试即报警」的安全网）。
// 若本模块日后新增真单测，删除本段以恢复默认检查（否则会掩盖「测试写了但 0 被发现」的配置错误）。
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    failOnNoDiscoveredTests = false
}