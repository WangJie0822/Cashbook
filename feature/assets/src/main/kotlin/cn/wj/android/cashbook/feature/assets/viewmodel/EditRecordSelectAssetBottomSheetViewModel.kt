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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.domain.usecase.GetAssetListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    fun update(
        currentTypeId: Long,
        selectedAssetId: Long,
        isRelated: Boolean,
    ) {
        _assetParamsData.tryEmit(
            AssetParams(
                currentTypeId = currentTypeId,
                selectedAssetId = selectedAssetId,
                isRelated = isRelated,
            ),
        )
    }
}

data class AssetParams(
    val currentTypeId: Long = -1L,
    val selectedAssetId: Long = -1L,
    val isRelated: Boolean = false,
)
