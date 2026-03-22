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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_CREDIT_CARD_PAYMENT
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.typeDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.datastore.datasource.CombineProtoDataSource
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
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
class TypeRepositoryImpl @Inject constructor(
    private val typeDao: TypeDao,
    private val transactionDao: TransactionDao,
    private val combineProtoDataSource: CombineProtoDataSource,
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
            typeDao.queryById(typeId)?.asModel(
                typeId == FIXED_TYPE_ID_REFUND || typeId == FIXED_TYPE_ID_REIMBURSE,
            )
        }

    override suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel =
        withContext(coroutineContext) {
            getRecordTypeById(typeId)
                ?: firstExpenditureTypeListData.first().first()
        }

    override suspend fun getNoNullDefaultRecordType(): RecordTypeModel =
        withContext(coroutineContext) {
            getNoNullRecordTypeById(combineProtoDataSource.recordSettingsData.first().defaultTypeId)
        }

    private suspend fun getFirstRecordTypeList(): List<RecordTypeModel> =
        withContext(coroutineContext) {
            typeDao.queryByLevel(TypeLevelEnum.FIRST.ordinal)
                .map {
                    val id = it.id ?: -1L
                    it.asModel(id == FIXED_TYPE_ID_REFUND || id == FIXED_TYPE_ID_REIMBURSE)
                }
        }

    override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> =
        withContext(coroutineContext) {
            typeDao.queryByParentId(parentId)
                .map {
                    val id = it.id ?: -1L
                    it.asModel(id == FIXED_TYPE_ID_REFUND || id == FIXED_TYPE_ID_REIMBURSE)
                }
        }

    override suspend fun needRelated(typeId: Long): Boolean = withContext(coroutineContext) {
        typeId == FIXED_TYPE_ID_REFUND || typeId == FIXED_TYPE_ID_REIMBURSE
    }

    override suspend fun isReimburseType(typeId: Long): Boolean = withContext(coroutineContext) {
        typeId == FIXED_TYPE_ID_REIMBURSE
    }

    override suspend fun isRefundType(typeId: Long): Boolean = withContext(coroutineContext) {
        typeId == FIXED_TYPE_ID_REFUND
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
        // 固定类型不允许删除
        require(id != FIXED_TYPE_ID_REFUND && id != FIXED_TYPE_ID_REIMBURSE && id != FIXED_TYPE_ID_CREDIT_CARD_PAYMENT) {
            "Cannot delete fixed type: $id"
        }
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
                    val parentSort = getRecordTypeById(parentId)?.sort
                        ?: (typeDao.countByLevel(TypeLevelEnum.FIRST.ordinal) + 1)
                    parentSort * 1000 + typeDao.countByParentId(parentId)
                }
            }
            sort
        }

    override suspend fun isCreditPaymentType(typeId: Long): Boolean =
        withContext(coroutineContext) {
            FIXED_TYPE_ID_CREDIT_CARD_PAYMENT == typeId
        }

    /**
     * 应用层一次性迁移：将旧的特殊类型记录引用迁移到固定 ID
     * 设计为幂等操作，崩溃后重试安全
     */
    override suspend fun migrateSpecialTypes(): Unit = withContext(coroutineContext) {
        val settings = combineProtoDataSource.recordSettingsData.first()

        migrateOneType(
            oldTypeId = settings.refundTypeId,
            fixedTypeId = FIXED_TYPE_ID_REFUND,
            fallbackName = "退款",
            fallbackCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            updateDataStore = { combineProtoDataSource.updateRefundTypeId(it) },
        )
        migrateOneType(
            oldTypeId = settings.reimburseTypeId,
            fixedTypeId = FIXED_TYPE_ID_REIMBURSE,
            fallbackName = "报销",
            fallbackCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            updateDataStore = { combineProtoDataSource.updateReimburseTypeId(it) },
        )
        migrateOneType(
            oldTypeId = settings.creditCardPaymentTypeId,
            fixedTypeId = FIXED_TYPE_ID_CREDIT_CARD_PAYMENT,
            fallbackName = "还信用卡",
            fallbackCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
            updateDataStore = { combineProtoDataSource.updateCreditCardPaymentTypeId(it) },
        )
    }

    private suspend fun migrateOneType(
        oldTypeId: Long,
        fixedTypeId: Long,
        fallbackName: String,
        fallbackCategory: Int,
        updateDataStore: suspend (Long) -> Unit,
    ) {
        if (oldTypeId == fixedTypeId) return // 已迁移

        val targetOldId = if (oldTypeId > 0L) {
            oldTypeId
        } else {
            // DataStore 无记录，按名称查找
            typeDao.queryByName(fallbackName)
                ?.takeIf { it.typeCategory == fallbackCategory }
                ?.id ?: 0L
        }

        if (targetOldId > 0L) {
            // 在事务中执行数据库操作
            transactionDao.migrateTypeRecords(targetOldId, fixedTypeId)
        }

        // 更新 DataStore（数据库事务之后，幂等安全）
        updateDataStore(fixedTypeId)
    }
}
