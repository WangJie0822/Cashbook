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

import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit 网络请求接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
interface RetrofitNetworkApi {

    @GET(UrlDefinition.GITEE_RELEASE_LIST)
    suspend fun giteeQueryReleaseList(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = DEFAULT_PAGE_SIZE,
        @Query("direction") direction: String = "desc",
    ): List<GitReleaseEntity>

    @GET(UrlDefinition.GITHUB_RELEASE_LIST)
    suspend fun githubQueryReleaseList(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = DEFAULT_PAGE_SIZE,
        @Query("direction") direction: String = "desc",
    ): List<GitReleaseEntity>
}
