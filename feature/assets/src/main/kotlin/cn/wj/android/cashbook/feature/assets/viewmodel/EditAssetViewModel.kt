@file:OptIn(ExperimentalCoroutinesApi::class)

package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.ext.hasBankInfo
import cn.wj.android.cashbook.core.model.ext.isCreditCard
import cn.wj.android.cashbook.domain.usecase.GetDefaultAssetUseCase
import cn.wj.android.cashbook.feature.assets.R
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditAssetViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    getDefaultAssetUseCase: GetDefaultAssetUseCase,
) : ViewModel() {

    /** 底部菜单类型数据 */
    val bottomSheetData: MutableStateFlow<EditAssetBottomSheetEnum> =
        MutableStateFlow(EditAssetBottomSheetEnum.CLASSIFICATION_TYPE)

    private val assetIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)

    private val defaultAssetData: Flow<AssetEntity> = assetIdData.flatMapLatest {
        getDefaultAssetUseCase(it)
    }

    private val mutableAssetData: MutableStateFlow<AssetEntity?> = MutableStateFlow(null)

    private val assetData: Flow<AssetEntity> =
        combine(defaultAssetData, mutableAssetData) { default, mutable ->
            mutable ?: default
        }

    val type: StateFlow<ClassificationTypeEnum> = assetData
        .mapLatest { it.type }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ClassificationTypeEnum.CAPITAL_ACCOUNT
        )

    val creditCard: StateFlow<Boolean> = assetData
        .mapLatest { it.isCreditCard }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val classification: StateFlow<AssetClassificationEnum> = assetData
        .mapLatest { it.classification }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AssetClassificationEnum.CASH
        )

    val hasBankInfo: StateFlow<Boolean> = assetData
        .mapLatest { it.hasBankInfo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val assetName: MutableState<String> =
        mutableStateOf(AssetClassificationEnum.CASH.nameResId.string)

    val assetHint: MutableStateFlow<String?> = MutableStateFlow(null)

    val totalAmount: StateFlow<String> = assetData
        .mapLatest { it.totalAmount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val totalAmountHint: MutableStateFlow<String?> = MutableStateFlow(null)

    val balance: StateFlow<String> = assetData
        .mapLatest { it.balance }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val openBank: StateFlow<String> = assetData
        .mapLatest { it.openBank }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val cardNo: StateFlow<String> = assetData
        .mapLatest { it.cardNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val remark: StateFlow<String> = assetData
        .mapLatest { it.remark }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val billingDate: StateFlow<String> = assetData
        .mapLatest { it.billingDate }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val repaymentDate: StateFlow<String> = assetData
        .mapLatest { it.repaymentDate }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    val invisible: StateFlow<Boolean> = assetData
        .mapLatest { it.invisible }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    private var typeTemp: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT

    fun onTypeChanged(type: ClassificationTypeEnum) {
        viewModelScope.launch {
            typeTemp = type
        }
    }

    fun onClassificationChanged(classification: AssetClassificationEnum) {
        viewModelScope.launch {
            val name = classification.nameResId.string
            assetName.value = name
            assetHint.value = null
            mutableAssetData.value =
                assetData.first()
                    .copy(name = name, type = typeTemp, classification = classification)
        }
    }

    fun onSelectBankCard() {
        bottomSheetData.value = EditAssetBottomSheetEnum.ASSET_CLASSIFICATION
    }

    fun onSelectTypeClick() {
        bottomSheetData.value = EditAssetBottomSheetEnum.CLASSIFICATION_TYPE
    }

    fun onAssetNameChanged(name: String) {
        viewModelScope.launch {
            assetHint.value = null
            mutableAssetData.value = assetData.first().copy(name = name)
        }
    }

    fun onTotalAmountChanged(totalAmount: String) {
        viewModelScope.launch {
            totalAmountHint.value = null
            mutableAssetData.value = assetData.first().copy(totalAmount = totalAmount)
        }
    }

    fun onBalanceChanged(balance: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(balance = balance)
        }
    }

    fun onOpenBankChanged(openBank: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(openBank = openBank)
        }
    }

    fun onCardNoChanged(cardNo: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(cardNo = cardNo)
        }
    }

    fun onRemarkChanged(remark: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(remark = remark)
        }
    }

    fun onBillingDateChanged(billingDate: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(billingDate = billingDate)
        }
    }

    fun onRepaymentDateChanged(repaymentDate: String) {
        viewModelScope.launch {
            mutableAssetData.value = assetData.first().copy(repaymentDate = repaymentDate)
        }
    }

    fun trySaveAsset(afterSave: () -> Unit) {
        viewModelScope.launch {
            var asset = assetData.first()
            var needFix = false
            if (asset.name.isBlank()) {
                assetHint.value = R.string.please_enter_asset_name.string
                needFix = true
            }
            if (asset.isCreditCard && asset.totalAmount.isBlank()) {
                totalAmountHint.value = R.string.please_enter_total_amount.string
                needFix = true
            }
            if (needFix) {
                return@launch
            }

            if (!asset.isCreditCard) {
                // 非信用卡，清空信用卡相关信息
                asset = asset.copy(totalAmount = "", billingDate = "", repaymentDate = "")
                if (!asset.hasBankInfo) {
                    // 非银行卡，清空银行卡信息
                    asset = asset.copy(openBank = "", cardNo = "")
                }
            }

            // 更新
            assetRepository.updateAsset(asset)
            afterSave()
        }
    }
}