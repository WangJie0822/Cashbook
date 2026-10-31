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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbPieChart
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.PieSlice
import cn.wj.android.cashbook.core.design.theme.rememberCbPieChartPalette
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import cn.wj.android.cashbook.core.ui.expand.text
import java.util.Locale
import kotlin.math.abs
import cn.wj.android.cashbook.feature.records.R as SearchR

@Stable
internal class SearchDistributionState(expanded: Boolean = true, allLegends: Boolean = false, selectedKey: String? = null) {
    var expanded by mutableStateOf(expanded)
    var allLegends by mutableStateOf(allLegends)
    var selectedKey by mutableStateOf(selectedKey)
}

@Composable
internal fun rememberSearchDistributionState(key: String, categories: List<RecordCategoryNet>?): SearchDistributionState {
    val state = rememberSaveable(
        key,
        saver = listSaver<SearchDistributionState, Any>(
            save = { listOf(it.expanded, it.allLegends, it.selectedKey.orEmpty()) },
            restore = { SearchDistributionState(it[0] as Boolean, it[1] as Boolean, (it[2] as String).ifEmpty { null }) },
        ),
    ) { SearchDistributionState() }
    LaunchedEffect(categories) {
        if (categories != null && categories.none { it.distributionKey == state.selectedKey }) state.selectedKey = null
    }
    return state
}

/** 展开余项直接进入搜索页的主 LazyColumn，避免嵌套滚动或一次构造全部图例。 */
internal fun LazyListScope.searchResultDistribution(statistics: RecordSearchStatistics, state: SearchDistributionState) {
    val colorOrder = distributionColorOrder(statistics.categories)
    item(key = "distribution") { SearchResultDistribution(statistics, state, colorOrder, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
    if (state.expanded && state.allLegends) {
        items(statistics.categories.drop(5), key = { "distribution_more_${it.distributionKey}" }) { category ->
            Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                SearchDistributionLegend(category, statistics.magnitude, state, colorOrder.getValue(category.distributionKey))
            }
        }
        item(key = "distribution_end") {
            CbTextButton(onClick = { state.allLegends = false }, modifier = Modifier.padding(horizontal = 16.dp).testTag("distribution_expand_bottom")) {
                Text(stringResource(SearchR.string.search_distribution_collapse))
            }
        }
    }
}

/** 搜索结果的全量分类净额；展示交互不改变任何查询条件。 */
@Composable
internal fun SearchResultDistribution(statistics: RecordSearchStatistics, state: SearchDistributionState, colorOrder: Map<String, Int>, modifier: Modifier = Modifier) {
    with(state) {
        val categories = statistics.categories
        val selected = categories.find { it.distributionKey == selectedKey }
        val palette = rememberCbPieChartPalette()
        fun colorIndex(category: RecordCategoryNet) = Math.floorMod(colorOrder.getValue(category.distributionKey), palette.pieColors.size)
        val chartCategories = categories.filter { it.amount != 0L }
        val chartSelected = chartCategories.find { it.distributionKey == selectedKey }
        val chartDescription = stringResource(SearchR.string.search_distribution_chart_count, chartCategories.size) +
            if (chartSelected != null) stringResource(SearchR.string.search_distribution_chart_selected, chartSelected.typeCategory.text + " · " + chartSelected.typeName) else ""
        val center = if (selected == null) {
            stringResource(SearchR.string.search_distribution_base) + "\n" + statistics.magnitude.toMoneyCNY()
        } else {
            selected.typeCategory.text + " · " + selected.typeName + "\n" + selected.amount.toMoneyCNY() + "\n" + distributionPercent(selected.amount, statistics.magnitude)
        }

        Surface(modifier.fillMaxWidth().testTag("search_distribution"), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(SearchR.string.search_distribution_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    CbTextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("distribution_toggle")) {
                        Text(stringResource(if (expanded) SearchR.string.search_distribution_collapse else SearchR.string.search_distribution_open))
                    }
                }
                if (expanded) {
                    val chart: @Composable () -> Unit = {
                        if (statistics.magnitude == 0L) {
                            Text(
                                stringResource(if (statistics.summary.totalCount == 0L) SearchR.string.search_distribution_no_matches else SearchR.string.search_distribution_zero),
                                Modifier.padding(vertical = 20.dp).testTag("distribution_empty"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                                CbPieChart(
                                    slices = chartCategories.map { category ->
                                        val index = colorIndex(category)
                                        PieSlice(category.typeName, (abs(category.amount).toDouble() / statistics.magnitude).toFloat(), palette.pieColors[index], palette.onPieColors[index])
                                    },
                                    centerText = "",
                                    modifier = Modifier.fillMaxWidth().height(190.dp).testTag("distribution_chart").semantics { stateDescription = chartDescription },
                                    sliceSpaceDegrees = 0f,
                                    selectedIndex = chartCategories.indexOfFirst { it.distributionKey == selectedKey },
                                    showLeaderLines = false,
                                    onSliceSelected = { index ->
                                        val key = chartCategories.getOrNull(index)?.distributionKey
                                        selectedKey = key.takeUnless { it == selectedKey }
                                    },
                                )
                                Text(center, Modifier.widthIn(max = 104.dp).testTag("distribution_center"), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    val legends: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            categories.take(5).forEach { category -> SearchDistributionLegend(category, statistics.magnitude, state, colorOrder.getValue(category.distributionKey)) }
                            if (categories.size > 5) {
                                CbTextButton(onClick = { allLegends = !allLegends }, modifier = Modifier.testTag("distribution_expand")) {
                                    Text(if (allLegends) stringResource(SearchR.string.search_distribution_collapse) else stringResource(SearchR.string.search_distribution_all, categories.size))
                                }
                            }
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth >= 600.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(Modifier.weight(1f)) { chart() }
                                Box(Modifier.weight(1f)) { legends() }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { chart(); legends() }
                        }
                    }
                    Text(stringResource(SearchR.string.search_distribution_rule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchDistributionLegend(category: RecordCategoryNet, magnitude: Long, state: SearchDistributionState, colorSlot: Int) {
    val chosen = category.distributionKey == state.selectedKey
    val palette = rememberCbPieChartPalette()
    val colorIndex = Math.floorMod(colorSlot, palette.pieColors.size)
    Row(
        Modifier.fillMaxWidth().testTag("distribution_group_${category.typeCategory.code}_${category.typeId}")
            .clip(MaterialTheme.shapes.small)
            .background(if (chosen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(role = Role.Button) { state.selectedKey = category.distributionKey.takeUnless { chosen } }
            .semantics { selected = chosen }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).background(palette.pieColors[colorIndex], CircleShape))
        Text(category.typeCategory.text + " · " + category.typeName, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Column(Modifier.widthIn(min = 68.dp, max = 132.dp), horizontalAlignment = Alignment.End) {
            Text(category.amount.toMoneyCNY(), style = MaterialTheme.typography.bodyMedium)
            Text(distributionPercent(category.amount, magnitude), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val RecordCategoryNet.distributionKey: String get() = "${typeCategory.code}:$typeId"

internal fun distributionColorOrder(categories: List<RecordCategoryNet>): Map<String, Int> =
    categories.sortedWith(compareBy<RecordCategoryNet> { it.amount == 0L }.thenBy { it.typeCategory.code }.thenBy { it.typeId })
        .mapIndexed { index, category -> category.distributionKey to index }.toMap()

/** 净额已通过模型绝对值/基数校验；极小非零比例不写成 0%。 */
internal fun distributionPercent(amount: Long, magnitude: Long, locale: Locale = Locale.getDefault()): String {
    if (amount == 0L || magnitude == 0L) return "0%"
    val value = abs(amount).toDouble() / magnitude * 100.0
    return if (value < 0.01) "<" + String.format(locale, "%.2f%%", 0.01) else String.format(locale, "%.2f%%", value)
}
