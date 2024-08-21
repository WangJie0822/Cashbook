// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.google.hilt) apply false
    alias(libs.plugins.takahirom.roborazzi) apply false
    alias(libs.plugins.dropbox.dependency.guard) apply false
}

tasks.register("clean", Delete::class) {
    description = "Clean generate build directory"
    group = JavaBasePlugin.BUILD_TASK_NAME
    delete(rootProject.layout.buildDirectory.asFile.get())
    delete("${rootProject.rootDir.path}/core/build")
    delete("${rootProject.rootDir.path}/feature/build")
    delete("${rootProject.rootDir.path}/sync/build")
}