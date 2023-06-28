package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 我的资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/27
 */
@HiltViewModel
class MyAssetViewModel @Inject constructor(
    assetRepository: AssetRepository,
) : ViewModel() {

    private val assetListData  = assetRepository.currentVisibleAssetListData

    val assetTypedListData = assetListData
        .mapLatest { list ->
            val result = mutableListOf<AssetTypeViewsModel>()
            ClassificationTypeEnum.values().forEach { type ->
                val assetList = list.filter { it.type == type }
                if (assetList.isNotEmpty()) {
                    var totalAmount = BigDecimal.ZERO
                    assetList.forEach { asset ->
                        totalAmount += asset.balance.toBigDecimalOrZero()
                    }
                    result.add(
                        AssetTypeViewsModel(
                            name = type.name,
                            totalAmount = totalAmount.decimalFormat(),
                            assetList = assetList
                        )
                    )
                }
            }
            result
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = listOf(),
        )

}