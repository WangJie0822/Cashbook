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

pluginManagement {
    // 配置插件仓库
    // CI（GitHub Actions 海外 runner）访问 aliyun 镜像不稳定（间歇 502/404），
    // CI 环境跳过 aliyun 直连官方源；本地开发保持 aliyun 优先加速
    val isCi = System.getenv("CI").toBoolean()
    repositories {
        maven { setUrl("https://repo1.maven.org/maven2") }
        if (!isCi) {
            maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { setUrl("https://maven.aliyun.com/repository/public") }
            maven { setUrl("https://maven.aliyun.com/repository/google") }
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // 配置三方依赖仓库
    // CI 环境跳过 aliyun 直连官方源，理由同 pluginManagement
    val isCi = System.getenv("CI").toBoolean()
    @Suppress("UnstableApiUsage")
    repositories {
        maven { setUrl("https://repo1.maven.org/maven2") }
        if (!isCi) {
            maven { setUrl("https://maven.aliyun.com/repository/public") }
            maven { setUrl("https://maven.aliyun.com/repository/google") }
        }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
// 项目包含的 Module
include(":convention")