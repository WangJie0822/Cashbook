package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.TypeTable
import kotlinx.coroutines.flow.Flow

/**
 * 记录类型数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface TypeDao {

    @Insert
    suspend fun insertType(type: TypeTable): Long

    @Insert
    suspend fun insertTypes(vararg types: TypeTable)

    @Query("SELECT * FROM db_type")
    fun queryAll(): Flow<List<TypeTable>>

    @Query("SELECT * FROM db_type WHERE type_category=:typeCategory")
    fun queryByTypeCategory(typeCategory: String): Flow<List<TypeTable>>

    @Query("SELECT * FROM db_type WHERE type_level=:typeLevel AND type_category=:typeCategory")
    suspend fun queryByLevelAndTypeCategory(typeLevel: String, typeCategory: String): List<TypeTable>

    @Query("SELECT * FROM db_type WHERE type_level=:typeLevel")
    suspend fun queryByLevel(typeLevel: String): List<TypeTable>

    @Query("SELECT * FROM db_type WHERE parent_id=:parentId")
    suspend fun queryByParentId(parentId: Long):List<TypeTable>

    @Query("SELECT * FROM db_type WHERE id=:typeId")
    suspend fun queryById(typeId: Long): TypeTable?
}