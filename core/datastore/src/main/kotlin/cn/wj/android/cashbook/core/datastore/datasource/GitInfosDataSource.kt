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

package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.datastore.GitInfos
import cn.wj.android.cashbook.core.datastore.copy
import cn.wj.android.cashbook.core.model.model.GitDataModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程仓库数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
@Singleton
class GitInfosDataSource @Inject constructor(
    private val gitInfos: DataStore<GitInfos>,
) {

    val gitData = gitInfos.data
        .map {
            GitDataModel(
                latestVersionName = it.latestVersionName,
                latestVersionInfo = it.latestVersionInfo,
                latestApkName = it.latestApkName,
                latestApkDownloadUrl = it.latestApkDownloadUrl,
            )
        }

    suspend fun updateLatestVersionData(
        latestVersionName: String,
        latestVersionInfo: String,
        latestApkName: String,
        latestApkDownloadUrl: String,
    ) {
        gitInfos.updateData {
            it.copy {
                this.latestVersionName = latestVersionName
                this.latestVersionInfo = latestVersionInfo
                this.latestApkName = latestApkName
                this.latestApkDownloadUrl = latestApkDownloadUrl
            }
        }
    }
}
