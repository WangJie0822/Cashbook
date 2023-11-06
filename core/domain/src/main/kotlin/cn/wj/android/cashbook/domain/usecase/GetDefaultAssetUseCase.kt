package cn.wj.android.cashbook.domain.usecase

import android.content.Context
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.helper.iconResId
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

/**
 * 获取默认记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetDefaultAssetUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
) {

    suspend operator fun invoke(assetId: Long): AssetModel {
        // appData 获取默认参数
        return assetRepository.getAssetById(assetId) ?: AssetModel(
            id = assetId,
            booksId = -1L,
            name = AssetClassificationEnum.CASH.nameResId.string(context),
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