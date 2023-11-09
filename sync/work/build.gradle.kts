/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import cn.wj.android.cashbook.buildlogic.CashbookFlavor

plugins {
    alias(conventionLibs.plugins.cashbook.android.library)
    alias(conventionLibs.plugins.cashbook.android.library.flavors)
    alias(conventionLibs.plugins.cashbook.android.library.jacoco)
    alias(conventionLibs.plugins.cashbook.android.hilt)
    alias(conventionLibs.plugins.cashbook.android.lint)
}

android {
    namespace = "cn.wj.android.cashbook.sync"

    sourceSets {
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