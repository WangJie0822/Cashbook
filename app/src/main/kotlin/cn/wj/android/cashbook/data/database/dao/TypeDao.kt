package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.wj.android.cashbook.data.database.table.TypeTable

/**
 * 类型数据库访问接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
@Dao
interface TypeDao {

    /** 将 [type] 插入数据库并返回生成的 id */
    @Insert
    suspend fun insert(type: TypeTable): Long

    /** 将 [types] 插入数据库 */
    @Insert
    suspend fun insert(vararg types: TypeTable)

    /** 获取数据库中的数据数量 */
    @Query("SELECT COUNT(*) FROM db_type")
    suspend fun getCount(): Long
}