package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.datastore.GitInfos
import cn.wj.android.cashbook.core.datastore.copy
import cn.wj.android.cashbook.core.model.model.GitDataModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

/**
 * 远程仓库数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
@Singleton
class GitInfosDataSource @Inject constructor(
    private val gitInfos: DataStore<GitInfos>
) {

    val gitData = gitInfos.data
        .map {
            GitDataModel(
                latestVersionName = it.latestVersionName,
                latestVersionInfo = it.latestVersionInfo,
                latestApkName = it.latestApkName,
                latestApkDownloadUrl = it.latestApkDownloadUrl,
            )
        }

    suspend fun updateLatestVersionData(
        latestVersionName: String,
        latestVersionInfo: String,
        latestApkName: String,
        latestApkDownloadUrl: String
    ) {
        gitInfos.updateData {
            it.copy {
                this.latestVersionName = latestVersionName
                this.latestVersionInfo = latestVersionInfo
                this.latestApkName = latestApkName
                this.latestApkDownloadUrl = latestApkDownloadUrl
            }
        }
    }
}