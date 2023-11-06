package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.network.entity.GitContentsEntity
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit 网络请求接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
interface RetrofitNetworkApi {

    /** 从 Gitee 中根据用户名 [owner] 仓库名 [repo] 以及 Release id [id] 查询相关 Release 信息 */
    @GET(UrlDefinition.GITEE_QUERY_RELEASE)
    suspend fun giteeQueryRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("id") id: String
    ): GitReleaseEntity

    /** 从 Gitee 中根据用户名 [owner] 仓库名 [repo] 获取 [path] 文件数据 */
    @GET(UrlDefinition.GITEE_FILE_CONTENTS)
    suspend fun giteeContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): GitContentsEntity

    /** 从 Github 中根据用户名 [owner] 仓库名 [repo] 以及 Release id [id] 查询相关 Release 信息 */
    @GET(UrlDefinition.GITHUB_QUERY_RELEASE)
    suspend fun githubQueryRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("id") id: String
    ): GitReleaseEntity

    /** 从 Github 中根据用户名 [owner] 仓库名 [repo] 获取 [path] 文件数据 */
    @GET(UrlDefinition.GITHUB_FILE_CONTENTS)
    suspend fun githubContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): GitContentsEntity
}