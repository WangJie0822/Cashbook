plugins {
   `kotlin-dsl`
}

repositories {
   maven { setUrl("https://maven.aliyun.com/repository/public/") }
   maven { setUrl("https://maven.aliyun.com/repository/google/") }
   mavenCentral()
}