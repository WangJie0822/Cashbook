package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
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

    /** 标记 - 是否显示更多弹窗 */
    var showMoreDialog by mutableStateOf(false)
        private set

    /**
     * 资产数据列表
     * - 按照资产大类进行分类
     */
    val assetTypedListData = assetRepository.currentVisibleAssetTypeData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = emptyList(),
        )

    val uiState = assetRepository.currentVisibleAssetListData
        .mapLatest { list ->
            var totalLiabilities = BigDecimal.ZERO
            var totalAsset = BigDecimal.ZERO
            list.forEach { assetModel ->
                if (assetModel.type.isCreditCard() || assetModel.classification == AssetClassificationEnum.BORROW) {
                    // 信用卡和债务借入
                    totalLiabilities += assetModel.balance.toBigDecimalOrZero()
                } else {
                    totalAsset += assetModel.balance.toBigDecimalOrZero()
                }

            }
            MyAssetUiState.Success(
                totalAsset = totalAsset.decimalFormat(),
                totalLiabilities = totalLiabilities.decimalFormat(),
                netAsset = (totalAsset - totalLiabilities).decimalFormat(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = MyAssetUiState.Loading,
        )

    fun displayShowMoreDialog() {
        showMoreDialog = true
    }

    fun dismissShowMoreDialog() {
        showMoreDialog = false
    }
}

sealed class MyAssetUiState {
    data object Loading : MyAssetUiState()
    data class Success(
        val totalAsset: String,
        val totalLiabilities: String,
        val netAsset: String,
    ) : MyAssetUiState()
}