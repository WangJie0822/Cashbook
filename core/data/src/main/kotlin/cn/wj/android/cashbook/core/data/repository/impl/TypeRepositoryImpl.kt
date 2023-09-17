package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.typeDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : TypeRepository {

    override val firstExpenditureTypeListData: Flow<List<RecordTypeModel>> =
        typeDataVersion.mapLatest {
            getFirstRecordTypeList().filter { it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE }
        }

    override val firstIncomeTypeListData: Flow<List<RecordTypeModel>> = typeDataVersion.mapLatest {
        getFirstRecordTypeList().filter { it.typeCategory == RecordTypeCategoryEnum.INCOME }
    }

    override val firstTransferTypeListData: Flow<List<RecordTypeModel>> =
        typeDataVersion.mapLatest {
            getFirstRecordTypeList().filter { it.typeCategory == RecordTypeCategoryEnum.TRANSFER }
        }

    override suspend fun getRecordTypeById(typeId: Long): RecordTypeModel? =
        withContext(coroutineContext) {
            typeDao.queryById(typeId)?.asModel(appPreferencesDataSource.needRelated(typeId))
        }

    override suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel =
        withContext(coroutineContext) {
            getRecordTypeById(typeId)
                ?: firstExpenditureTypeListData.first().first()
        }

    override suspend fun getNoNullDefaultRecordType(): RecordTypeModel =
        withContext(coroutineContext) {
            getNoNullRecordTypeById(appPreferencesDataSource.appData.first().defaultTypeId)
        }

    private suspend fun getFirstRecordTypeList(): List<RecordTypeModel> =
        withContext(coroutineContext) {
            val result = typeDao.queryByLevel(TypeLevelEnum.FIRST.ordinal)
                .map {
                    it.asModel(appPreferencesDataSource.needRelated(it.id ?: -1L))
                }
            result
        }

    override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> =
        withContext(coroutineContext) {
            typeDao.queryByParentId(parentId)
                .map {
                    it.asModel(appPreferencesDataSource.needRelated(it.id ?: -1L))
                }
        }

    override suspend fun needRelated(typeId: Long): Boolean =
        withContext(coroutineContext) {
            val appDataModel = appPreferencesDataSource.appData.first()
            typeId == appDataModel.refundTypeId || typeId == appDataModel.reimburseTypeId
        }

    override suspend fun changeTypeToSecond(id: Long, parentId: Long): Unit =
        withContext(coroutineContext) {
            typeDao.updateTypeLevel(
                id = id,
                parentId = parentId,
                typeLevel = TypeLevelEnum.SECOND.ordinal
            )
            typeDataVersion.updateVersion()
        }

    override suspend fun changeSecondTypeToFirst(id: Long): Unit =
        withContext(coroutineContext) {
            typeDao.updateTypeLevel(
                id = id,
                parentId = -1L,
                typeLevel = TypeLevelEnum.FIRST.ordinal
            )
            typeDataVersion.updateVersion()
        }

    override suspend fun deleteById(id: Long): Unit =
        withContext(coroutineContext) {
            typeDao.deleteById(id)
            typeDataVersion.updateVersion()
        }
}