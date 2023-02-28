package cn.wj.android.cashbook.core.model.entity

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum


/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 *  @param booksId 关联账本 id
 * @param typeCategory 记录类型分类
 * @param type 记录类型
 * @param asset 关联资产
 * @param relatedAsset 转账转入资产
 * @param amount 记录金额
 * @param charges 转账手续费
 * @param concessions 优惠
 * @param remark 备注
 * @param associated 关联记录
 * @param tags 标签列表
 * @param reimbursable 能否报销
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordEntity(
    val id: Long,
    val booksId: Long,
    val typeCategory: RecordTypeCategoryEnum,
    val type: RecordTypeEntity,
    val asset: AssetEntity?,
    val relatedAsset: AssetEntity?,
    val amount: String,
    val charges: String,
    val concessions: String,
    val remark: String,
    val reimbursable: Boolean,
    val associated: List<RecordEntity>,
    val tags: List<TagEntity>,
    val modifyTime: String,
)