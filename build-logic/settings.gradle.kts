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
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://repo1.maven.org/maven2/") }
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { setUrl("https://maven.aliyun.com/repository/apache-snapshots/") }
    }
}

dependencyResolutionManagement {
    // 配置三方依赖仓库
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://repo1.maven.org/maven2/") }
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/apache-snapshots/") }
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