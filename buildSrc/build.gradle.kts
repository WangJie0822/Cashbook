plugins {
    `kotlin-dsl`
}

repositories {
    maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-repo/") }
    maven { setUrl("https://artifacts.iflytek.com/artifactory/mvn-AndroidPublic-repo/") }
    maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
    maven { setUrl("https://maven.aliyun.com/nexus/content/repositories/jcenter") }
    maven { setUrl("https://jitpack.io") }
    google()
    mavenCentral()
}