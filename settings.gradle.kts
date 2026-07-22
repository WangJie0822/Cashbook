@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    // 配置插件仓库
    // CI（GitHub Actions 海外 runner）访问 aliyun 镜像不稳定（间歇 502/404，且镜像部分同步时
    // Gradle 会锁定在缺 jar 的镜像上不回落官方源），故 CI 环境跳过 aliyun 直连官方源；
    // 本地开发保持 aliyun 优先加速
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
    // 配置只能在当前文件配置三方依赖仓库，否则编译异常退出
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // 配置三方依赖仓库
    // CI 环境跳过 aliyun 直连官方源，理由同 pluginManagement
    val isCi = System.getenv("CI").toBoolean()
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
include(":feature:record-import")
include(":feature:budget")

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

include(":baselineProfile")
include(":lint")

include(":ui-test-hilt-manifest")