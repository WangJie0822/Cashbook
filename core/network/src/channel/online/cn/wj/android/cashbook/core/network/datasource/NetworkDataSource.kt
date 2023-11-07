package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.common.GITEE_OWNER
import cn.wj.android.cashbook.core.common.GITHUB_OWNER
import cn.wj.android.cashbook.core.common.REPO_NAME
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType
import retrofit2.Retrofit

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
            networkJson.asConverterFactory(MediaType.get("application/json")),
        )
        .build()
        .create(RetrofitNetworkApi::class.java)

    /** 根据是否使用 gitee [useGitee] 从不同数据源检查更新 */
    override suspend fun checkUpdate(useGitee: Boolean): GitReleaseEntity? {
        val result = if (useGitee) {
            networkApi.giteeQueryReleaseList(GITEE_OWNER, REPO_NAME)
        } else {
            networkApi.githubQueryReleaseList(GITHUB_OWNER, REPO_NAME)
        }
        logger().i("checkUpdate(useGitee = <$useGitee>), result = <$result>")
        val release = result.filter { it.name?.startsWith("Release") ?: false }
            .sortedBy { it.id }
            .firstOrNull()
        logger().i("checkUpdate(useGitee = <$useGitee>), release = <$release>")
        return release
    }
}