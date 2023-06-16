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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.model.enums.LauncherMenuAction
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 首页显示
 *
 * @param onMenuClick 菜单点击回调
 * @param content 内容区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherRoute(
    onMenuClick: (LauncherMenuAction) -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    LauncherScreen(
        onMenuClick = onMenuClick,
        content = content,
    )
}

/**
 * 首页显示
 *
 * @param onMenuClick 菜单点击回调
 * @param content 内容区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherScreen(
    onMenuClick: (LauncherMenuAction) -> Unit,
    content: @Composable (() -> Unit) -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
) {

    if (drawerState.isOpen) {
        BackPressHandler {
            coroutineScope.launch {
                drawerState.close()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LauncherSheet { action ->
                onMenuClick(action)
                // 关闭抽屉
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        },
    ) {
        content {
            coroutineScope.launch {
                drawerState.open()
            }
        }
    }
}

/**
 * 首页抽屉菜单
 *
 * @param onMenuClick 菜单点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherSheet(
    onMenuClick: (LauncherMenuAction) -> Unit,
) {
    ModalDrawerSheet {
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
            onClick = { onMenuClick(LauncherMenuAction.MY_BOOK) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_assets)) },
            icon = { Icon(imageVector = Icons.Default.WebAsset, contentDescription = null) },
            selected = false,
            onClick = { onMenuClick(LauncherMenuAction.MY_ASSET) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_categories)) },
            icon = { Icon(imageVector = Icons.Default.Category, contentDescription = null) },
            selected = false,
            onClick = { onMenuClick(LauncherMenuAction.MY_CATEGORY) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_tags)) },
            icon = { Icon(imageVector = Icons.Default.Layers, contentDescription = null) },
            selected = false,
            onClick = { onMenuClick(LauncherMenuAction.MY_TAG) },
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
            onClick = { onMenuClick(LauncherMenuAction.SETTING) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.about_us)) },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            selected = false,
            onClick = { onMenuClick(LauncherMenuAction.ABOUT_US) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}