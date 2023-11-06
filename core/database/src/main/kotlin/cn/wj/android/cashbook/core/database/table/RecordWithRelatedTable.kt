package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记录关联关系表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/6
 */
@Entity(tableName = TABLE_RECORD_RELATED)
class RecordWithRelatedTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_RECORD_RELATED_ID) val id: Long?,
    @ColumnInfo(name = TABLE_RECORD_RELATED_RECORD_ID) val recordId: Long,
    @ColumnInfo(name = TABLE_RECORD_RELATED_RELATED_RECORD_ID) val relatedRecordId: Long,
)