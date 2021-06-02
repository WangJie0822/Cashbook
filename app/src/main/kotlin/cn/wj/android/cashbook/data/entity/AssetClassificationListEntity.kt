package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum

/**
 * 资产分类列表数据
 *
 * @param classificationType 分类大类
 * @param classifications 分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
data class AssetClassificationListEntity(
    val classificationType: ClassificationTypeEnum,
    val classifications: ArrayList<AssetClassificationEnum>
)