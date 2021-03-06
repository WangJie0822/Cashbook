package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.drawableString
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toFloatOrZero
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 * @param typeEnum 记录类型
 * @param type 记录分类
 * @param asset 关联资产
 * @param intoAsset 转账转入资产
 * @param booksId 关联账本 id
 * @param record 关联记录
 * @param amount 记录金额
 * @param charge 转账手续费
 * @param remark 备注
 * @param tags 标签列表
 * @param reimbursable 能否报销
 * @param system 是否是系统记录
 * @param recordTime 记录时间
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 * @param showDate 备注文本是否显示时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Parcelize
data class RecordEntity(
    val id: Long,
    val typeEnum: RecordTypeEnum,
    val type: TypeEntity?,
    val asset: AssetEntity?,
    val intoAsset: AssetEntity?,
    val booksId: Long,
    val record: RecordEntity?,
    val beAssociated: RecordEntity?,
    val amount: String,
    val charge: String,
    val remark: String,
    val tags: List<TagEntity>,
    val reimbursable: Boolean,
    val system: Boolean,
    val recordTime: Long,
    val createTime: String,
    val modifyTime: String,
    val showDate: Boolean
) : Parcelable {

    /** 是否显示标签 */
    @IgnoredOnParcel
    val showTags: Boolean
        get() = tags.isNotEmpty()

    /** 标签显示文本 */
    @IgnoredOnParcel
    val tagsStr: String
        get() = with(StringBuilder()) {
            tags.forEach {
                if (isNotBlank()) {
                    append(",")
                }
                append(it.name)
            }
            toString()
        }

    /** 分类文本 */
    @IgnoredOnParcel
    val typeStr: String
        get() = type?.name.orElse(R.string.balance_adjustment.string)

    /** 类型图标显示 */
    @IgnoredOnParcel
    val typeIconResIdStr: String
        get() = type?.iconResName.orElse(R.string.type_icon_name_balance_adjustment.drawableString)

    /** 是否显示备注 */
    @IgnoredOnParcel
    val showRemark: Boolean
        get() = showDate || remark.isNotBlank()

    /** 备注显示文本 */
    @IgnoredOnParcel
    val remarkStr: String
        get() = if (showDate) {
            "${recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)} $remark"
        } else {
            remark
        }

    /** 资金文本 */
    @IgnoredOnParcel
    val amountStr: String
        get() {
            val amountValue = amount.moneyFormat()
            return when (typeEnum) {
                RecordTypeEnum.EXPENDITURE -> {
                    "-$amountValue"
                }
                RecordTypeEnum.INCOME -> {
                    "+$amountValue"
                }
                else -> {
                    amountValue
                }
            }
        }

    /** 是否显示手续费 */
    @IgnoredOnParcel
    val showCharge: Boolean
        get() = typeEnum == RecordTypeEnum.TRANSFER && charge.toFloatOrZero() != 0f

    /** 手续费文本 */
    @IgnoredOnParcel
    val chargeStr: String
        get() = if (charge.isBlank()) {
            ""
        } else {
            charge.moneyFormat()
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
        get() = typeEnum == RecordTypeEnum.TRANSFER

    /** 资产信息文本 */
    @IgnoredOnParcel
    val assetInfoStr: String
        get() = if (typeEnum == RecordTypeEnum.TRANSFER) {
            "${asset?.name}->${intoAsset?.name}"
        } else {
            asset?.name.orEmpty()
        } + if (!showCharge) {
            ""
        } else {
            "($chargeStr)"
        }

    /** 记录时间 */
    @IgnoredOnParcel
    val recordTimeStr: String
        get() = recordTime.dateFormat(DATE_FORMAT_NO_SECONDS)

    /** 能否修改，修改余额不可以修改 */
    @IgnoredOnParcel
    val canModify: Boolean
        get() = typeEnum != RecordTypeEnum.MODIFY_BALANCE

    /** 能否删除，系统插入数据无法删除 */
    @IgnoredOnParcel
    val canDelete: Boolean
        get() = !system

    /** 是否显示关联信息 */
    @IgnoredOnParcel
    val showAssociated: Boolean
        get() = null != beAssociated

    /** 当前记录是否有关联的记录 */
    @IgnoredOnParcel
    val showAssociation: Boolean
        get() = null != record

    /** 当前记录关联的记录类型 icon 资源id */
    @IgnoredOnParcel
    val associationTypeIconResIdStr: String
        get() = record?.typeIconResIdStr.orEmpty()

    /** 当前记录关联的记录类型名称 */
    @IgnoredOnParcel
    val associationTypeStr: String
        get() = record?.typeStr.orEmpty()

    /** 当前记录关联的记录金额信息 */
    @IgnoredOnParcel
    val associationAmountStr: String
        get() = record?.amountStr.orEmpty()

    /** 关联信息文本 */
    @IgnoredOnParcel
    val associatedStr: String
        get() = when {
            beAssociated?.type?.refund.condition -> {
                // 退款
                R.string.refunded_with_colon.string + beAssociated?.amountStr
            }
            beAssociated?.type?.reimburse.condition -> {
                // 报销
                R.string.returned_with_colon.string + beAssociated?.amountStr
            }
            else -> {
                ""
            }
        }

    /** 是否在信息弹窗中显示关联信息 */
    @IgnoredOnParcel
    val showAssociatedInInfoDialog: Boolean
        get() = reimbursable || showAssociated

    /** 在信息弹窗中显示关联信息 */
    @IgnoredOnParcel
    val associatedStrInInfoDialog: String
        get() = when {
            beAssociated?.type?.refund.condition -> {
                // 退款
                R.string.refunded_with_colon.string + beAssociated?.amountStr
            }
            beAssociated?.type?.reimburse.condition -> {
                // 报销
                R.string.returned_with_colon.string + beAssociated?.amountStr
            }
            reimbursable -> {
                R.string.reimbursable.string
            }
            else -> {
                ""
            }
        }

    /** 金额文本颜色 */
    @IgnoredOnParcel
    val amountTextTint: Int
        get() = when (typeEnum) {
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

    /** 手续费文本颜色 */
    @IgnoredOnParcel
    val chargeTextTint: Int
        get() = if (charge.toFloatOrZero() > 0f) {
            R.color.color_expenditure
        } else {
            R.color.color_income
        }.color
}