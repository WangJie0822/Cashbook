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

package cn.wj.android.cashbook.buildlogic

/**
 * 渠道枚举
 *
 * @param dimension 维度
 * @param applicationIdSuffix 应用 id 后缀
 * @param versionNameSuffix 版本名后缀
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/2
 */
enum class CashbookFlavor(
    val dimension: CashbookFlavorDimension,
    val applicationIdSuffix: String?,
    val versionNameSuffix: String?,
) {
    /** 在线渠道，需要网络请求 */
    Online(CashbookFlavorDimension.ContentType, null, "_online"),

    /** 离线渠道，无网络请求 */
    Offline(CashbookFlavorDimension.ContentType, null, "_offline"),

    /** 尝鲜渠道 */
    Canary(CashbookFlavorDimension.ContentType, null, "_canary"),

    /** 开发渠道 */
    Dev(CashbookFlavorDimension.ContentType, ".dev", "_dev"),
}
