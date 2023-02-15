package cn.wj.android.cashbook.feature.settings.screen

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.feature.settings.model.LauncherMenuItemModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherRoute(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, DrawerState) -> Unit,
) {
    // TODO 设置项列表
    val menus = arrayListOf(
        LauncherMenuItemModel(Icons.Default.LibraryBooks, "我的账本") {
        },
        LauncherMenuItemModel(Icons.Default.WebAsset, "我的资产") {
        },
        LauncherMenuItemModel(Icons.Default.Category, "我的分类") {
        },
        LauncherMenuItemModel(Icons.Default.Layers, "我的标签") {
        },
        LauncherMenuItemModel(Icons.Default.Settings, "设置") {
        },
        LauncherMenuItemModel(Icons.Default.Info, "关于我们") {
        },
    )
    LauncherScreen(menus = menus, modifier = modifier, content = content)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherScreen(
    menus: List<LauncherMenuItemModel>,
    modifier: Modifier,
    content: @Composable (Modifier, DrawerState) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet {
                Text(text = "标题")
                menus.forEach { menuItem ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(imageVector = menuItem.vectorStart, contentDescription = null)
                        },
                        label = {
                            Text(text = menuItem.tittle)
                        },
                        selected = false,
                        onClick = menuItem.onClick,
                    )
                }
            }
        },
    ) {
        Scaffold {
            content(modifier, drawerState)
        }
    }
}