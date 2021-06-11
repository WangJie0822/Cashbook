package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

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
    val firstType: TypeEntity?,
    val secondType: TypeEntity?,
    val asset: AssetEntity?,
    val intoAsset: AssetEntity?,
    val booksId: Long,
    val amount: String,
    val charge: String,
    val remark: String,
    val tags: List<String>,
    val reimbursable: Boolean,
    val recordTime: String,
    val createTime: String,
    val modifyTime: String
) {

    /** 分类文本 */
    val typeStr: String
        get() = secondType?.name.orElse(firstType?.name).orElse(R.string.balance_adjustment.string)

    /** 是否显示备注 */
    val showRemark: Boolean
        get() = remark.isNotBlank()

    /** 金额文本 */
    val amountStr: String
        get() = when (type) {
            RecordTypeEnum.EXPENDITURE -> {
                "-"
            }
            RecordTypeEnum.INCOME -> {
                "+"
            }
            else -> {
                ""
            }
        } + CurrentBooksLiveData.currency.symbol + amount

    /** 是否显示资产信息 */
    val showAssetInfo: Boolean
        get() = null != asset

    /** 资产信息文本 */
    val assetInfoStr: String
        get() = if (type == RecordTypeEnum.TRANSFER) {
            "${asset?.name}->${intoAsset?.name}"
        } else {
            asset?.name.orEmpty()
        }

    /** 不同类型着色 */
    val colorTint: Int
        get() = when (type) {
            RecordTypeEnum.MODIFY_BALANCE -> {
                R.color.color_on_secondary
            }
            RecordTypeEnum.EXPENDITURE -> {
                R.color.color_spending
            }
            RecordTypeEnum.INCOME -> {
                R.color.color_income
            }
            RecordTypeEnum.TRANSFER -> {
                R.color.color_primary
            }
        }.color
}