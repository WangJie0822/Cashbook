package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 不可见资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/19
 */
@HiltViewModel
class InvisibleAssetViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
) : ViewModel() {

    /**
     * 不可见资产数据列表
     * - 按照资产大类进行分类
     */
    val assetTypedListData = assetRepository.currentInvisibleAssetTypeData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = emptyList(),
        )

    fun visibleAsset(id: Long) {
        viewModelScope.launch {
            assetRepository.visibleAssetById(id)
        }
    }
}