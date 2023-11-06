package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.domain.usecase.GetAssetListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 编辑记录界面选择资产抽屉 ViewModel
 */
@HiltViewModel
class EditRecordSelectAssetBottomSheetViewModel @Inject constructor(
    getAssetListUseCase: GetAssetListUseCase,
) : ViewModel() {

    private val _assetParamsData = MutableStateFlow(AssetParams())

    /** 可选择资产列表 */
    val assetListData = _assetParamsData.flatMapLatest { params ->
        getAssetListUseCase(
            currentTypeId = params.currentTypeId,
            selectedAssetId = params.selectedAssetId,
            isRelated = params.isRelated,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    fun update(
        currentTypeId: Long,
        selectedAssetId: Long,
        isRelated: Boolean
    ) {
        _assetParamsData.tryEmit(
            AssetParams(
                currentTypeId = currentTypeId,
                selectedAssetId = selectedAssetId,
                isRelated = isRelated
            )
        )
    }
}

data class AssetParams(
    val currentTypeId: Long = -1L,
    val selectedAssetId: Long = -1L,
    val isRelated: Boolean = false,
)