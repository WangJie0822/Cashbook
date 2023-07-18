package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class EditRecordSelectAssetBottomSheetViewModel @Inject constructor(
    assetRepository: AssetRepository,
) : ViewModel() {

    val assetListData = assetRepository.currentVisibleAssetListData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    fun update(
        currentTypeId: Long,
        isRelated: Boolean
    ) {
        // TODO 根据当前类型和是否关联获取资产列表
    }
}