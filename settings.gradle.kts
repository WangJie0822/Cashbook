@file:kotlin.Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        maven { setUrl(Dependencies.MavenRepository.AliYun.gradlePlugin) }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { setUrl(Dependencies.MavenRepository.AliYun.public) }
        maven { setUrl(Dependencies.MavenRepository.AliYun.google) }
        mavenCentral()
    }
}

rootProject.name = "Cashbook"
include(":app")