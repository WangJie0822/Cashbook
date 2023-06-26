package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherViewModel

/**
 * 首页显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherRoute(
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    content: @Composable (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel(),
) {
    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        onMyAssetClick = {
            onMyAssetClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyBookClick = {
            onMyBookClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyCategoryClick = {
            onMyCategoryClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyTagClick = {
            onMyTagClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onSettingClick = {
            onSettingClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onAboutUsClick = {
            onAboutUsClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
        drawerState = rememberDrawerState(
            initialValue = DrawerValue.Closed,
            confirmStateChange = {
                if (it == DrawerValue.Closed) {
                    viewModel.dismissDrawerSheet()
                }
                true
            },
        )
    )
}

/**
 * 首页显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherScreen(
    shouldDisplayDrawerSheet: Boolean,
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
) {

    // 抽屉菜单显示状态
    LaunchedEffect(shouldDisplayDrawerSheet) {
        if (shouldDisplayDrawerSheet) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            LauncherSheet(
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

/**
 * 首页抽屉菜单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherSheet(
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
            modifier = Modifier.padding(24.dp)
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_books)) },
            icon = { Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null) },
            selected = false,
            onClick = onMyBookClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_assets)) },
            icon = { Icon(imageVector = Icons.Default.WebAsset, contentDescription = null) },
            selected = false,
            onClick = onMyAssetClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_categories)) },
            icon = { Icon(imageVector = Icons.Default.Category, contentDescription = null) },
            selected = false,
            onClick = onMyCategoryClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_tags)) },
            icon = { Icon(imageVector = Icons.Default.Layers, contentDescription = null) },
            selected = false,
            onClick = onMyTagClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        Divider(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .height(1.dp)
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.settings)) },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
            selected = false,
            onClick = onSettingClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.about_us)) },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            selected = false,
            onClick = onAboutUsClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}