package cn.wj.android.cashbook.core.data.repository.fake

import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.model.RecordModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object FakeRecordRepository : RecordRepository {

    private var coroutineContext = Dispatchers.IO

    private val _recordListDate = MutableStateFlow(
        listOf(
            RecordModel(
                1L,
                1L,
                1L,
                1L,
                2L,
                "100",
                "10",
                "0",
                "",
                false,
                System.currentTimeMillis().dateFormat()
            ),
            RecordModel(
                2L,
                1L,
                1L,
                1L,
                2L,
                "100",
                "10",
                "0",
                "",
                false,
                System.currentTimeMillis().dateFormat()
            ),
            RecordModel(
                3L,
                1L,
                1L,
                1L,
                2L,
                "100",
                "10",
                "0",
                "",
                false,
                System.currentTimeMillis().dateFormat()
            ),
        )
    )

    override suspend fun queryById(recordId: Long): RecordModel? = withContext(coroutineContext) {
        _recordListDate.first().firstOrNull { it.id == recordId }
    }

    override suspend fun queryByTypeId(id: Long): List<RecordModel> {
        TODO("Not yet implemented")
    }

    override suspend fun queryRelatedById(recordId: Long): List<RecordModel> =
        withContext(coroutineContext) {
            _recordListDate.first().filter { it.id == recordId }
        }

    override suspend fun updateRecord(record: RecordModel, tagIdList: List<Long>) =
        withContext(coroutineContext) {

        }

    override suspend fun deleteRecord(recordId: Long) = withContext(coroutineContext) {

    }

    override suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int
    ): List<RecordModel> = withContext(coroutineContext) {
        _recordListDate.first()
    }

    override fun queryRecordByYearMonth(year: String, month: String): Flow<List<RecordModel>> {
        return _recordListDate
    }

    override suspend fun getDefaultRecord(typeId: Long): RecordModel =
        withContext(coroutineContext) {
            _recordListDate.first().first()
        }

    override suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long) {
        TODO("Not yet implemented")
    }
}