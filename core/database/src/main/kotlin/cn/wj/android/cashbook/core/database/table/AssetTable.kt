package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 资产信息数据库表
 *
 * @param id 资产主键，自增长
 * @param booksId 所属账本主键
 * @param name 资产名称
 * @param balance 资产余额，信用卡为已使用额度
 * @param totalAmount 总额度，信用卡使用
 * @param billingDate 账单日，信用卡使用
 * @param repaymentDate 还款日，信用卡使用
 * @param type 资产大类
 * @param classification 资产分类
 * @param invisible 是否隐藏
 * @param openBank 开户行
 * @param cardNo 卡号
 * @param remark 备注
 * @param sort 排序
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Entity(tableName = TABLE_ASSET)
data class AssetTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_ASSET_ID) val id: Long?,
    @ColumnInfo(name = TABLE_ASSET_BOOKS_ID) val booksId: Long,
    @ColumnInfo(name = TABLE_ASSET_NAME) val name: String,
    @ColumnInfo(name = TABLE_ASSET_BALANCE) val balance: Double,
    @ColumnInfo(name = TABLE_ASSET_TOTAL_AMOUNT) val totalAmount: Double,
    @ColumnInfo(name = TABLE_ASSET_BILLING_DATE) val billingDate: String,
    @ColumnInfo(name = TABLE_ASSET_REPAYMENT_DATE) val repaymentDate: String,
    @ColumnInfo(name = TABLE_ASSET_TYPE) val type: Int,
    @ColumnInfo(name = TABLE_ASSET_CLASSIFICATION) val classification: Int,
    @ColumnInfo(name = TABLE_ASSET_INVISIBLE) val invisible: Int,
    @ColumnInfo(name = TABLE_ASSET_OPEN_BANK) val openBank: String,
    @ColumnInfo(name = TABLE_ASSET_CARD_NO) val cardNo: String,
    @ColumnInfo(name = TABLE_ASSET_REMARK) val remark: String,
    @ColumnInfo(name = TABLE_ASSET_SORT) val sort: Int,
    @ColumnInfo(name = TABLE_ASSET_MODIFY_TIME) val modifyTime: Long,
)