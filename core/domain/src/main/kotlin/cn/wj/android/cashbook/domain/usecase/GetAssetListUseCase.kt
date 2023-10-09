package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.model.AssetModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetAssetListUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
) {

    operator fun invoke(
        currentTypeId: Long,
        selectedAssetId: Long,
        isRelated: Boolean,
    ): Flow<List<AssetModel>> {
        // TODO 不同类型添加判断筛选排序
        return assetRepository.currentVisibleAssetListData
    }
}