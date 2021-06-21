package cn.wj.android.cashbook.data.net

/**
 * Url 定义
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
object UrlDefinition {

    /** 使用 Gitee api */
    const val BASE_URL = "https://gitee.com/api/v5/"

    /** 查询 Gitee 中的 Release 信息 */
    const val GITEE_QUERY_RELEASE = "https://gitee.com/api/v5/repos/{owner}/{repo}/releases/{id}"

    /** 查询 Github 中的 Release 信息 */
    const val GITHUB_QUERY_RELEASE = "https://api.github.com/repos/{owner}/{repo}/releases/{id}"
}