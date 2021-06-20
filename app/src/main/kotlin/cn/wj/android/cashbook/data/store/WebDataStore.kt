package cn.wj.android.cashbook.data.store

import cn.wj.android.cashbook.data.constants.GITEE_OWNER
import cn.wj.android.cashbook.data.constants.GITEE_REPO
import cn.wj.android.cashbook.data.entity.GiteeReleaseEntity
import cn.wj.android.cashbook.data.net.WebService
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
    suspend fun giteeQueryLatestRelease(): GiteeReleaseEntity = withContext(Dispatchers.IO) {
        service.giteeQueryRelease(GITEE_OWNER, GITEE_REPO, "latest")
    }
}