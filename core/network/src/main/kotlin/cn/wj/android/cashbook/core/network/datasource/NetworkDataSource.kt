package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.common.CHANGELOG_FILE_PATH
import cn.wj.android.cashbook.core.common.GITEE_OWNER
import cn.wj.android.cashbook.core.common.GITHUB_OWNER
import cn.wj.android.cashbook.core.common.PRIVACY_POLICY_FILE_PATH
import cn.wj.android.cashbook.core.common.REPO_NAME
import cn.wj.android.cashbook.core.network.entity.GitContentsEntity
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
) {

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
    suspend fun checkUpdate(useGitee: Boolean): GitReleaseEntity {
        return if (useGitee) {
            networkApi.giteeQueryRelease(GITEE_OWNER, REPO_NAME, "latest")
        } else {
            networkApi.githubQueryRelease(GITHUB_OWNER, REPO_NAME, "latest")
        }
    }

    suspend fun getChangelog(useGitee: Boolean): GitContentsEntity {
        return getFileContent(useGitee, CHANGELOG_FILE_PATH)
    }

    suspend fun getPrivacyPolicy(useGitee: Boolean): GitContentsEntity {
        return getFileContent(useGitee, PRIVACY_POLICY_FILE_PATH)
    }

    private suspend fun getFileContent(useGitee: Boolean, filePath: String): GitContentsEntity {
        return if (useGitee) {
            networkApi.giteeContents(GITEE_OWNER, REPO_NAME, filePath)
        } else {
            networkApi.githubContents(GITHUB_OWNER, REPO_NAME, filePath)
        }
    }
}