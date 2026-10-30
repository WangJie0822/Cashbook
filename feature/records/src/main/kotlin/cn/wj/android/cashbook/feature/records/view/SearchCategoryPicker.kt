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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbOutlinedTextField
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.model.CategorySelectionKind
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.R as FilterR

@Composable
fun SearchCategoryPicker(state: SearchFilterState, onEvent: (SearchFilterEvent) -> Unit, modifier: Modifier = Modifier) {
    val selection = state.categoryDraft ?: return
    val allowed = state.drawerDraft?.typeCategories.orEmpty()
    val types = RecordTypeCategoryEnum.entries.filter { allowed.isEmpty() || it in allowed }
    val browseType = state.categoryBrowseType.takeIf { it in types } ?: types.first()
    val catalog = state.catalog.filter { it.typeCategory == browseType }
    val parents = catalog.filter { it.parentId == -1L && it.typeLevel == TypeLevelEnum.FIRST }.sortedWith(compareBy({ it.sort }, { it.id }))
    val parentNames = parents.associate { it.id to it.name }
    val parent = parents.find { it.id == state.categoryBrowseParentId } ?: parents.firstOrNull()
    val search = state.categorySearchQuery.trim()
    val visible = if (search.isNotEmpty()) {
        catalog.filter { type -> type.name.contains(search, ignoreCase = true) || parentNames[type.parentId]?.contains(search, ignoreCase = true) == true }
            .sortedWith(compareBy({ it.sort }, { it.id }))
    } else {
        catalog.filter { it.id == parent?.id || it.parentId == parent?.id }.sortedWith(compareBy({ it.typeLevel.ordinal }, { it.sort }, { it.id }))
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints {
            val compact = maxHeight < 480.dp
            val typeTabs: @Composable () -> Unit = {
                CbTabRow(selectedTabIndex = types.indexOf(browseType), modifier = Modifier.fillMaxWidth()) {
                    types.forEach { type ->
                        CbTab(selected = browseType == type, onClick = { onEvent(SearchFilterEvent.ChangeCategoryBrowser(type)) }, modifier = Modifier.testTag("category_tab_${type.name}"), text = { Text(filterTypeLabel(type)) })
                    }
                }
            }
            Column {
                Row(Modifier.fillMaxWidth().padding(if (compact) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CbIconButton(onClick = { onEvent(SearchFilterEvent.CancelCategories) }, modifier = Modifier.testTag("category_back")) { Icon(CbIcons.ArrowBack, stringResource(R.string.cancel)) }
                    if (compact) {
                        Column(Modifier.weight(1f)) { typeTabs() }
                    } else {
                        Text(stringResource(FilterR.string.search_filter_choose_categories), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    }
                    CbTextButton(onClick = { onEvent(SearchFilterEvent.ConfirmCategories) }) { Text(stringResource(FilterR.string.search_filter_done)) }
                }
                if (!compact) typeTabs()
                SearchFilterTextField(state.categorySearchQuery, { onEvent(SearchFilterEvent.ChangeCategorySearch(it)) }, stringResource(FilterR.string.search_filter_search_categories), Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (compact) 0.dp else 12.dp).testTag("category_search"))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(Modifier.weight(0.30f).testTag("category_parents")) {
                        items(parents, key = { it.id }) { item ->
                            Surface(color = if (item.id == parent?.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                                Text(
                                    item.name,
                                    Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(item.id == parent?.id, role = Role.Tab, onClick = { onEvent(SearchFilterEvent.ChangeCategoryBrowser(browseType, item.id)) }).padding(12.dp).testTag("category_parent_${item.id}"),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(0.70f).padding(horizontal = 8.dp)) {
                        if (parent != null && search.isEmpty()) {
                            val members = catalog.filter { it.id == parent.id || it.parentId == parent.id }
                            val grouped = parent.id in selection.wholeGroupIds
                            val partial = members.any { it.id in selection.exactTypeIds || it.id in selection.excludedTypeIds }
                            val toggle = if (grouped && !partial) ToggleableState.On else if (grouped || partial) ToggleableState.Indeterminate else ToggleableState.Off
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).triStateToggleable(toggle, role = Role.Checkbox, onClick = { onEvent(SearchFilterEvent.ToggleCategoryGroup(parent.id, !grouped)) }).testTag("category_group_${parent.id}"), verticalAlignment = Alignment.CenterVertically) {
                                TriStateCheckbox(toggle, onClick = null)
                                Text(stringResource(FilterR.string.search_filter_whole_group, parent.name), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (visible.isEmpty()) Text(stringResource(FilterR.string.search_filter_no_categories), Modifier.padding(12.dp))
                        LazyVerticalGrid(columns = GridCells.Adaptive(112.dp), modifier = Modifier.weight(1f).testTag("category_items"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(visible, key = { it.id }) { type ->
                                FilterCheckRow(
                                    if (type.typeLevel == TypeLevelEnum.FIRST) stringResource(FilterR.string.search_filter_parent_only, type.name) else if (search.isNotEmpty()) parentNames[type.parentId]?.let { "$it / ${type.name}" } ?: type.name else type.name,
                                    !selection.isUnrestricted && selection.matches(type.id, type.parentId),
                                    { onEvent(SearchFilterEvent.ToggleCategoryItem(type.id, selection.isUnrestricted || !selection.matches(type.id, type.parentId))) },
                                    Modifier.testTag("category_item_${type.id}"),
                                )
                            }
                        }
                    }
                }
                CbHorizontalDivider()
                if (!compact) Text(stringResource(FilterR.string.search_filter_selected), Modifier.padding(start = 16.dp, top = 12.dp), style = MaterialTheme.typography.labelLarge)
                LazyRow(Modifier.fillMaxWidth().heightIn(min = 48.dp, max = if (compact) 64.dp else 112.dp).testTag("category_selected"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selection.isUnrestricted) item { Text(stringResource(FilterR.string.search_filter_all_categories), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
                    val selected = selection.wholeGroupIds.map { CategorySelectionKind.GROUP to it } + selection.exactTypeIds.map { CategorySelectionKind.EXACT to it } + selection.excludedTypeIds.map { CategorySelectionKind.EXCLUDED to it }
                    items(selected, key = { "${it.first}_${it.second}" }) { (kind, id) ->
                        val type = state.catalog.find { it.id == id }
                        val name = type?.let { selected -> state.catalog.find { it.id == selected.parentId }?.let { "${it.name} / ${selected.name}" } ?: selected.name } ?: stringResource(FilterR.string.search_filter_unavailable, id)
                        val label = when (kind) {
                            CategorySelectionKind.GROUP -> {
                                val excludedCount = state.catalog.count { (it.id == id || it.parentId == id) && it.id in selection.excludedTypeIds }
                                if (excludedCount == 0) stringResource(FilterR.string.search_filter_group_summary, name) else stringResource(FilterR.string.search_filter_group_except, name, excludedCount)
                            }
                            CategorySelectionKind.EXCLUDED -> stringResource(FilterR.string.search_filter_excluded_summary, name)
                            else -> name
                        }
                        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.widthIn(max = 220.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            CbIconButton(onClick = { onEvent(SearchFilterEvent.RemoveCategory(id, kind)) }, modifier = Modifier.testTag("category_remove_${kind.name.lowercase()}_$id")) {
                                Icon(CbIcons.Cancel, stringResource(FilterR.string.search_filter_remove, name))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchFilterTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val callback by rememberUpdatedState(onValueChange)
    val field = remember { TextFieldState(defaultText = value, delayAfterTextChange = 0L, afterTextChange = { callback(it) }) }
    LaunchedEffect(value) { if (field.text != value) field.text = value }
    CbOutlinedTextField(field, modifier = modifier, label = { Text(label) }, singleLine = true)
}
