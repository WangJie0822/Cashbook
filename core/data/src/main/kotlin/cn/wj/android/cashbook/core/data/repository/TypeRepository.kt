package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.orElse
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 记录类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
class TypeRepository @Inject constructor(
    private val typeDao: TypeDao,
) {

    suspend fun getRecordTypeById(typeId: Long): RecordTypeModel? = withContext(Dispatchers.IO) {
        typeDao.queryById(typeId)?.asModel()
    }


    suspend fun getFirstRecordTypeListByCategory(typeCategory: RecordTypeCategoryEnum): List<RecordTypeModel> =
        withContext(Dispatchers.IO) {
            typeDao.queryByLevelAndTypeCategory(TypeLevelEnum.FIRST.name, typeCategory.name)
                .map {
                    it.asModel()
                }
        }

    suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> =
        withContext(Dispatchers.IO) {
            typeDao.queryByParentId(parentId)
                .map {
                    it.asModel(parentId)
                }
        }

    fun getRecordTypeListByCategory(typeCategory: RecordTypeCategoryEnum): Flow<List<RecordTypeModel>> {
        // 查询类型数据并返回
        return typeDao.queryByTypeCategory(typeCategory.name)
            .map { list ->
                list.map {
                    it.asModel()
                }
            }
    }
}

private fun TypeTable.asModel(parentId: Long = -1L): RecordTypeModel {
    return RecordTypeModel(
        id = this.id.orElse(-1L),
        parentId = parentId,
        name = this.name,
        iconName = this.iconName,
        typeLevel = TypeLevelEnum.valueOf(this.typeLevel),
        typeCategory = RecordTypeCategoryEnum.valueOf(this.typeCategory),
        protected = this.protected == SWITCH_INT_ON,
        sort = this.sort,
    )
}