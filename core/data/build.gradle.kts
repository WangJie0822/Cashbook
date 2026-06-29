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
    alias(conventionLibs.plugins.cashbook.android.library.jacoco)
    alias(conventionLibs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.core.data"
}

// 将根目录 PRIVACY_POLICY.md / CHANGELOG.md 拷入 assets。
// 原实现挂在 libraryVariants.preBuild.doFirst（AGP 9 移除变体 API），改为普通 Copy task 并让 preBuild 依赖它。
val copyLegalDocsToAssets by tasks.registering(Copy::class) {
    val intoDir = File(projectDir, "src/main/assets")
    // config-cache：执行阶段(doFirst)不可访问 Task.project，配置阶段捕获所需值
    val rootDirFile = rootDir
    val projectName = project.name
    doFirst {
        println("> Task :$projectName:copyLegalDocsToAssets copy .md files from $rootDirFile into $intoDir")
        intoDir.deleteRecursively()
    }
    from(rootDirFile) {
        include("PRIVACY_POLICY.md", "CHANGELOG.md")
    }
    into(intoDir)
}
tasks.named("preBuild").configure { dependsOn(copyLegalDocsToAssets) }

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    api(libs.androidx.paging.common)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.google.truth)
    // 真实 org.json：本地 JVM 单测中 android.jar 的 org.json 是抛 "not mocked" 的 stub，
    // 测试经 SettingsBackupCodec / BackupManifest 用到 org.json 时需真实实现（运行时/androidTest 用系统内置）
    testImplementation(libs.org.json)
}