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

package cn.wj.android.cashbook.feature.records.screen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet
import cn.wj.android.cashbook.feature.records.viewmodel.RecordDetailsSheetViewModel
import kotlinx.coroutines.launch

/**
 * 记录详情 sheet 内容
 *
 * 标记/改回报销写库成功后关闭弹窗；失败则保留弹窗并 Toast 提示重试。
 *
 * @param recordEntity 显示的记录数据
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestNaviToAssetInfo 导航到资产信息
 * @param onRequestDismissSheet 隐藏 sheet
 */
@Composable
fun RecordDetailSheetContent(
    recordEntity: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
    viewModel: RecordDetailsSheetViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val failedHint = stringResource(id = R.string.reimbursed_update_failed)
    RecordDetailsSheet(
        recordData = recordEntity,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
        onMarkReimbursed = { id ->
            scope.launch {
                if (viewModel.markReimbursed(id, reimbursed = true)) {
                    onRequestDismissSheet()
                } else {
                    Toast.makeText(context, failedHint, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onRevertReimbursed = { id ->
            scope.launch {
                if (viewModel.markReimbursed(id, reimbursed = false)) {
                    onRequestDismissSheet()
                } else {
                    Toast.makeText(context, failedHint, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onRequestDismissSheet = onRequestDismissSheet,
    )
}
