package cn.wj.android.cashbook.data.store

import cn.wj.android.cashbook.data.constants.GITEE_OWNER
import cn.wj.android.cashbook.data.constants.GITHUB_OWNER
import cn.wj.android.cashbook.data.constants.REPO_NAME
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.transform.toUpdateInfoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 网络数据存储
 *
 * @param service 网络请求服务接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
class WebDataStore(private val service: WebService) {

    /** 获取 Gitee 最新 Release 信息 */
    suspend fun queryLatestRelease(useGitee: Boolean): UpdateInfoEntity = withContext(Dispatchers.IO) {
        if (useGitee) {
            service.giteeQueryRelease(GITEE_OWNER, REPO_NAME, "latest")
        } else {
            service.githubQueryRelease(GITHUB_OWNER, REPO_NAME, "latest")
        }.toUpdateInfoEntity()
    }

    suspend fun getChangelog(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "CHANGELOG.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "CHANGELOG.md")
        }.string()
    }

    suspend fun getPrivacyPolicy(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        }.string()
    }
}