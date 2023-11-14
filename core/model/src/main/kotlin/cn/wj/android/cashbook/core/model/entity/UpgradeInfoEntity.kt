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

package cn.wj.android.cashbook.core.model.entity

/**
 * 升级信息数据实体类
 *
 * @param versionName 版本名
 * @param versionInfo 版本更新信息
 * @param apkName 安装包名称
 * @param downloadUrl 下载地址
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
data class UpgradeInfoEntity(
    val versionName: String,
    val versionInfo: String,
    val apkName: String,
    val downloadUrl: String,
) {
    val displayVersionInfo: String
        get() = with(StringBuilder()) {
            append("# ")
            appendLine(versionName)
            appendLine()
            appendLine(versionInfo)
            toString()
        }
}
