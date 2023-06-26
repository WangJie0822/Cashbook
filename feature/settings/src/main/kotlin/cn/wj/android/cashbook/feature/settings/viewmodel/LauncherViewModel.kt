package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 首页 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
@HiltViewModel
class LauncherViewModel @Inject constructor() : ViewModel() {

    /** 是否显示抽屉菜单 */
    var shouldDisplayDrawerSheet by mutableStateOf(false)

    fun displayDrawerSheet() {
        shouldDisplayDrawerSheet = true
    }

    fun dismissDrawerSheet() {
        shouldDisplayDrawerSheet = false
    }
}