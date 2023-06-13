package cn.wj.android.cashbook.core.model.entity

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum


/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 * @param amount 记录金额
 * @param charges 转账手续费
 * @param concessions 优惠
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param recordTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordViewsEntity(
    val id: Long,
    val typeCategory: RecordTypeCategoryEnum,
    val typeName: String,
    val typeIconResName: String,
    val assetName: String?,
    val assetIconResId: Int?,
    val relatedAssetName: String?,
    val relatedAssetIconResId: Int?,
    val amount: String,
    val charges: String,
    val concessions: String,
    val remark: String,
    val reimbursable: Boolean,
    val relatedTags: List<TagEntity>,
    val relatedRecord: List<RecordViewsEntity>,
    val recordTime: String,
)