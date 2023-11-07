import cn.wj.android.cashbook.buildlogic.CashbookFlavor

plugins {
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.flavors)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
}

android {
    namespace = "cn.wj.android.cashbook.sync"

    sourceSets {
        @Suppress("EnumValuesSoftDeprecate")
        CashbookFlavor.values().forEach { flavor ->
            val srcDir = if (flavor == CashbookFlavor.Offline) {
                // 离线渠道
                "src/channel/offline"
            } else {
                // 在线渠道
                "src/channel/online"
            }
            getByName(flavor.name).java.srcDirs(srcDir)
        }
    }
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.core.data)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    kapt(libs.androidx.hilt.compiler)

    // okhttp
    OnlineImplementation(libs.squareup.okhttp3)
    DevImplementation(libs.squareup.okhttp3)
}