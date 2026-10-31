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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.R as FilterR

/** 候选过滤仅改变显示；已选项始终对照完整目录，隐藏或删除后仍可显式移除。 */
@Composable
fun SearchTagPicker(
    state: SearchFilterState,
    onEvent: (SearchFilterEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selection = state.tagDraft ?: return
    val catalog = remember(state.tags) { state.tags.associateBy { it.id } }
    val candidates = remember(state.tags, state.tagSearchQuery, state.showHiddenTags) {
        val query = state.tagSearchQuery.trim()
        state.tags.filter { (state.showHiddenTags || !it.invisible) && it.name.contains(query, ignoreCase = true) }
    }
    val selectedIds = remember(selection) { selection.sorted() }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints {
            val compact = maxHeight < 480.dp
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(if (compact) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CbIconButton(
                        onClick = { onEvent(SearchFilterEvent.CancelTags) },
                        modifier = Modifier.testTag("tag_back"),
                    ) { Icon(CbIcons.ArrowBack, stringResource(R.string.cancel)) }
                    Text(
                        text = stringResource(FilterR.string.search_filter_choose_tags),
                        modifier = Modifier.weight(1f),
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CbTextButton(
                        onClick = { onEvent(SearchFilterEvent.ConfirmTags) },
                        modifier = Modifier.testTag("tag_done"),
                    ) { Text(stringResource(FilterR.string.search_filter_done)) }
                }
                SearchFilterTextField(
                    value = state.tagSearchQuery,
                    onValueChange = { onEvent(SearchFilterEvent.ChangeTagSearch(it)) },
                    label = stringResource(FilterR.string.search_filter_search_tags),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (compact) 0.dp else 8.dp)
                        .testTag("tag_search"),
                )
                FilterCheckRow(
                    text = stringResource(FilterR.string.search_filter_show_hidden_tags),
                    checked = state.showHiddenTags,
                    onClick = { onEvent(SearchFilterEvent.ShowHiddenTags(!state.showHiddenTags)) },
                    modifier = Modifier.testTag("tag_show_hidden"),
                )
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().testTag("tag_candidates")) {
                    if (candidates.isEmpty()) {
                        item { Text(stringResource(FilterR.string.search_filter_no_tags), Modifier.padding(16.dp)) }
                    }
                    items(candidates, key = { it.id }) { tag ->
                        FilterCheckRow(
                            text = if (tag.invisible) stringResource(FilterR.string.search_filter_hidden_tag, tag.name) else tag.name,
                            checked = tag.id in selection,
                            onClick = { onEvent(SearchFilterEvent.ToggleTagDraft(tag.id)) },
                            modifier = Modifier.testTag("filter_tag_${tag.id}"),
                        )
                    }
                }
                CbHorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(FilterR.string.search_filter_selected_tags),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    CbTextButton(
                        onClick = { onEvent(SearchFilterEvent.ClearTagSelection) },
                        modifier = Modifier.testTag("tag_clear"),
                    ) { Text(stringResource(FilterR.string.search_filter_clear_tags)) }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = if (compact) 56.dp else 96.dp)
                        .testTag("tag_selected"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (selection.isEmpty()) {
                        item { Text(stringResource(FilterR.string.search_filter_any_tag), Modifier.padding(16.dp)) }
                    }
                    items(selectedIds, key = { it }) { id ->
                        val tag = catalog[id]
                        val label = when {
                            tag == null -> stringResource(FilterR.string.search_filter_unavailable, id)
                            tag.invisible -> stringResource(FilterR.string.search_filter_hidden_tag, tag.name)
                            else -> tag.name
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp).testTag("tag_selected_$id"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.widthIn(max = 200.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            CbIconButton(
                                onClick = { onEvent(SearchFilterEvent.ToggleTagDraft(id)) },
                                modifier = Modifier.testTag("tag_remove_$id"),
                            ) { Icon(CbIcons.Cancel, stringResource(FilterR.string.search_filter_remove, label)) }
                        }
                    }
                }
            }
        }
    }
}
