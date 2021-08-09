package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    /** 从数据库中删除 [type] */
    @Delete
    suspend fun delete(type: TypeTable)

    /** 清空数据表数据 */
    @Query("DELETE FROM db_type")
    suspend fun deleteAll()

    /** 更新类型数据 [types] */
    @Update
    suspend fun update(vararg types: TypeTable)

    /** 更新类型数据 [type] */
    @Update
    suspend fun update(type: TypeTable)

    /** 查询所有数据 */
    @Query("SELECT * FROM db_type")
    suspend fun queryAll(): List<TypeTable>

    /** 获取 name 是 [name] 的数据数量 */
    @Query("SELECT COUNT(*) FROM db_type WHERE name=:name")
    suspend fun getCountByName(name: String): Long

    /** 获取数据库中的数据数量 */
    @Query("SELECT COUNT(*) FROM db_type")
    suspend fun getCount(): Long

    /** 查询并返回类型为 [type] 记录类型为 [position] 的类型数据列表 */
    @Query("SELECT * FROM db_type WHERE type=:type AND record_type=:position ")
    suspend fun queryByPosition(type: String, position: Int): List<TypeTable>

    /** 查询并返回类型为 [type] 父类型id为 [parentId] 的二级级类型数据列表 */
    @Query("SELECT * FROM db_type WHERE type=:type AND parent_id=:parentId")
    suspend fun queryByParentId(type: String, parentId: Long): List<TypeTable>

    /** 获取 id 为 [id] 的类型 */
    @Query("SELECT * FROM db_type WHERE id=:id")
    suspend fun queryById(id: Long): TypeTable?
}