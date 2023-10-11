package cn.wj.android.cashbook.feature.assets.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoDialogEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 资产信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/30
 */
@HiltViewModel
class AssetInfoViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val recordRepository: RecordRepository,
    private val tagRepository: TagRepository,
    application: Application,
) : AndroidViewModel(application) {

    /** 需显示详情的记录数据 */
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 提示语状态 */
    var shouldDisplayBookmark by mutableStateOf("")
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 当前资产 id */
    private val _assetIdData = MutableStateFlow(-1L)

    val uiState = _assetIdData.mapLatest {
        val assetInfo = assetRepository.getAssetById(it)
        AssetInfoUiState.Success(
            assetName = assetInfo?.name.orEmpty(),
            isCreditCard = assetInfo?.type?.isCreditCard() ?: false,
            balance = assetInfo?.balance ?: "0",
            totalAmount = assetInfo?.totalAmount ?: "0",
            billingDate = assetInfo?.billingDate.orEmpty(),
            repaymentDate = assetInfo?.repaymentDate.orEmpty(),
            openBank = assetInfo?.openBank.orEmpty(),
            cardNo = assetInfo?.cardNo.orEmpty(),
            remark = assetInfo?.remark.orEmpty(),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AssetInfoUiState.Loading,
        )

    fun updateAssetId(id: Long) {
        _assetIdData.tryEmit(id)
    }

    fun onRecordItemClick(item: RecordViewsEntity) {
        viewRecordData = item
    }

    fun dismissRecordDetailSheet() {
        viewRecordData = null
    }

    fun displayMoreDialog() {
        dialogState = DialogState.Shown(AssetInfoDialogEnum.MORE_INFO)
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun displayBookmark() {
        shouldDisplayBookmark = R.string.copied_to_clipboard.string(getApplication())
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = ""
    }

    fun showDeleteConfirmDialog() {
        dialogState = DialogState.Shown(AssetInfoDialogEnum.DELETE_ASSET)
    }

    fun deleteAsset(onSuccess: () -> Unit) {
        // 删除资产
        viewModelScope.launch {
            runCatchWithProgress(
                hint = R.string.asset_in_delete.string(getApplication()),
                cancelable = false,
            ) {
                val assetId = _assetIdData.first()
                tagRepository.deleteRelatedWithAsset(assetId)
                recordRepository.deleteRecordRelatedWithAsset(assetId)
                recordRepository.deleteRecordsWithAsset(assetId)
                assetRepository.deleteById(assetId)
                dismissDialog()
                onSuccess()
            }.getOrElse { throwable ->
                this@AssetInfoViewModel.logger().e(throwable, "deleteAsset()")
                shouldDisplayBookmark = R.string.asset_delete_falied.string(getApplication())
            }
        }
    }
}

sealed class AssetInfoUiState(val title: String = "") {
    data object Loading : AssetInfoUiState()

    data class Success(
        private val assetName: String,
        val isCreditCard: Boolean,
        val balance: String,
        val totalAmount: String,
        val billingDate: String,
        val repaymentDate: String,
        val openBank: String,
        val cardNo: String,
        val remark: String,
    ) : AssetInfoUiState(title = assetName) {
        val shouldDisplayMore: Boolean
            get() = openBank.isNotBlank() || cardNo.isNotBlank() || remark.isNotBlank()
    }
}