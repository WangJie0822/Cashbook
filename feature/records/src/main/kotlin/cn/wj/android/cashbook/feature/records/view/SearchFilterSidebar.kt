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

package cn.wj.android.cashbook.feature.records.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** 与首页使用相同的 M3 抽屉；方向适配只作用于容器，正文保留原方向。 */
@Composable
internal fun SearchFilterSidebar(
    state: SearchFilterState,
    onEvent: (SearchFilterEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val latestState by rememberUpdatedState(state)
    val latestEvent by rememberUpdatedState(onEvent)
    val drawerState = rememberDrawerState(DrawerValue.Closed) { value ->
        if (value == DrawerValue.Closed && latestState.drawerDraft != null) latestEvent(SearchFilterEvent.CancelDrawer)
        true
    }
    val requestedOpen = state.drawerDraft != null
    var retained by remember { mutableStateOf(state.copy(drawerDraft = RecordFilter())) }
    var previouslyOpen by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }
    SideEffect {
        if (requestedOpen) {
            if (!previouslyOpen) session++
            retained = state
        }
        previouslyOpen = requestedOpen
    }
    LaunchedEffect(requestedOpen) {
        if (requestedOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.targetValue }.distinctUntilChanged().filter { it == DrawerValue.Open }.collect {
            if (latestState.drawerDraft == null && latestState.metadataReady) latestEvent(SearchFilterEvent.OpenDrawer)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = minOf(maxWidth * 0.92f, 600.dp)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = state.metadataReady,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        ModalDrawerSheet(
                            modifier = Modifier.width(width).testTag("filter_sidebar"),
                            drawerShape = AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp),
                            windowInsets = WindowInsets(0, 0, 0, 0),
                        ) {
                            // 关闭期间保留最后一页供滑出；候选与按钮不能继续修改已丢弃的草稿。
                            key(session) {
                                SearchFilterDrawerContent(
                                    state = if (requestedOpen) state else retained,
                                    onEvent = { if (latestState.drawerDraft != null) latestEvent(it) },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
                                    active = requestedOpen,
                                )
                            }
                        }
                    }
                },
                content = { CompositionLocalProvider(LocalLayoutDirection provides direction, content = content) },
            )
        }
    }
}
