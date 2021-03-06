package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.decimalFormat
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toBigDecimalOrZero
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

/**
 * 资产信息数据库表
 *
 * @param id 资产主键，自增长
 * @param booksId 所属账本 id
 * @param name 资产名称
 * @param totalAmount 总额度，信用卡使用
 * @param billingDate 账单日，信用卡使用
 * @param repaymentDate 还款日，信用卡使用
 * @param type 资产大类
 * @param classification 资产分类
 * @param invisible 是否隐藏
 * @param sort 排序
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 * @param balance 显示余额
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Parcelize
data class AssetEntity(
    val id: Long,
    val booksId: Long,
    val name: String,
    val totalAmount: String,
    val billingDate: String,
    val repaymentDate: String,
    val type: ClassificationTypeEnum,
    val classification: AssetClassificationEnum,
    val invisible: Boolean,
    val sort: Int,
    val createTime: String,
    val modifyTime: String,
    val balance: String
) : Parcelable {

    /** 标记 - 是否是信用卡类型 */
    @IgnoredOnParcel
    val creditCardAccount: Boolean
        get() = type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT

    /** 信用卡消费进度 */
    @IgnoredOnParcel
    val progress: Int
        get() = if (!creditCardAccount || totalAmount.toFloat() <= 0f) {
            0
        } else {
            (balance.toBigDecimalOrZero() / totalAmount.toBigDecimalOrNull().orElse("1".toBigDecimal()) * "100".toBigDecimal()).toFloat().roundToInt()
        }

    /** 余额显示文本 */
    @IgnoredOnParcel
    val balanceStr: String
        get() = if (creditCardAccount) {
            // 信用卡类型，显示总额度
            totalAmount
        } else {
            // 其他类型，显示余额
            balance
        }.moneyFormat()

    /** 显示文本：支付宝（￥200）*/
    @IgnoredOnParcel
    val showStr: String
        get() = R.string.account_show_format.string.format(
            name, if (creditCardAccount) {
                // 信用卡，显示总额度减去已使用
                (totalAmount.toBigDecimalOrZero() - balance.toBigDecimalOrZero()).decimalFormat()
            } else {
                // 其他，显示余额
                balance
            }.moneyFormat()
        )

    /** 信用卡已用文本 */
    @IgnoredOnParcel
    val creditCardUsed: String
        get() = if (creditCardAccount) {
            R.string.used_with_colon_format.string.format(balance.moneyFormat())
        } else {
            ""
        }

    companion object {
        /** 创建新建资产数据 */
        fun newAsset(type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT, classification: AssetClassificationEnum = AssetClassificationEnum.CASH): AssetEntity {
            return AssetEntity(
                id = -1,
                booksId = CurrentBooksLiveData.booksId,
                name = "",
                totalAmount = "",
                billingDate = "",
                repaymentDate = "",
                type = type,
                classification = classification,
                false,
                -1,
                "",
                "",
                ""
            )
        }
    }
}