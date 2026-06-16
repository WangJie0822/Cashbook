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

package cn.wj.android.cashbook.core.design.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cn.wj.android.cashbook.core.common.TestTag
import cn.wj.android.cashbook.core.design.R
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

/**
 * 通用顶部标题栏
 *
 * @param onBackClick 返回事件
 * @param title 标题
 * @param actions 标题菜单
 * @param colors 标题控件颜色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbTopAppBar(
    title: @Composable () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIconContentDescription: String? = stringResource(id = R.string.cd_design_navigate_back),
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CbTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            CbIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = CbIcons.ArrowBack,
                    contentDescription = navigationIconContentDescription,
                )
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = title,
        modifier = modifier.testTag(TestTag.CB_TOP_APP_BAR),
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

/**
 * 顶部标题栏 + title 槽内 Tab 行的封装。
 *
 * 固化 [CbTopAppBar] 的 title 槽放一个 `Modifier.fillMaxWidth()` 的 [CbTabRow]——
 * fillMaxWidth 写死、不暴露给调用方，从 API 层杜绝 fillMaxSize 误用（BUG-1：fillMaxSize
 * 含 fillMaxHeight 会使 TopAppBar 撑满全屏高、CbScaffold body 塌陷 0 高、内容不渲染）。
 *
 * @param selectedTabIndex 选中 tab 下标
 * @param indicatorColor 指示器颜色（必填，无默认——各调用方用色不同）
 * @param onBackClick 返回事件
 * @param actions 标题菜单
 * @param tabs tab 内容槽，调用方放 [CbTab]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbTabTopAppBar(
    selectedTabIndex: Int,
    indicatorColor: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    tabs: @Composable () -> Unit,
) {
    CbTopAppBar(
        onBackClick = onBackClick,
        modifier = modifier,
        actions = actions,
        title = {
            CbTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Unspecified,
                contentColor = Color.Unspecified,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = indicatorColor,
                        )
                    }
                },
                divider = {},
                tabs = tabs,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CommonTopBarPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            Column {
                CbTopAppBar(title = { Text(text = "标题") }, onBackClick = { })
                CbTopAppBar(title = { Text(text = "标题") }, onBackClick = { }, actions = {
                    CbIconButton(onClick = { }) {
                        Icon(imageVector = CbIcons.Menu, contentDescription = null)
                    }
                })
            }
        }
    }
}
