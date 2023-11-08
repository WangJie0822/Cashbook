/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.assets.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoBookmarkEnum
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoDialogEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) : ViewModel() {

    private var progressDialogHintText = ""

    /** 需显示详情的记录数据 */
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 提示语状态 */
    var bookmark: AssetInfoBookmarkEnum by mutableStateOf(AssetInfoBookmarkEnum.DISMISS)
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
            isCreditCard = assetInfo?.type?.isCreditCard ?: false,
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

    fun setProgressDialogHintText(text: String) {
        progressDialogHintText = text
    }

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
        bookmark = AssetInfoBookmarkEnum.COPIED_TO_CLIPBOARD
    }

    fun dismissBookmark() {
        bookmark = AssetInfoBookmarkEnum.DISMISS
    }

    fun showDeleteConfirmDialog() {
        dialogState = DialogState.Shown(AssetInfoDialogEnum.DELETE_ASSET)
    }

    fun deleteAsset(onSuccess: () -> Unit) {
        // 删除资产
        viewModelScope.launch {
            runCatchWithProgress(
                hint = progressDialogHintText,
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
                bookmark = AssetInfoBookmarkEnum.ASSET_DELETE_FAILED
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
