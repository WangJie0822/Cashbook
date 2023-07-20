plugins {
    id("cashbook.jvm.library")
}

dependencies {
    implementation("org.apache.ant:ant:1.10.13")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    implementation(project(":repos:httpclient4"))
}
