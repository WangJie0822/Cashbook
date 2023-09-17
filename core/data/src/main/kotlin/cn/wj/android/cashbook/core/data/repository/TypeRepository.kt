package cn.wj.android.cashbook.core.data.repository

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

    suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel

    suspend fun getNoNullDefaultRecordType(): RecordTypeModel

    suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel>

    suspend fun needRelated(typeId: Long): Boolean

    suspend fun changeTypeToSecond(id: Long, parentId: Long)

    suspend fun changeSecondTypeToFirst(id: Long)

    suspend fun deleteById(id: Long)
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