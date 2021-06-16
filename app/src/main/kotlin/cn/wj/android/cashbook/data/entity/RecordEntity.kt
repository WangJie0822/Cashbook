package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 * @param type 记录类型
 * @param firstType 记录一级分类
 * @param secondType 记录二级分类
 * @param asset 关联资产
 * @param intoAsset 转账转入资产
 * @param booksId 关联账本 id
 * @param record 退款关联记录
 * @param amount 记录金额
 * @param charge 转账手续费
 * @param remark 备注
 * @param tags 标签列表
 * @param reimbursable 能否报销
 * @param system 是否是系统记录
 * @param recordTime 记录时间
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Parcelize
data class RecordEntity(
    val id: Long,
    val type: RecordTypeEnum,
    val firstType: TypeEntity?,
    val secondType: TypeEntity?,
    val asset: AssetEntity?,
    val intoAsset: AssetEntity?,
    val booksId: Long,
    val record: RecordEntity?,
    val amount: String,
    val charge: String,
    val remark: String,
    val tags: List<String>,
    val reimbursable: Boolean,
    val system: Boolean,
    val recordTime: Long,
    val createTime: String,
    val modifyTime: String
) : Parcelable {

    /** 分类文本 */
    @IgnoredOnParcel
    val typeStr: String
        get() = secondType?.name.orElse(firstType?.name).orElse(R.string.balance_adjustment.string)

    /** 类型图标显示 */
    @IgnoredOnParcel
    val typeIconResIdStr: String
        get() = secondType?.iconResName.orElse(firstType?.iconResName).orElse("@drawable/${R.string.type_icon_name_balance_adjustment.string}")

    /** 是否显示备注 */
    @IgnoredOnParcel
    val showRemark: Boolean
        get() = remark.isNotBlank()

    /** 金额文本 */
    @IgnoredOnParcel
    val amountStrWithCharge: String
        get() {
            val symbol = CurrentBooksLiveData.currency.symbol
            return when (type) {
                RecordTypeEnum.EXPENDITURE -> {
                    "-$symbol$amount"
                }
                RecordTypeEnum.INCOME -> {
                    "+$symbol$amount"
                }
                RecordTypeEnum.TRANSFER -> {
                    "$symbol$amount${
                        if (charge.toFloatOrNull().orElse(0f) > 0f) {
                            "(-$symbol$charge)"
                        } else {
                            ""
                        }
                    }"
                }
                else -> {
                    "$symbol$amount"
                }
            }
        }

    /** 资金文本 */
    @IgnoredOnParcel
    val amountStr: String
        get() {
            val symbol = CurrentBooksLiveData.currency.symbol
            return when (type) {
                RecordTypeEnum.EXPENDITURE -> {
                    "-$symbol$amount"
                }
                RecordTypeEnum.INCOME -> {
                    "+$symbol$amount"
                }
                RecordTypeEnum.TRANSFER -> {
                    "$symbol$amount"
                }
                else -> {
                    "$symbol$amount"
                }
            }
        }

    /** 是否显示手续费 */
    @IgnoredOnParcel
    val showCharge: Boolean
        get() = type == RecordTypeEnum.TRANSFER && charge.toFloatOrNull().orElse(0f) > 0f

    /** 手续费文本 */
    @IgnoredOnParcel
    val chargeStr: String
        get() = if (charge.isBlank()) {
            ""
        } else {
            "-${CurrentBooksLiveData.currency.symbol}$charge"
        }

    /** 是否显示资产信息 */
    @IgnoredOnParcel
    val showAssetInfo: Boolean
        get() = null != asset

    /** 资产名称显示 */
    @IgnoredOnParcel
    val assetName: String
        get() = asset?.name.orEmpty()

    /** 资产 logo 资源 id */
    @IgnoredOnParcel
    val assetLogoResId: Int?
        get() = asset?.classification?.logoResId

    /** 转入资产名称 */
    @IgnoredOnParcel
    val intoAssetName: String
        get() = intoAsset?.name.orEmpty()

    /** 转入资产 logo 资源 id */
    @IgnoredOnParcel
    val intoAssetLogoResId: Int?
        get() = intoAsset?.classification?.logoResId

    /** 是否显示资产信息 - 区分转账情况 */
    @IgnoredOnParcel
    val showIntoAsset: Boolean
        get() = type == RecordTypeEnum.TRANSFER

    /** 资产信息文本 */
    @IgnoredOnParcel
    val assetInfoStr: String
        get() = if (type == RecordTypeEnum.TRANSFER) {
            "${asset?.name}->${intoAsset?.name}"
        } else {
            asset?.name.orEmpty()
        }

    /** 记录时间 */
    @IgnoredOnParcel
    val recordTimeStr: String
        get() = recordTime.dateFormat(DATE_FORMAT_NO_SECONDS)

    /** 能否修改，修改余额不可以修改 */
    @IgnoredOnParcel
    val canModify: Boolean
        get() = type != RecordTypeEnum.MODIFY_BALANCE

    /** 不同类型着色 */
    @IgnoredOnParcel
    val colorTint: Int
        get() = when (type) {
            RecordTypeEnum.MODIFY_BALANCE -> {
                R.color.color_on_secondary
            }
            RecordTypeEnum.EXPENDITURE -> {
                R.color.color_expenditure
            }
            RecordTypeEnum.INCOME -> {
                R.color.color_income
            }
            RecordTypeEnum.TRANSFER -> {
                R.color.color_primary
            }
        }.color
}