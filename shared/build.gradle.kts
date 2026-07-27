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
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9 KMP 库插件：提供 android target（AGP 9 禁止 com.android.library + kotlin.multiplatform 联用）
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    // AGP 9.2.1 起用 android{}（旧 androidLibrary{} 已弃用 @Deprecated(ReplaceWith("android"))）
    android {
        namespace = "cn.wj.android.cashbook.shared"
        // shared 不走 cashbook convention 插件，compileSdk 须与 ProjectSetting.Config.COMPILE_SDK 手动同步
        compileSdk = 37
        minSdk = 24
        // 启用 JVM host 单元测试（使 commonTest 在本机 JVM 上运行，无需设备）
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            // api：ApplicationCoroutineScope 公共超类型暴露 CoroutineScope，消费方需传递可见
            api(libs.kotlinx.coroutines.core)
            // api：DateSelectionEntity 公共字段暴露 kotlinx.datetime.LocalDate/YearMonth，消费方需传递可见
            api(libs.kotlinx.datetime)
            // CashbookDispatchers 的 @Dispatcher 用 @Qualifier（JSR-330 纯注解，KMP 可用）；api 暴露给消费方（domain）的 Hilt 处理
            api(libs.javax.inject)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
