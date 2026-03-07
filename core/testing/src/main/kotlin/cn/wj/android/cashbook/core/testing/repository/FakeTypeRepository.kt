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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTypeRepository : TypeRepository {

    private val types = mutableListOf<RecordTypeModel>()
    private val needRelatedSet = mutableSetOf<Long>()
    private val reimburseTypeSet = mutableSetOf<Long>()
    private val refundTypeSet = mutableSetOf<Long>()
    private val creditPaymentTypeSet = mutableSetOf<Long>()

    private val _firstExpenditureTypeListData = MutableStateFlow<List<RecordTypeModel>>(emptyList())
    private val _firstIncomeTypeListData = MutableStateFlow<List<RecordTypeModel>>(emptyList())
    private val _firstTransferTypeListData = MutableStateFlow<List<RecordTypeModel>>(emptyList())

    override val firstExpenditureTypeListData: Flow<List<RecordTypeModel>> = _firstExpenditureTypeListData
    override val firstIncomeTypeListData: Flow<List<RecordTypeModel>> = _firstIncomeTypeListData
    override val firstTransferTypeListData: Flow<List<RecordTypeModel>> = _firstTransferTypeListData

    fun addType(type: RecordTypeModel) {
        types.add(type)
        updateFlows()
    }

    fun setNeedRelated(typeId: Long) {
        needRelatedSet.add(typeId)
    }

    fun setReimburse(typeId: Long) {
        reimburseTypeSet.add(typeId)
        needRelatedSet.add(typeId)
    }

    fun setRefund(typeId: Long) {
        refundTypeSet.add(typeId)
        needRelatedSet.add(typeId)
    }

    fun setCreditPayment(typeId: Long) {
        creditPaymentTypeSet.add(typeId)
    }

    private fun updateFlows() {
        _firstExpenditureTypeListData.value = types.filter {
            it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE && it.typeLevel == TypeLevelEnum.FIRST
        }
        _firstIncomeTypeListData.value = types.filter {
            it.typeCategory == RecordTypeCategoryEnum.INCOME && it.typeLevel == TypeLevelEnum.FIRST
        }
        _firstTransferTypeListData.value = types.filter {
            it.typeCategory == RecordTypeCategoryEnum.TRANSFER && it.typeLevel == TypeLevelEnum.FIRST
        }
    }

    override suspend fun getRecordTypeById(typeId: Long): RecordTypeModel? {
        return types.find { it.id == typeId }
    }

    override suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel {
        return types.first { it.id == typeId }
    }

    override suspend fun getNoNullDefaultRecordType(): RecordTypeModel {
        return types.firstOrNull { it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE }
            ?: types.first()
    }

    override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> {
        return types.filter { it.parentId == parentId && it.typeLevel == TypeLevelEnum.SECOND }
    }

    override suspend fun needRelated(typeId: Long): Boolean {
        return typeId in needRelatedSet
    }

    override suspend fun isReimburseType(typeId: Long): Boolean {
        return typeId in reimburseTypeSet
    }

    override suspend fun isRefundType(typeId: Long): Boolean {
        return typeId in refundTypeSet
    }

    override suspend fun setReimburseType(typeId: Long) {
        reimburseTypeSet.clear()
        reimburseTypeSet.add(typeId)
        needRelatedSet.add(typeId)
    }

    override suspend fun setRefundType(typeId: Long) {
        refundTypeSet.clear()
        refundTypeSet.add(typeId)
        needRelatedSet.add(typeId)
    }

    override suspend fun changeTypeToSecond(id: Long, parentId: Long) {
        val index = types.indexOfFirst { it.id == id }
        if (index >= 0) {
            types[index] = types[index].copy(parentId = parentId, typeLevel = TypeLevelEnum.SECOND)
            updateFlows()
        }
    }

    override suspend fun changeSecondTypeToFirst(id: Long) {
        val index = types.indexOfFirst { it.id == id }
        if (index >= 0) {
            types[index] = types[index].copy(parentId = -1L, typeLevel = TypeLevelEnum.FIRST)
            updateFlows()
        }
    }

    override suspend fun deleteById(id: Long) {
        types.removeAll { it.id == id }
        updateFlows()
    }

    override suspend fun countByName(name: String): Int {
        return types.count { it.name == name }
    }

    override suspend fun update(model: RecordTypeModel) {
        val index = types.indexOfFirst { it.id == model.id }
        if (index >= 0) {
            types[index] = model
        } else {
            types.add(model)
        }
        updateFlows()
    }

    override suspend fun generateSortById(id: Long, parentId: Long): Int {
        return types.filter { it.parentId == parentId }.size
    }

    override suspend fun isCreditPaymentType(typeId: Long): Boolean {
        return typeId in creditPaymentTypeSet
    }

    override suspend fun setCreditPaymentType(typeId: Long) {
        creditPaymentTypeSet.clear()
        creditPaymentTypeSet.add(typeId)
    }
}
