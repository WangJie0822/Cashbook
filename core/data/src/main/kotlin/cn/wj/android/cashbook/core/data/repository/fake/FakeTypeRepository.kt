package cn.wj.android.cashbook.core.data.repository.fake

import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import kotlinx.coroutines.flow.Flow

object FakeTypeRepository : TypeRepository {
    override val firstExpenditureTypeListData: Flow<List<RecordTypeModel>>
        get() = TODO("Not yet implemented")
    override val firstIncomeTypeListData: Flow<List<RecordTypeModel>>
        get() = TODO("Not yet implemented")
    override val firstTransferTypeListData: Flow<List<RecordTypeModel>>
        get() = TODO("Not yet implemented")

    override suspend fun getRecordTypeById(typeId: Long): RecordTypeModel? {
        TODO("Not yet implemented")
    }

    override suspend fun getNoNullRecordTypeById(typeId: Long): RecordTypeModel {
        TODO("Not yet implemented")
    }

    override suspend fun getNoNullDefaultRecordType(): RecordTypeModel {
        TODO("Not yet implemented")
    }

    override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> {
        TODO("Not yet implemented")
    }

    override suspend fun needRelated(typeId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeTypeToSecond(id: Long, parentId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun changeSecondTypeToFirst(id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun countByName(name: String): Int {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: RecordTypeModel) {
        TODO("Not yet implemented")
    }

    override suspend fun generateSortById(id: Long, parentId: Long): Int {
        TODO("Not yet implemented")
    }
}