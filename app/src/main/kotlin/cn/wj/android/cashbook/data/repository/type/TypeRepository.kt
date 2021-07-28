package cn.wj.android.cashbook.data.repository.type

import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.table.TypeTable
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.entity.TypeIconGroupEntity
import cn.wj.android.cashbook.data.entity.getTypeIconGroupList
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.transform.toTypeEntity
import cn.wj.android.cashbook.data.transform.toTypeTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 类型相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
class TypeRepository(database: CashbookDatabase) : Repository(database) {

    /** 查询并返回记录类型为 [type] 的类型数据列表 */
    suspend fun getTypeListByType(type: RecordTypeEnum): List<TypeEntity> = withContext(Dispatchers.IO) {
        typeDao.queryByPosition(TypeEnum.FIRST.name, type.position).map { first ->
            val firstEntity = first.toTypeEntity(null)
            firstEntity.copy(
                childList = typeDao.queryByParentId(TypeEnum.SECOND.name, first.id.orElse(-1L)).map { second ->
                    second.toTypeEntity(firstEntity)
                }.sortedBy {
                    it.sort
                }
            )
        }.sortedBy {
            it.sort
        }
    }

    /** 获取分类图标数据 */
    suspend fun getTypeIconData(): List<TypeIconGroupEntity> = withContext(Dispatchers.IO) {
        getTypeIconGroupList()
    }

    /** 获取名称为 [name] 的分类数量 */
    suspend fun getTypeCountByName(name: String): Long = withContext(Dispatchers.IO) {
        typeDao.getCountByName(name)
    }

    /** 将 [type] 插入数据库并返回 id */
    suspend fun insertType(type: TypeEntity): Long = withContext(Dispatchers.IO) {
        typeDao.insert(type.toTypeTable())
    }

    /** 更新分类数据 [type] */
    suspend fun updateType(type: TypeEntity) = withContext(Dispatchers.IO) {
        typeDao.update(type.toTypeTable())
    }

    /** 获取数据库中分类数据数量 */
    suspend fun getTypeCount(): Long = withContext(Dispatchers.IO) {
        typeDao.getCount()
    }

    /** 获取分类为 [type] 的记录数量 */
    suspend fun getRecordCountByType(type: TypeEntity): Int = withContext(Dispatchers.IO) {
        recordDao.queryRecordCountByTypeId(type.id)
    }

    /** 查询并返回记录类型为 [type] 的类型数据列表 */
    suspend fun getReplaceTypeListByType(type: TypeEntity): List<TypeEntity> = withContext(Dispatchers.IO) {
        val ls = arrayListOf<TypeEntity>()
        typeDao.queryByPosition(TypeEnum.FIRST.name, type.recordType.position)
            .sortedBy {
                it.sort
            }
            .map { first ->
                val firstEntity = first.toTypeEntity(null)
                val secondLs = typeDao.queryByParentId(TypeEnum.SECOND.name, first.id.orElse(-1L)).map { second ->
                    second.toTypeEntity(firstEntity)
                }.sortedBy {
                    it.sort
                }
                ls.add(firstEntity.copy(childList = secondLs))
                secondLs.forEach {
                    ls.add(it)
                }
            }
        val index = ls.indexOfFirst { it.id == type.id }
        if (index >= 0) {
            ls.removeAt(index)
        }
        ls
    }

    /** 将记录中分类为 [old] 的记录分类修改为 [new] */
    suspend fun updateRecordTypes(old: TypeEntity, new: TypeEntity) = withContext(Dispatchers.IO) {
        recordDao.updateTypeId(old.id, new.id)
    }

    /** 从数据库中删除 [type] */
    suspend fun deleteType(type: TypeEntity) = withContext(Dispatchers.IO) {
        typeDao.delete(type.toTypeTable())
    }

    /** 更新分类数据 [types] */
    suspend fun updateTypes(types: List<TypeEntity>) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<TypeTable>()
        types.forEach {
            ls.add(it.toTypeTable())
        }
        typeDao.update(*ls.toTypedArray())
    }
}