package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.model.RecordModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 获取默认记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetDefaultRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {

    suspend operator fun invoke(
        recordId: Long,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): RecordModel = withContext(coroutineContext) {
        val result = recordRepository.queryById(recordId)
        if (null != result) {
            return@withContext result
        }
        // 没有查到对应记录，新建，创建默认记录数据
        val appDataModel = appPreferencesDataSource.appData.first()
        val recordTypeById = typeRepository.getNoNullRecordTypeById(appDataModel.defaultTypeId)
        RecordModel(
            id = -1L,
            booksId = appDataModel.currentBookId,
            typeId = recordTypeById.id,
            assetId = appDataModel.lastAssetId,
            relatedAssetId = -1L,
            amount = "0",
            charges = "",
            concessions = "",
            remark = "",
            reimbursable = false,
            recordTime = System.currentTimeMillis().dateFormat(DATE_FORMAT_NO_SECONDS),
        )
    }
}