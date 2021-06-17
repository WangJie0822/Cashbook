package cn.wj.android.cashbook.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.wj.android.cashbook.base.ext.base.toMoneyFloat
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 记录数据库表
 *
 * @param id 主键自增长
 * @param type 记录类型
 * @param firstTypeId 记录一级分类id
 * @param secondTypeId 记录二级分类id
 * @param assetId 关联资产 id
 * @param intoAssetId 转账转入资产 id
 * @param booksId 关联账本 id
 * @param recordId 关联记录 id - 退款使用
 * @param amount 记录金额
 * @param charge 转账手续费
 * @param remark 备注
 * @param tagIds 标签 id
 * @param reimbursable 能否报销
 * @param system 是否是系统记录
 * @param recordTime 记录时间
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Entity(tableName = "db_record")
data class RecordTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val type: String,
    @ColumnInfo(name = "first_type_id") val firstTypeId: Long,
    @ColumnInfo(name = "second_type_id") val secondTypeId: Long,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    @ColumnInfo(name = "into_asset_id") val intoAssetId: Long,
    @ColumnInfo(name = "books_id") val booksId: Long,
    @ColumnInfo(name = "record_id") val recordId: Long,
    val amount: Float,
    val charge: Float,
    val remark: String,
    @ColumnInfo(name = "tag_ids") val tagIds: String,
    val reimbursable: Int,
    val system: Int,
    @ColumnInfo(name = "record_time") val recordTime: Long,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "modify_time") val modifyTime: Long
) {

    companion object {
        fun newModifyBalance(assetId: Long, balance: String, remark: String = "", system: Boolean = false): RecordTable {
            val ms = System.currentTimeMillis()
            return RecordTable(
                id = null,
                type = RecordTypeEnum.MODIFY_BALANCE.name,
                firstTypeId = -1L,
                secondTypeId = -1L,
                assetId = assetId,
                intoAssetId = -1L,
                booksId = CurrentBooksLiveData.booksId,
                recordId = -1L,
                amount = balance.toMoneyFloat(),
                charge = 0f,
                remark = remark,
                tagIds = "",
                reimbursable = SWITCH_INT_OFF,
                system = if (system) SWITCH_INT_ON else SWITCH_INT_OFF,
                recordTime = if (system) 0L else ms,
                createTime = ms,
                modifyTime = ms
            )
        }
    }
}