package cn.wj.android.cashbook.data.net

import cn.wj.android.cashbook.data.entity.GitReleaseEntity
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 网络请求服务接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
interface WebService {

    /** 从 Gitee 中根据用户名 [owner] 仓库名 [repo] 以及 Release id [id] 查询相关 Release 信息 */
    @GET(UrlDefinition.GITEE_QUERY_RELEASE)
    suspend fun giteeQueryRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("id") id: String
    ): GitReleaseEntity

    /** 从 Gitee 中根据用户名 [owner] 仓库名 [repo] 获取 CHANGELOG.md 文件数据 */
    @GET(UrlDefinition.GITEE_CHANGELOG)
    suspend fun giteeChangelog(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): ResponseBody

    /** 从 Github 中根据用户名 [owner] 仓库名 [repo] 以及 Release id [id] 查询相关 Release 信息 */
    @GET(UrlDefinition.GITHUB_QUERY_RELEASE)
    suspend fun githubQueryRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("id") id: String
    ): GitReleaseEntity

    /** 从 Github 中根据用户名 [owner] 仓库名 [repo] 获取 CHANGELOG.md 文件数据 */
    @GET(UrlDefinition.GITHUB_CHANGELOG)
    suspend fun githubChangelog(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): ResponseBody
}