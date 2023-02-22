package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记录数据库表
 *
 * @param id 主键自增长
 * @param typeId 类型 id
 * @param assetId 关联资产 id
 * @param intoAssetId 转账转入资产 id
 * @param booksId 关联账本 id
 * @param amount 金额
 * @param concessions 优惠
 * @param charge 手续费
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Entity(tableName = "db_record")
data class RecordTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "type_id") val typeId: Long,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    @ColumnInfo(name = "into_asset_id") val intoAssetId: Long,
    @ColumnInfo(name = "books_id") val booksId: Long,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "concessions") val concessions: Double,
    @ColumnInfo(name = "charge") val charge: Double,
    val remark: String,
    val reimbursable: Int,
    @ColumnInfo(name = "modify_time") val modifyTime: Long
)