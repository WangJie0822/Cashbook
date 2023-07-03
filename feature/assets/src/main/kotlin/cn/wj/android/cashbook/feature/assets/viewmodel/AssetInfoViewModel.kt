package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 资产信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/30
 */
@HiltViewModel
class AssetInfoViewModel @Inject constructor(
    assetRepository: AssetRepository,
) : ViewModel() {

    /** 当前资产 id */
    private val assetId = MutableStateFlow(-1L)

    /** 当前资产信息 */
    private val currentAssetInfo = assetId.mapLatest { assetRepository.getAssetById(it) }

    /** 标记 - 是否是信用卡 */
    val isCreditCard = currentAssetInfo.mapLatest {
        it?.type?.isCreditCard() ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = false,
    )

    /** 资产名 */
    val assetName = currentAssetInfo.mapLatest {
        it?.name.orEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = "",
    )

    /** 资产余额或已使用额度 */
    val balance = currentAssetInfo.mapLatest {
        it?.balance ?: "0"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = "0",
    )

    /** 总额度 */
    val totalAmount = currentAssetInfo.mapLatest {
        it?.totalAmount ?: "0"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = "0",
    )

    /** 账单日 */
    val billingDate = currentAssetInfo.mapLatest {
        it?.billingDate.orEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = "",
    )

    /** 还款日 */
    val repaymentDate = currentAssetInfo.mapLatest {
        it?.repaymentDate.orEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = "",
    )

    fun updateAssetId(id: Long) {
        assetId.tryEmit(id)
    }
}