@file:OptIn(ExperimentalCoroutinesApi::class)

package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetDefaultAssetUseCase
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import cn.wj.android.cashbook.feature.assets.enums.SelectDayEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditAssetViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    getDefaultAssetUseCase: GetDefaultAssetUseCase,
) : ViewModel() {

    /** 底部 Sheet 类型 */
    var bottomSheetData by mutableStateOf(EditAssetBottomSheetEnum.DISMISS)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 资产 id */
    private val assetIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)

    /** 显示的资产信息 */
    private val _mutableAssetInfo = MutableStateFlow<AssetModel?>(null)
    private val defaultAssetInfo = assetIdData.mapLatest { getDefaultAssetUseCase(it) }
    private val displayAssetInfo =
        combine(_mutableAssetInfo, defaultAssetInfo) { mutable, default ->
            mutable ?: default
        }

    val uiState = displayAssetInfo
        .mapLatest {
            EditAssetUiState.Success(
                isCreditCard = it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
                classification = it.classification,
                assetName = it.name,
                totalAmount = it.totalAmount,
                balance = it.balance,
                openBank = it.openBank,
                cardNo = it.cardNo,
                remark = it.remark,
                billingDate = it.billingDate,
                repaymentDate = it.repaymentDate,
                invisible = it.invisible,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = EditAssetUiState.Loading,
        )

    fun updateAssetId(id: Long) {
        assetIdData.tryEmit(id)
    }

    private var typeTemp: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT

    fun showSelectClassificationSheet() {
        bottomSheetData = EditAssetBottomSheetEnum.CLASSIFICATION_TYPE
    }

    fun onClassificationChange(
        type: ClassificationTypeEnum?,
        classification: AssetClassificationEnum
    ) {
        if (null != type) {
            typeTemp = type
        }
        if (classification.isBankCard) {
            // 银行卡、信用卡类型，继续选择银行
            bottomSheetData = EditAssetBottomSheetEnum.ASSET_CLASSIFICATION
        } else {
            // 其它类型，保存
            viewModelScope.launch {
                _mutableAssetInfo.tryEmit(
                    displayAssetInfo.first().copy(type = typeTemp, classification = classification)
                )
            }
            dismissBottomSheet()
        }
    }

    fun onBillingDateClick() {
        dialogState = DialogState.Shown(SelectDayEnum.BILLING_DATE)
    }

    fun onRepaymentDateClick() {
        dialogState = DialogState.Shown(SelectDayEnum.REPAYMENT_DATE)
    }

    fun onDaySelect(day: String) {
        viewModelScope.launch {
            (dialogState as? DialogState.Shown<*>)?.let { state ->
                val assetEntity = if (state.data == SelectDayEnum.BILLING_DATE) {
                    displayAssetInfo.first().copy(billingDate = day)
                } else {
                    displayAssetInfo.first().copy(repaymentDate = day)
                }
                _mutableAssetInfo.tryEmit(assetEntity)
            }
            dismissDialog()
        }

    }

    fun onInvisibleChange(invisible: Boolean) {
        viewModelScope.launch {
            _mutableAssetInfo.tryEmit(displayAssetInfo.first().copy(invisible = invisible))
        }
    }

    fun dismissBottomSheet() {
        bottomSheetData = EditAssetBottomSheetEnum.DISMISS
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun save(
        assetName: String,
        totalAmount: String,
        balance: String,
        openBank: String,
        cardNo: String,
        remark: String,
        onSuccess: () -> Unit,
    ) {
        // TODO 修改余额平账功能
        viewModelScope.launch {
            try {
                val assetInfo = displayAssetInfo.first().copy(
                    name = assetName,
                    totalAmount = totalAmount,
                    balance = balance,
                    openBank = openBank,
                    cardNo = cardNo,
                    remark = remark,
                )
                assetRepository.updateAsset(assetInfo)
                onSuccess()
            } catch (throwable: Throwable) {
                this@EditAssetViewModel.logger().e(throwable, "save()")
            }
        }
    }
}

sealed class EditAssetUiState(
    open val classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
    open val assetName: String = "",
    open val totalAmount: String = "",
    open val balance: String = "",
    open val openBank: String = "",
    open val cardNo: String = "",
    open val remark: String = "",
    open val billingDate: String = "",
    open val repaymentDate: String = "",
) {
    object Loading : EditAssetUiState()

    data class Success(
        val isCreditCard: Boolean,
        override val classification: AssetClassificationEnum,
        override val assetName: String,
        override val totalAmount: String,
        override val balance: String,
        override val openBank: String,
        override val cardNo: String,
        override val remark: String,
        override val billingDate: String,
        override val repaymentDate: String,
        val invisible: Boolean,
    ) : EditAssetUiState()
}