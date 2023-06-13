package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.model.typeDataVersion
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/**
 * 记录类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TypeRepositoryImpl @Inject constructor(
    private val typeDao: TypeDao,
    private val appPreferencesDataSource: AppPreferencesDataSource,
) : TypeRepository {

    private val firstTypeListData: Flow<List<RecordTypeModel>> = typeDataVersion.mapLatest {
        getFirstRecordTypeList()
    }

    override val firstExpenditureTypeListData: Flow<List<RecordTypeModel>> =
        firstTypeListData.mapLatest { list ->
            list.filter { it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE }
        }

    override val firstIncomeTypeListData: Flow<List<RecordTypeModel>> =
        firstTypeListData.mapLatest { list ->
            list.filter { it.typeCategory == RecordTypeCategoryEnum.INCOME }
        }

    override val firstTransferTypeListData: Flow<List<RecordTypeModel>> =
        firstTypeListData.mapLatest { list ->
            list.filter { it.typeCategory == RecordTypeCategoryEnum.TRANSFER }
        }

    override suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel =
        withContext(Dispatchers.IO) {
            typeDao.queryById(typeId)?.asModel(appPreferencesDataSource.needRelated(typeId))
                ?: getFirstRecordTypeListByCategory(RecordTypeCategoryEnum.EXPENDITURE)
                    .first()
        }

    override suspend fun getFirstRecordTypeListByCategory(typeCategory: RecordTypeCategoryEnum): List<RecordTypeModel> =
        withContext(Dispatchers.IO) {
            typeDao.queryByLevelAndTypeCategory(TypeLevelEnum.FIRST.name, typeCategory.name)
                .map {
                    it.asModel(appPreferencesDataSource.needRelated(it.id ?: -1L))
                }
        }

    private suspend fun getFirstRecordTypeList(): List<RecordTypeModel> =
        withContext(Dispatchers.IO) {
            val result = typeDao.queryByLevel(TypeLevelEnum.FIRST.name)
                .map {
                    it.asModel(appPreferencesDataSource.needRelated(it.id ?: -1L))
                }
            result
        }

    override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> =
        withContext(Dispatchers.IO) {
            typeDao.queryByParentId(parentId)
                .map {
                    it.asModel(appPreferencesDataSource.needRelated(it.id ?: -1L))
                }
        }
}