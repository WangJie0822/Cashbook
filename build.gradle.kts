// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-repo/") }
        maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-AndroidPublic-repo/") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/repositories/jcenter") }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.0")
        classpath(kotlin(module = "gradle-plugin", version = Dependencies.Kotlin.version))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-repo/") }
        maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-AndroidPublic-repo/") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/repositories/jcenter") }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}