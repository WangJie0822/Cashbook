package cn.wj.android.cashbook.core.model.entity


/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 *  @param booksId 关联账本 id
 * @param typeId 记录类型 id
 * @param assetId 关联资产 id
 * @param relatedAssetId 转账转入资产 id
 * @param amount 记录金额
 * @param charges 转账手续费
 * @param concessions 优惠
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordEntity(
    val id: Long,
    val booksId: Long,
    val typeId: Long,
    val assetId: Long,
    val relatedAssetId: Long,
    val amount: String,
    val charges: String,
    val concessions: String,
    val remark: String,
    val reimbursable: Boolean,
    val modifyTime: String,
)