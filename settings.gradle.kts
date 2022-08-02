@file:kotlin.Suppress("UnstableApiUsage")

enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("io.github.wangjie0822:catalog:1.1.3")

            // 应用配置信息
            // 编译版本
            version("compileSdk", "31")
            // 最低支持版本
            version("minSdk", "21")
            // 目标版本
            version("targetSdk", "30")
            // 版本号
            val versionCode = getVersionCode().toString()
            version("versionCode", versionCode)
            // 版本名
            version("versionName", "v0.5.4_$versionCode")
            // 备份版本号 - 用于兼容性控制
            version("backupVersion", "1")
        }
    }
}

/** 根据日期时间获取对应版本号 */
fun getVersionCode(): Int {
    val sdf = java.text.SimpleDateFormat("yyMMddHH", java.util.Locale.CHINA)
    val formatDate = sdf.format(java.util.Date())
    val versionCode = formatDate.toIntOrNull() ?: 10001
    println("> Task :versionCatalogs:AppConfigs getVersionCode formatDate: $formatDate versionCode: $versionCode")
    return versionCode
}

rootProject.name = "Cashbook"
include(":app")