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

package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.viewmodel.InvisibleAssetViewModel

@Composable
internal fun InvisibleAssetRoute(
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvisibleAssetViewModel = hiltViewModel(),
) {
    val assetTypedList by viewModel.assetTypedListData.collectAsStateWithLifecycle()

    InvisibleAssetScreen(
        assetTypedList = assetTypedList,
        onAssetItemClick = onRequestNaviToAssetInfo,
        onRequestVisibleAsset = viewModel::visibleAsset,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvisibleAssetScreen(
    assetTypedList: List<AssetTypeViewsModel>,
    onAssetItemClick: (Long) -> Unit,
    onRequestVisibleAsset: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                title = { Text(text = stringResource(id = R.string.invisible_asset)) },
                onBackClick = onRequestPopBackStack,
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues),
            ) {
                if (assetTypedList.isEmpty()) {
                    // 无数据
                    Empty(
                        hintText = stringResource(id = R.string.asset_no_data),
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(state = rememberScrollState()),
                    ) {
                        Text(
                            text = stringResource(id = R.string.invisible_asset_show_hint),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp),
                        )
                        assetTypedList.forEach { assetTypedInfo ->
                            AssetTypedInfoItem(
                                assetTypedInfo = assetTypedInfo,
                                onAssetItemClick = onAssetItemClick,
                                onAssetItemLongClick = onRequestVisibleAsset,
                            )
                        }
                        Footer(hintText = stringResource(id = R.string.footer_hint_default))
                    }
                }
            }
        },
    )
}
