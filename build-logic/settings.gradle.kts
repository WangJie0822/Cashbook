@file:Suppress("UnstableApiUsage")

pluginManagement {
    // 配置插件仓库
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { setUrl("https://maven.aliyun.com/repository/apache-snapshots/") }
        maven { setUrl("https://repo1.maven.org/maven2/") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // 配置三方依赖仓库
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/apache-snapshots/") }
        maven { setUrl("https://repo1.maven.org/maven2/") }
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