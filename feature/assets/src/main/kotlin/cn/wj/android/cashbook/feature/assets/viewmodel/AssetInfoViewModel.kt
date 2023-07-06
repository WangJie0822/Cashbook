package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.DialogState
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

    /** 需显示详情的记录数据 */
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 是否显示失败提示 */
    var shouldDisplayDeleteFailedBookmark by mutableStateOf(false)
        private set

    /** 当前资产 id */
    private val assetIdData = MutableStateFlow(-1L)

    val uiState = assetIdData.mapLatest {
        val assetInfo = assetRepository.getAssetById(it)
        AssetInfoUiState.Success(
            assetName = assetInfo?.name.orEmpty(),
            isCreditCard = assetInfo?.type?.isCreditCard() ?: false,
            balance = assetInfo?.balance ?: "0",
            totalAmount = assetInfo?.totalAmount ?: "0",
            billingDate = assetInfo?.billingDate.orEmpty(),
            repaymentDate = assetInfo?.repaymentDate.orEmpty(),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AssetInfoUiState.Loading,
        )

    fun updateAssetId(id: Long) {
        assetIdData.tryEmit(id)
    }

    fun onDeleteRecordClick(recordId: Long) {
        dialogState = DialogState.Shown(recordId)
    }

    fun onDeleteRecordResult(result: ResultModel) {
        if (result.isSuccess) {
            // 删除成功，隐藏弹窗
            dismissDeleteConfirmDialog()
        } else {
            // 提示
            shouldDisplayDeleteFailedBookmark = true
        }
    }

    fun onRecordItemClick(item: RecordViewsEntity) {
        viewRecordData = item
    }

    fun dismissRecordDetailSheet() {
        viewRecordData = null
    }

    fun dismissDeleteConfirmDialog() {
        dialogState = DialogState.Dismiss
    }

    fun dismissBookmark() {
        shouldDisplayDeleteFailedBookmark = false
    }
}

sealed class AssetInfoUiState(val title: String) {
    object Loading : AssetInfoUiState(title = "")

    data class Success(
        private val assetName: String,
        val isCreditCard: Boolean,
        val balance: String,
        val totalAmount: String,
        val billingDate: String,
        val repaymentDate: String,
    ) : AssetInfoUiState(title = assetName)
}