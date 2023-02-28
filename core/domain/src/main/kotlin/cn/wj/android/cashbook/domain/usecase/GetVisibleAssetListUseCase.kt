package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.model.AssetModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVisibleAssetListUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {

    operator fun invoke(): Flow<List<AssetEntity>> {
        return appPreferencesDataSource.appData.map {
            assetRepository.getVisibleAssetsByBookId(it.currentBookId)
                .map {
                    it.asEntity()
                }
        }
    }

    private fun AssetModel.asEntity(): AssetEntity {
        return AssetEntity(
            id = this.id,
            booksId = this.booksId,
            name = this.name,
            iconResId = this.iconResId,
            totalAmount = this.totalAmount,
            billingDate = this.billingDate,
            repaymentDate = this.repaymentDate,
            type = this.type,
            classification = this.classification,
            invisible = this.invisible,
            openBank = this.openBank,
            cardNo = this.cardNo,
            remark = this.remark,
            sort = this.sort,
            modifyTime = this.modifyTime,
            balance = this.balance
        )
    }
}