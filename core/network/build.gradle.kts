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
    alias(libs.plugins.cashbook.android.library)
    alias(libs.plugins.cashbook.android.library.flavors)
    alias(libs.plugins.cashbook.android.library.jacoco)
    alias(libs.plugins.cashbook.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "cn.wj.android.cashbook.core.network"

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

    // kotlin 协程
    implementation(libs.kotlinx.coroutines.android)
    // kotlin 序列化
    implementation(libs.kotlinx.serialization.json)

    // OkHttp
    OnlineImplementation(libs.squareup.okhttp3)
    DevImplementation(libs.squareup.okhttp3)

    // Retrofit
    OnlineImplementation(libs.squareup.retrofit2)
    OnlineImplementation(libs.jakewharton.retrofit2.converter.kotlin)
    DevImplementation(libs.squareup.retrofit2)
    DevImplementation(libs.jakewharton.retrofit2.converter.kotlin)

    // HTML 解析
    implementation(libs.jsoup)
}