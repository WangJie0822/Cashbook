// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven { setUrl(Dependencies.MavenRepository.AliYun.public) }
        maven { setUrl(Dependencies.MavenRepository.AliYun.google) }
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.1")
        classpath(kotlin(module = "gradle-plugin", version = Dependencies.Kotlin.version))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}