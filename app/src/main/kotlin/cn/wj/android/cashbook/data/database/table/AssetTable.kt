package cn.wj.android.cashbook.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.serialization.Serializable

/**
 * 资产信息数据库表
 *
 * @param id 资产主键，自增长
 * @param booksId 所属账本主键
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
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Serializable
@Entity(tableName = "db_asset")
data class AssetTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "books_id") val booksId: Long,
    val name: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Float,
    @ColumnInfo(name = "billing_date") val billingDate: String,
    @ColumnInfo(name = "repayment_date") val repaymentDate: String,
    val type: String,
    val classification: String,
    val invisible: Int,
    val sort: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "modify_time") val modifyTime: Long
) {

    /** 信用卡、借入类型计算余额时需要取反 */
    @IgnoredOnParcel
    val needNegative: Boolean
        get() = type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name || classification == AssetClassificationEnum.BORROW.name
}