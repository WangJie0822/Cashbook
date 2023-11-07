package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity

/**
 * 远程数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/11/7
 */
interface RemoteDataSource {

    /** 根据是否使用 gitee [useGitee] 从不同数据源检查更新 */
    suspend fun checkUpdate(useGitee: Boolean): GitReleaseEntity?
}