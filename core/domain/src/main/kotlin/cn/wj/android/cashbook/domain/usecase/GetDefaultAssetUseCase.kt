package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.helper.iconResId
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.transfer.asEntity
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 获取默认记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetDefaultAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {

    operator fun invoke(assetId: Long): Flow<AssetEntity> {
        // appData 获取默认参数
        return appPreferencesDataSource.appData.map { appDataModel ->
            assetRepository.getAssetById(assetId)?.asEntity() ?: AssetEntity(
                id = assetId,
                booksId = appDataModel.currentBookId,
                name = AssetClassificationEnum.CASH.nameResId.string,
                iconResId = AssetClassificationEnum.CASH.iconResId,
                totalAmount = "",
                billingDate = "",
                repaymentDate = "",
                type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
                classification = AssetClassificationEnum.CASH,
                invisible = false,
                openBank = "",
                cardNo = "",
                remark = "",
                sort = 0,
                modifyTime = Date().dateFormat(),
                balance = "",
            )
        }
    }
}