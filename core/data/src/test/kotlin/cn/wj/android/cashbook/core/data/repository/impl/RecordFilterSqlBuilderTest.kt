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

package cn.wj.android.cashbook.core.data.repository.impl

import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordFilterSqlBuilderTest {

    @Test
    fun recordQuery_bindsBookAndBothDateEndpoints() {
        val queries = RecordFilterSqlBuilder.build(request())
        assertThat(bindings(queries.records)).containsExactly(2L, 1000L, 2000L).inOrder()
        assertThat(queries.records.sql).doesNotContain("1000")
        assertThat(queries.records.sql).doesNotContain("2000")
    }

    @Test
    fun listAndSummary_shareFilterPredicate() {
        val queries = RecordFilterSqlBuilder.build(
            request().copy(
                typeCategories = setOf(RecordTypeCategoryEnum.EXPENDITURE),
                categories = CategoryFilterSelection(exactTypeIds = setOf(12L), wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L)),
                tagIds = setOf(41L, 42L),
            ),
        )
        assertThat(queries.records.sql.substringAfter(" FROM ").substringBefore(" ORDER BY "))
            .isEqualTo(queries.summary.sql.substringAfter(" FROM "))
        assertThat(queries.records.sql).endsWith("ORDER BY r.record_time DESC, r.id DESC")
    }

    @Test
    fun selectedIds_areAlwaysBoundRatherThanInterpolated() {
        val queries = RecordFilterSqlBuilder.build(
            request().copy(categories = CategoryFilterSelection(exactTypeIds = setOf(987654321L)), tagIds = setOf(876543219L)),
        )
        assertThat(bindings(queries.records)).containsAtLeast(987654321L, 876543219L)
        assertThat(queries.records.sql).doesNotContain("987654321")
        assertThat(queries.records.sql).doesNotContain("876543219")
    }

    @Test
    fun budget_accountsForAggregateAndPagingArguments() {
        val queries = RecordFilterSqlBuilder.build(request().copy(tagIds = (1L..901L).toSet()))
        assertThat(queries.requiredBindCount).isAtLeast(queries.records.argCount + 2)
        assertThat(queries.requiredBindCount).isAtLeast(queries.summary.argCount)
        assertThat(queries.requiredBindCount).isGreaterThan(900)
    }

    @Test
    fun mixedSelection_combinesCategoryAndTagBudgets() {
        val queries = RecordFilterSqlBuilder.build(
            request().copy(categories = CategoryFilterSelection(exactTypeIds = (1L..600L).toSet()), tagIds = (1L..300L).toSet()),
        )
        assertThat(queries.requiredBindCount).isGreaterThan(900)
    }

    private fun request() = RecordFilterRequest(2L, 1000L, 2000L, emptySet(), CategoryFilterSelection(), emptySet())

    private fun bindings(query: SupportSQLiteQuery): List<Any?> {
        val program = object : SupportSQLiteProgram {
            val values = sortedMapOf<Int, Any?>()
            override fun bindNull(index: Int) { values[index] = null }
            override fun bindLong(index: Int, value: Long) { values[index] = value }
            override fun bindDouble(index: Int, value: Double) { values[index] = value }
            override fun bindString(index: Int, value: String) { values[index] = value }
            override fun bindBlob(index: Int, value: ByteArray) { values[index] = value }
            override fun clearBindings() { values.clear() }
            override fun close() = Unit
        }
        query.bindTo(program)
        return program.values.values.toList()
    }
}
