@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
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
    // 配置只能在当前文件配置三方依赖仓库，否则编译异常退出
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
    // 配置 Version Catalogs
    versionCatalogs {
        // mavenCentral 中的依赖仓库
        create("libs") {
            from(files("./gradle/libs.version.toml"))
        }
        // 签名配置信息
        create("signingLibs") {
            from(files("./gradle/signing.versions.toml"))
        }
    }
}

// 项目名称
rootProject.name = "Cashbook"
// 项目包含的 Module
include(
    ":app",
    ":app-catalog",
)
include(
    ":feature:tags",
    ":feature:types",
    ":feature:books",
    ":feature:assets",
    ":feature:records",
    ":feature:settings",
)
include(
    ":core:data",
    ":core:database",
    ":core:datastore",
    ":core:common",
    ":core:model",
    ":core:domain",
    ":core:design",
    ":core:ui",
    ":core:network",
    ":core:testing",
    ":core:MPChart",
)
include(
    ":sync:work",
)