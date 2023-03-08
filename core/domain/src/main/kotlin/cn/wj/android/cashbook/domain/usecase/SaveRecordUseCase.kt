package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.transfer.asModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource,
) {

    suspend operator fun invoke(recordEntity: RecordEntity, tags: List<TagEntity>) =
        withContext(Dispatchers.IO) {
            // 向数据库内更新最新记录信息及关联信息
            recordRepository.updateRecord(recordEntity.asModel(), tags.map { it.asModel() })
            // 更新上次使用的默认数据
            appPreferencesDataSource.updateLastAssetId(recordEntity.assetId)
        }
}