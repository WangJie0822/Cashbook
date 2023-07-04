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

    /** 当前资产 id */
    private val assetId = MutableStateFlow(-1L)

    /** 当前资产信息 */
    private val currentAssetInfo = assetId.mapLatest { assetRepository.getAssetById(it) }

    /** 需显示详情的记录数据 */
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)

    /** 是否显示失败提示 */
    var shouldDisplayDeleteFailedBookmark by mutableStateOf(false)

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

    fun onDeleteRecordClick(recordId: Long) {
        dialogState = DialogState.Shown(recordId)
    }

    fun onDeleteRecordResult(result: ResultModel) {
        if (result.isSuccess) {
            // 删除成功，隐藏弹窗
            dismissDeleteConfirmDialog()
            shouldDisplayDeleteFailedBookmark = true
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