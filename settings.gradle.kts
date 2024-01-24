@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
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
    // 配置只能在当前文件配置三方依赖仓库，否则编译异常退出
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // 配置三方依赖仓库
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
    // 配置 Version Catalogs
    versionCatalogs {
        // 本地项目插件
        create("conventionLibs") {
            from(files("./gradle/convention.versions.toml"))
        }
        // 签名配置信息
        if (File("./gradle/signing.versions.toml").exists()) {
            create("signingLibs") {
                from(files("./gradle/signing.versions.toml"))
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// 项目名称
rootProject.name = "Cashbook"

// 项目包含的 Module
include(":app")
include(":app-catalog")

include(":feature:tags")
include(":feature:types")
include(":feature:books")
include(":feature:assets")
include(":feature:records")
include(":feature:settings")

include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:network")
include(":core:testing")
include(":core:design")

include(":core:ui")
include(":core:model")
include(":core:common")

include(":sync:work")

include(":repos:MPChartLib")

include(":baselineProfile")
include(":lint")

include(":ui-test-hilt-manifest")