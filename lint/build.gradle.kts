import cn.wj.android.cashbook.buildlogic.ProjectSetting
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    alias(conventionLibs.plugins.cashbook.android.lint)
}

java {
    // Up to Java 11 APIs are available through desugaring
    // https://developer.android.com/studio/write/java11-minimal-support-table
    sourceCompatibility = ProjectSetting.Config.javaVersion
    targetCompatibility = ProjectSetting.Config.javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = ProjectSetting.Config.javaVersion.toString()
    }
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.android.lint.api)
    testImplementation(libs.android.lint.checks)
    testImplementation(libs.android.lint.tests)
    testImplementation(kotlin("test"))
}
