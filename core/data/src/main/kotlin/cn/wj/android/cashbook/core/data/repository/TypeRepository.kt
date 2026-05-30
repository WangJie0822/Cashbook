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

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.orElse
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import kotlinx.coroutines.flow.Flow

/**
 * 记录类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
interface TypeRepository {

    val firstExpenditureTypeListData: Flow<List<RecordTypeModel>>

    val firstIncomeTypeListData: Flow<List<RecordTypeModel>>

    val firstTransferTypeListData: Flow<List<RecordTypeModel>>

    suspend fun getRecordTypeById(typeId: Long): RecordTypeModel?

    /**
     * 批量查询 [typeIds] 对应的类型分类，一次查询避免逐条 N+1。
     * 返回 typeId -> 分类的映射；库中不存在的类型（如平账合成类型 -1101/-1102）不在结果中，由调用方兜底解析。
     */
    suspend fun getRecordTypeCategories(typeIds: List<Long>): Map<Long, RecordTypeCategoryEnum>

    suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel

    suspend fun getNoNullDefaultRecordType(): RecordTypeModel

    suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel>

    suspend fun needRelated(typeId: Long): Boolean

    suspend fun isReimburseType(typeId: Long): Boolean

    suspend fun isRefundType(typeId: Long): Boolean

    suspend fun changeTypeToSecond(id: Long, parentId: Long)

    suspend fun changeSecondTypeToFirst(id: Long)

    suspend fun deleteById(id: Long)

    suspend fun countByName(name: String): Int

    suspend fun update(model: RecordTypeModel)

    suspend fun generateSortById(id: Long, parentId: Long): Int

    /** 按 [sortedIds] 顺序为一级分类写入连续的 sort（0,1,2...） */
    suspend fun updateFirstTypeSort(sortedIds: List<Long>)

    suspend fun isCreditPaymentType(typeId: Long): Boolean

    suspend fun migrateSpecialTypes()
}

internal fun TypeTable.asModel(needRelated: Boolean): RecordTypeModel {
    return RecordTypeModel(
        id = this.id.orElse(-1L),
        parentId = this.parentId,
        name = this.name,
        iconName = this.iconName,
        typeLevel = TypeLevelEnum.ordinalOf(this.typeLevel),
        typeCategory = RecordTypeCategoryEnum.ordinalOf(this.typeCategory),
        protected = this.protected == SWITCH_INT_ON,
        sort = this.sort,
        needRelated = needRelated,
    )
}

internal fun RecordTypeModel.asTable(): TypeTable {
    return TypeTable(
        id = if (this.id == -1L) null else this.id,
        parentId = this.parentId,
        name = this.name,
        iconName = this.iconName,
        typeLevel = this.typeLevel.ordinal,
        typeCategory = this.typeCategory.ordinal,
        protected = if (this.protected) SWITCH_INT_ON else SWITCH_INT_OFF,
        sort = this.sort,
    )
}
