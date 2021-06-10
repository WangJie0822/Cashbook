package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import kotlin.math.roundToInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
            (balance.toFloat() / totalAmount.toFloat() * 100).roundToInt()
        }

    /** 余额显示文本 */
    @IgnoredOnParcel
    val balanceStr: String
        get() {
            val symbol = if (classification == AssetClassificationEnum.NOT_SELECT) {
                ""
            } else {
                CurrentBooksLiveData.currency.orElse(CurrencyEnum.CNY).symbol
            }
            val num = if (creditCardAccount) {
                // 信用卡类型，显示总额度
                totalAmount
            } else {
                // 其他类型，显示余额
                balance
            }
            return symbol + num
        }

    /** 显示文本：支付宝（￥200）*/
    val showStr: String
        get() = R.string.account_show_format.string.format(name, balanceStr)

    /** 信用卡已用文本 */
    @IgnoredOnParcel
    val creditCardUsed: String
        get() = if (creditCardAccount) {
            R.string.used_with_colon_format.string.format("${CurrentBooksLiveData.currency.orElse(CurrencyEnum.CNY).symbol}$balance")
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

        /** 不选择账户资产 */
        fun notSelectAsset(): AssetEntity {
            return AssetEntity(
                id = -1L,
                booksId = CurrentBooksLiveData.booksId,
                name = R.string.do_not_select_account.string,
                totalAmount = "",
                billingDate = "",
                repaymentDate = "",
                type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
                classification = AssetClassificationEnum.NOT_SELECT,
                false,
                -1,
                "",
                "",
                ""
            )
        }
    }
}