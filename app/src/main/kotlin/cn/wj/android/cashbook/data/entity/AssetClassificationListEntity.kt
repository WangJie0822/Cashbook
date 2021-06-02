package cn.wj.android.cashbook.data.entity

import androidx.annotation.StringRes
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum

/**
 * 资产分类列表数据
 *
 * @param groupNameResId 分组名称资源id
 * @param classifications 分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
data class AssetClassificationListEntity(
    @StringRes val groupNameResId: Int,
    val classifications: ArrayList<AssetClassificationEnum>
)