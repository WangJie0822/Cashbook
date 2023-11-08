/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.typeDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

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

    override suspend fun needRelated(typeId: Long): Boolean = withContext(coroutineContext) {
        val appDataModel = appPreferencesDataSource.appData.first()
        val refundTypeId = if (appDataModel.refundTypeId > 0L) {
            appDataModel.refundTypeId
        } else {
            val id = typeDao.queryByName("退款")?.id ?: 0L
            if (id > 0L) {
                appPreferencesDataSource.updateRefundTypeId(id)
            }
            id
        }
        val reimburseTypeId = if (appDataModel.reimburseTypeId > 0L) {
            appDataModel.reimburseTypeId
        } else {
            val id = typeDao.queryByName("报销")?.id ?: 0L
            if (id > 0L) {
                appPreferencesDataSource.updateReimburseTypeId(id)
            }
            id
        }
        typeId == refundTypeId || typeId == reimburseTypeId
    }

    override suspend fun isReimburseType(typeId: Long): Boolean = withContext(coroutineContext) {
        val appDataModel = appPreferencesDataSource.appData.first()
        val reimburseTypeId = if (appDataModel.reimburseTypeId > 0L) {
            appDataModel.reimburseTypeId
        } else {
            val id = typeDao.queryByName("报销")?.id ?: 0L
            if (id > 0L) {
                appPreferencesDataSource.updateReimburseTypeId(id)
            }
            id
        }
        typeId == reimburseTypeId
    }

    override suspend fun isRefundType(typeId: Long): Boolean = withContext(coroutineContext) {
        val appDataModel = appPreferencesDataSource.appData.first()
        val refundTypeId = if (appDataModel.refundTypeId > 0L) {
            appDataModel.refundTypeId
        } else {
            val id = typeDao.queryByName("退款")?.id ?: 0L
            if (id > 0L) {
                appPreferencesDataSource.updateRefundTypeId(id)
            }
            id
        }
        typeId == refundTypeId
    }

    override suspend fun setReimburseType(typeId: Long): Unit = withContext(coroutineContext) {
        appPreferencesDataSource.updateReimburseTypeId(typeId)
    }

    override suspend fun setRefundType(typeId: Long): Unit = withContext(coroutineContext) {
        appPreferencesDataSource.updateRefundTypeId(typeId)
    }

    override suspend fun changeTypeToSecond(id: Long, parentId: Long): Unit =
        withContext(coroutineContext) {
            typeDao.updateTypeLevel(
                id = id,
                parentId = parentId,
                typeLevel = TypeLevelEnum.SECOND.ordinal,
            )
            typeDataVersion.updateVersion()
        }

    override suspend fun changeSecondTypeToFirst(id: Long): Unit = withContext(coroutineContext) {
        typeDao.updateTypeLevel(
            id = id,
            parentId = -1L,
            typeLevel = TypeLevelEnum.FIRST.ordinal,
        )
        typeDataVersion.updateVersion()
    }

    override suspend fun deleteById(id: Long): Unit = withContext(coroutineContext) {
        typeDao.deleteById(id)
        typeDataVersion.updateVersion()
    }

    override suspend fun countByName(name: String): Int = withContext(coroutineContext) {
        typeDao.countByName(name)
    }

    override suspend fun update(model: RecordTypeModel): Unit = withContext(coroutineContext) {
        typeDao.insertOrReplace(model.asTable())
        typeDataVersion.updateVersion()
    }

    override suspend fun generateSortById(id: Long, parentId: Long): Int =
        withContext(coroutineContext) {
            var sort = getRecordTypeById(id)?.sort ?: -1
            if (sort == -1) {
                sort = if (parentId == -1L) {
                    typeDao.countByLevel(TypeLevelEnum.FIRST.ordinal) + 1
                } else {
                    val parentSort = getRecordTypeById(parentId)?.sort ?: (
                        typeDao.countByLevel(
                            TypeLevelEnum.FIRST.ordinal,
                        ) + 1
                        )
                    parentSort * 1000 + typeDao.countByParentId(parentId)
                }
            }
            sort
        }

    override suspend fun isCreditPaymentType(typeId: Long): Boolean =
        withContext(coroutineContext) {
            val appDataModel = appPreferencesDataSource.appData.first()
            val creditCardPaymentTypeId = if (appDataModel.creditCardPaymentTypeId > 0L) {
                appDataModel.creditCardPaymentTypeId
            } else {
                val id = typeDao.queryByName("还信用卡")?.id ?: 0L
                if (id > 0L) {
                    appPreferencesDataSource.updateCreditCardPaymentTypeId(id)
                }
                id
            }
            creditCardPaymentTypeId == typeId
        }

    override suspend fun setCreditPaymentType(typeId: Long): Unit = withContext(coroutineContext) {
        appPreferencesDataSource.updateCreditCardPaymentTypeId(typeId)
    }
}
