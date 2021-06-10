package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.data.enums.RecordTypeEnum

/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 * @param type 记录类型
 * @param firstType 记录一级分类
 * @param secondType 记录二级分类
 * @param asset 关联资产
 * @param intoAsset 转账转入资产
 * @param amount 记录金额
 * @param charge 转账手续费
 * @param remark 备注
 * @param tags 标签列表
 * @param reimbursable 能否报销
 * @param recordTime 记录时间
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordEntity(
    val id: Long,
    val type: RecordTypeEnum,
    val firstType: TypeEntity,
    val secondType: TypeEntity?,
    val asset: AssetEntity?,
    val intoAsset: AssetEntity?,
    val amount: String,
    val charge: String,
    val remark: String,
    val tags: List<String>,
    val reimbursable: Boolean,
    val recordTime: String,
    val createTime: String,
    val modifyTime: String
)