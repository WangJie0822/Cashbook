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
import kotlinx.coroutines.flow.combine
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

    /**
     * 总资产
     * - 资金账户 + 充值账户 + 投资理财 + 账务借出
     */
    val totalAsset = assetRepository.currentVisibleAssetListData
        .mapLatest { list ->
            var total = BigDecimal.ZERO
            list.filterNot { assetModel ->
                // 排除信用卡和账务借入
                assetModel.type.isCreditCard() || assetModel.classification == AssetClassificationEnum.BORROW
            }.forEach { assetModel ->
                total += assetModel.balance.toBigDecimalOrZero()
            }
            total.decimalFormat()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = "0",
        )

    /**
     * 总负债
     * - 信用卡已用 + 债务借入
     */
    val totalLiabilities = assetRepository.currentVisibleAssetListData
        .mapLatest { list ->
            var total = BigDecimal.ZERO
            list.filter { assetModel ->
                // 只计算信用卡和债务借入
                assetModel.type.isCreditCard() || assetModel.classification == AssetClassificationEnum.BORROW
            }.forEach { assetModel ->
                total += if (assetModel.type.isCreditCard()) {
                    // 信用卡，总额度 - 可用
                    (assetModel.totalAmount.toBigDecimalOrZero() - assetModel.balance.toBigDecimalOrZero())
                } else {
                    assetModel.balance.toBigDecimalOrZero()
                }
            }
            total.decimalFormat()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = "0",
        )

    /**
     * 净资产
     * - 总资产 - 总负债
     */
    val netAsset = combine(totalAsset, totalLiabilities) { totalAsset, totalLiabilities ->
        (totalAsset.toBigDecimalOrZero() - totalLiabilities.toBigDecimalOrZero()).decimalFormat()
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = "0",
        )

    /**
     * 资产数据列表
     * - 按照资产大类进行分类
     */
    val assetTypedListData = assetRepository.currentVisibleAssetTypeData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = listOf(),
        )

    fun displayShowMoreDialog() {
        showMoreDialog = true
    }

    fun dismissShowMoreDialog() {
        showMoreDialog = false
    }
}