plugins {
    id("cashbook.android.library")
    id("cashbook.android.library.jacoco")
    id("cashbook.android.hilt")
    alias(libs.plugins.google.protobuf)
}

android {
    namespace = "cn.wj.android.cashbook.core.datastore"
}

protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

androidComponents.beforeVariants {
    android.sourceSets.getByName(it.name) {
        java.srcDir(buildDir.resolve("generated/source/proto/${it.name}/java"))
        kotlin.srcDir(buildDir.resolve("generated/source/proto/${it.name}/kotlin"))
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.datastore.proto)
    implementation(libs.google.protobuf.kotlin.lite)
}