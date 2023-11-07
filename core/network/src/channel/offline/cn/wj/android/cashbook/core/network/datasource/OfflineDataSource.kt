package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@Singleton
class OfflineDataSource @Inject constructor() : RemoteDataSource {

    /** 根据是否使用 gitee [useGitee] 从不同数据源检查更新 */
    override suspend fun checkUpdate(useGitee: Boolean): GitReleaseEntity? {
        return null
    }
}