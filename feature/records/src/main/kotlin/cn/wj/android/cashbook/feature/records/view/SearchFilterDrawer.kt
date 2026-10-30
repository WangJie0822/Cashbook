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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import cn.wj.android.cashbook.feature.records.R as FilterR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFilterDrawer(state: SearchFilterState, onEvent: (SearchFilterEvent) -> Unit, modifier: Modifier = Modifier) {
    val draft = state.drawerDraft ?: return
    val validDate = runCatching { draft.dateSelection.toDateRange(state.monthStartDay).let { it.first < it.second } }.getOrDefault(false)
    BackHandler { onEvent(if (state.categoryDraft != null) SearchFilterEvent.CancelCategories else SearchFilterEvent.CancelDrawer) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (state.categoryDraft != null) {
            SearchCategoryPicker(state, onEvent)
        } else {
            var showDatePicker by rememberSaveable { mutableStateOf(false) }
            if (showDatePicker) {
                SearchFilterDateRangeDialog(draft.dateSelection, state.monthStartDay, { showDatePicker = false }) {
                    onEvent(SearchFilterEvent.UpdateDate(it)); showDatePicker = false
                }
            }
            Column {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(FilterR.string.search_filter), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    CbIconButton(onClick = { onEvent(SearchFilterEvent.CancelDrawer) }, modifier = Modifier.testTag("filter_drawer_close")) {
                        Icon(CbIcons.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
                LazyColumn(Modifier.weight(1f).testTag("filter_options"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(stringResource(FilterR.string.search_filter_scope, state.bookName), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(FilterR.string.search_filter_rule), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        FilterSectionTitle(stringResource(FilterR.string.search_filter_time))
                        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val today = LocalDate.now()
                            val month = DateSelectionEntity.currentMonthPeriod(today, state.monthStartDay)
                            listOf(
                                FilterR.string.search_filter_all_time to DateSelectionEntity.All,
                                FilterR.string.search_filter_this_month to month,
                                FilterR.string.search_filter_last_month to DateSelectionEntity.ByMonth(month.yearMonth.minusMonths(1)),
                                FilterR.string.search_filter_this_year to DateSelectionEntity.ByYear(today.year),
                            ).forEach { (label, date) ->
                                FilterChip(selected = draft.dateSelection == date, onClick = { onEvent(SearchFilterEvent.UpdateDate(date)) }, label = { Text(stringResource(label)) })
                            }
                        }
                        CbTextButton(onClick = { showDatePicker = true }, modifier = Modifier.padding(horizontal = 8.dp).testTag("filter_date_range")) {
                            Text(if (draft.dateSelection is DateSelectionEntity.DateRange) draft.dateSelection.getDisplayText() else stringResource(FilterR.string.search_filter_range))
                        }
                    }
                    item {
                        FilterSectionTitle(stringResource(FilterR.string.search_filter_types))
                        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = draft.typeCategories.isEmpty(), onClick = { onEvent(SearchFilterEvent.UpdateTypes(emptySet())) }, label = { Text(stringResource(FilterR.string.search_filter_any_type)) })
                            RecordTypeCategoryEnum.entries.forEach { type ->
                                FilterChip(
                                    selected = type in draft.typeCategories,
                                    onClick = { onEvent(SearchFilterEvent.UpdateTypes(if (type in draft.typeCategories) draft.typeCategories - type else draft.typeCategories + type)) },
                                    label = { Text(filterTypeLabel(type)) },
                                    modifier = Modifier.testTag("filter_type_${type.name}"),
                                )
                            }
                        }
                        if (state.removedTypeIds.isNotEmpty()) Text(stringResource(FilterR.string.search_filter_pruned, state.removedTypeIds.size), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        FilterSectionTitle(stringResource(FilterR.string.search_filter_categories))
                        CbTextButton(onClick = { onEvent(SearchFilterEvent.OpenCategories) }, modifier = Modifier.fillMaxWidth().testTag("filter_categories")) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(stringResource(FilterR.string.search_filter_choose_categories))
                                Text(categorySelectionSummary(draft.categories, state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        SearchFilterConstraintNotice(draft, state, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        FilterSectionTitle(stringResource(FilterR.string.search_filter_tags))
                        CbTextButton(onClick = { draft.tagIds.forEach { onEvent(SearchFilterEvent.ToggleTag(it)) } }, modifier = Modifier.testTag("filter_all_tags")) {
                            Text(stringResource(FilterR.string.search_filter_any_tag))
                        }
                    }
                    if (state.tags.isEmpty()) item { Text(stringResource(FilterR.string.search_filter_no_tags), Modifier.padding(horizontal = 16.dp)) }
                    items(state.tags, key = { "tag_${it.id}" }) { tag ->
                        FilterCheckRow(
                            text = if (tag.invisible) stringResource(FilterR.string.search_filter_hidden_tag, tag.name) else tag.name,
                            checked = tag.id in draft.tagIds,
                            onClick = { onEvent(SearchFilterEvent.ToggleTag(tag.id)) },
                            modifier = Modifier.testTag("filter_tag_${tag.id}"),
                        )
                    }
                    items((draft.tagIds - state.tags.map { it.id }.toSet()).toList(), key = { "missing_$it" }) { id ->
                        FilterCheckRow(stringResource(FilterR.string.search_filter_unavailable, id), true, { onEvent(SearchFilterEvent.ToggleTag(id)) }, Modifier.testTag("filter_tag_$id"))
                    }
                }
                (if (!validDate) SearchFilterMessage.INVALID_DATE else state.message)?.takeUnless { it == SearchFilterMessage.INCOMPATIBLE_CATEGORIES_REMOVED }?.let {
                    Text(filterMessageText(it), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
                }
                CbHorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    CbTextButton(onClick = { onEvent(SearchFilterEvent.ResetDrawer) }) { Text(stringResource(FilterR.string.search_filter_reset)) }
                    CbTextButton(enabled = validDate, onClick = { onEvent(SearchFilterEvent.ApplyDrawer) }) { Text(stringResource(FilterR.string.search_filter_done)) }
                }
            }
        }
    }
}

@Composable
internal fun FilterSectionTitle(text: String) { Text(text, Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.titleSmall) }

@Composable
internal fun FilterCheckRow(text: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = { onClick() }).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun filterTypeLabel(type: RecordTypeCategoryEnum): String = stringResource(
    when (type) {
        RecordTypeCategoryEnum.EXPENDITURE -> FilterR.string.search_filter_expenditure
        RecordTypeCategoryEnum.INCOME -> FilterR.string.search_filter_income
        RecordTypeCategoryEnum.TRANSFER -> FilterR.string.search_filter_transfer
    },
)

@Composable
internal fun filterMessageText(message: SearchFilterMessage): String = stringResource(
    when (message) {
        SearchFilterMessage.INVALID_DATE -> FilterR.string.search_filter_invalid_date
        SearchFilterMessage.TOO_MANY_CONDITIONS -> FilterR.string.search_filter_too_many
        SearchFilterMessage.METADATA_FAILED -> FilterR.string.search_filter_metadata_failed
        SearchFilterMessage.RESTORE_FAILED -> FilterR.string.search_filter_restore_failed
        SearchFilterMessage.UNAVAILABLE_SELECTIONS -> FilterR.string.search_filter_unavailable_hint
        SearchFilterMessage.INCOMPATIBLE_CATEGORIES_REMOVED -> FilterR.string.search_filter_pruned
        else -> FilterR.string.search_filter_failed
    },
)

@Composable
internal fun categorySelectionSummary(selection: CategoryFilterSelection, state: SearchFilterState): String {
    if (selection.isUnrestricted) return stringResource(FilterR.string.search_filter_all_categories)
    val count = (selection.exactTypeIds + selection.wholeGroupIds).size
    val names = (selection.wholeGroupIds + selection.exactTypeIds).take(2).map { id -> state.catalog.find { it.id == id }?.name ?: stringResource(FilterR.string.search_filter_unavailable, id) }
    return stringResource(FilterR.string.search_filter_category_count, count) + "：" + names.joinToString("、")
}

/** Material 日期选择器以 UTC 表示日历日，查询边界仍由 DateSelectionEntity 按本地时区转换。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchFilterDateRangeDialog(date: DateSelectionEntity, monthStartDay: Int, onDismiss: () -> Unit, onSelect: (DateSelectionEntity.DateRange) -> Unit) {
    val range = date.takeUnless { it == DateSelectionEntity.All }?.toDateRange(monthStartDay)
    fun utcDay(value: Long) = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val picker = rememberDateRangePickerState(initialSelectedStartDateMillis = range?.let { utcDay(it.first) }, initialSelectedEndDateMillis = range?.let { utcDay(it.second - 1L) })
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        CbTextButton(enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null, modifier = Modifier.testTag("filter_date_done"), onClick = {
            fun day(value: Long) = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()
            onSelect(DateSelectionEntity.DateRange(day(picker.selectedStartDateMillis!!), day(picker.selectedEndDateMillis!!)))
        }) { Text(stringResource(FilterR.string.search_filter_done)) }
    }, dismissButton = { CbTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DateRangePicker(state = picker, modifier = Modifier.heightIn(max = 520.dp).testTag("filter_date_picker"), title = { Text(stringResource(FilterR.string.search_filter_range), Modifier.padding(16.dp)) })
    }
}

@Composable
internal fun SearchFilterConstraintNotice(filter: RecordFilter, state: SearchFilterState, modifier: Modifier = Modifier) {
    if (!state.metadataReady) return
    val catalog = state.catalog.associateBy { it.id }
    val categories = filter.categories
    val unavailable = (categories.exactTypeIds + categories.excludedTypeIds).any { it !in catalog } ||
        categories.wholeGroupIds.any { id -> catalog[id]?.let { it.parentId != -1L || it.typeLevel != TypeLevelEnum.FIRST } ?: true } ||
        (filter.tagIds - state.tags.map { it.id }.toSet()).isNotEmpty()
    val conflict = filter.typeCategories.isNotEmpty() && (categories.exactTypeIds + categories.wholeGroupIds).any { id -> catalog[id]?.let { it.typeCategory !in filter.typeCategories } == true }
    if (unavailable || conflict) Text(stringResource(if (unavailable) FilterR.string.search_filter_unavailable_hint else FilterR.string.search_filter_conflicting_categories), modifier.testTag("filter_constraint_notice"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
