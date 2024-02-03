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

package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherViewModel

/**
 * 首页显示
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param onRequestNaviToMyAsset 导航到我的资产
 * @param onRequestNaviToMyBooks 导航到我的账本
 * @param onRequestNaviToMyCategory 导航到我的分类
 * @param onRequestNaviToMyTags 导航到我的标签
 * @param onRequestNaviToSetting 导航到设置
 * @param onRequestNaviToAboutUs 导航到关于我们
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 */
@Composable
internal fun LauncherRoute(
    onRequestNaviToMyAsset: () -> Unit,
    onRequestNaviToMyBooks: () -> Unit,
    onRequestNaviToMyCategory: () -> Unit,
    onRequestNaviToMyTags: () -> Unit,
    onRequestNaviToSetting: () -> Unit,
    onRequestNaviToAboutUs: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel(),
    content: @Composable (() -> Unit) -> Unit,
) {
    // 界面 UI 状态数据
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        onRequestDisplayDrawerSheet = viewModel::displayDrawerSheet,
        onRequestDismissDrawerSheet = viewModel::dismissDrawerSheet,
        uiState = uiState,
        onMyAssetClick = {
            onRequestNaviToMyAsset.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyBookClick = {
            onRequestNaviToMyBooks.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyCategoryClick = {
            onRequestNaviToMyCategory.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyTagClick = {
            onRequestNaviToMyTags.invoke()
            viewModel.dismissDrawerSheet()
        },
        onSettingClick = {
            onRequestNaviToSetting.invoke()
            viewModel.dismissDrawerSheet()
        },
        onAboutUsClick = {
            onRequestNaviToAboutUs.invoke()
            viewModel.dismissDrawerSheet()
        },
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
    )
}

/**
 * 首页显示
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param shouldDisplayDrawerSheet 是否显示抽屉菜单
 * @param onRequestDisplayDrawerSheet 显示抽屉菜单
 * @param onRequestDismissDrawerSheet 隐藏抽屉菜单
 * @param uiState UI 显示数据
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 * @param drawerState 抽屉状态，默认关闭，状态变化时在回调中更新数据状态
 */
@Composable
internal fun LauncherScreen(
    shouldDisplayDrawerSheet: Boolean,
    onRequestDisplayDrawerSheet: () -> Unit,
    onRequestDismissDrawerSheet: () -> Unit,
    uiState: LauncherUiState,
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = {
            if (it == DrawerValue.Closed) {
                onRequestDismissDrawerSheet()
            } else {
                onRequestDisplayDrawerSheet()
            }
            true
        },
    ),
) {
    // 控制抽屉菜单显示隐藏
    LaunchedEffect(shouldDisplayDrawerSheet) {
        if (shouldDisplayDrawerSheet) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // 抽屉显示时，返回关闭抽屉
    if (drawerState.isOpen) {
        BackPressHandler {
            onRequestDismissDrawerSheet()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (uiState) {
            LauncherUiState.Loading -> {
                Loading(modifier = Modifier.align(Alignment.Center))
            }

            is LauncherUiState.Success -> {
                ModalNavigationDrawer(
                    modifier = modifier,
                    drawerState = drawerState,
                    drawerContent = {
                        LauncherSheet(
                            currentBookName = uiState.currentBookName,
                            onMyAssetClick = onMyAssetClick,
                            onMyBookClick = onMyBookClick,
                            onMyCategoryClick = onMyCategoryClick,
                            onMyTagClick = onMyTagClick,
                            onSettingClick = onSettingClick,
                            onAboutUsClick = onAboutUsClick,
                        )
                    },
                    content = content,
                )
            }
        }
    }
}

/**
 * 首页抽屉菜单
 *
 * @param currentBookName 当前账本名
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 */
@Composable
internal fun LauncherSheet(
    currentBookName: String,
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.sheet_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(24.dp),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_books)) },
            icon = { Icon(imageVector = CbIcons.LibraryBooks, contentDescription = null) },
            badge = {
                Text(
                    text = currentBookName,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                )
            },
            selected = false,
            onClick = onMyBookClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_assets)) },
            icon = { Icon(imageVector = CbIcons.WebAsset, contentDescription = null) },
            selected = false,
            onClick = onMyAssetClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_categories)) },
            icon = { Icon(imageVector = CbIcons.Category, contentDescription = null) },
            selected = false,
            onClick = onMyCategoryClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_tags)) },
            icon = { Icon(imageVector = CbIcons.Layers, contentDescription = null) },
            selected = false,
            onClick = onMyTagClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        CbHorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.settings)) },
            icon = { Icon(imageVector = CbIcons.Settings, contentDescription = null) },
            selected = false,
            onClick = onSettingClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.about_us)) },
            icon = { Icon(imageVector = CbIcons.Info, contentDescription = null) },
            selected = false,
            onClick = onAboutUsClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}
