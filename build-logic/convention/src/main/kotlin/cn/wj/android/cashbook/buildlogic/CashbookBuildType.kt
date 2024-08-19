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
 * 编译类型枚举
 *
 * @param applicationIdSuffix 应用 id 后缀
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/8/19
 */
enum class CashbookBuildType(val applicationIdSuffix: String?) {
    Debug(applicationIdSuffix = null),
    Release(applicationIdSuffix = null),
}
