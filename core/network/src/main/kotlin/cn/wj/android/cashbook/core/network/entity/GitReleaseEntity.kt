package cn.wj.android.cashbook.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Git 仓库 Release 信息
 *
 * @param name 版本名
 * @param body 描述信息
 * @param assets 资产列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
@Serializable
data class GitReleaseEntity(
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitReleaseAssetEntity>? = null
)

/**
 * 资产信息
 *
 * @param name 名称
 * @param downloadUrl 下载地址
 */
@Serializable
data class GitReleaseAssetEntity(
    val name: String? = null,
    @SerialName("browser_download_url") val downloadUrl: String? = null
)