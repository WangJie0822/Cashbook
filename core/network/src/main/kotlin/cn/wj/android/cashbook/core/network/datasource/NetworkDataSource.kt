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

package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.common.GITEE_OWNER
import cn.wj.android.cashbook.core.common.GITHUB_OWNER
import cn.wj.android.cashbook.core.common.REPO_NAME
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@Singleton
class NetworkDataSource @Inject constructor(
    networkJson: Json,
    okhttpCallFactory: Call.Factory,
) : RemoteDataSource {

    /** 网络请求接口 */
    private val networkApi = Retrofit.Builder()
        .baseUrl(UrlDefinition.BASE_URL)
        .callFactory(okhttpCallFactory)
        .addConverterFactory(
            networkJson.asConverterFactory("application/json".toMediaType()),
        )
        .build()
        .create(RetrofitNetworkApi::class.java)

    /** 根据是否使用 gitee [useGitee] 从不同数据源检查更新 */
    override suspend fun checkUpdate(useGitee: Boolean, canary: Boolean): GitReleaseEntity? {
        val result = if (useGitee) {
            networkApi.giteeQueryReleaseList(GITEE_OWNER, REPO_NAME)
        } else {
            networkApi.githubQueryReleaseList(GITHUB_OWNER, REPO_NAME)
        }
        logger().d("checkUpdate(useGitee = <$useGitee>), result = <$result>")
        val release = result.firstOrNull {
            val name = it.name ?: ""
            name.startsWith("Release") || (canary && name.startsWith("Pre Release"))
        }
        logger().i("checkUpdate(useGitee = <$useGitee>), release = <$release>")
        return release
    }
}
