// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android Gradle 插件版本
    val androidGradlePluginVersion = "7.2.2"
    // Kotlin 版本
    val kotlinVersion = libs.versions.kotlin.get()

    id("com.android.application") version androidGradlePluginVersion apply false
    id("com.android.library") version androidGradlePluginVersion apply false
    kotlin("android") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}