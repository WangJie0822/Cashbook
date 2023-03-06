package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import kotlinx.coroutines.flow.Flow

/**
 * 记录数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface RecordDao {

    @Query("SELECT * FROM db_record WHERE id=:recordId")
    suspend fun queryById(recordId:Long):RecordTable?
}