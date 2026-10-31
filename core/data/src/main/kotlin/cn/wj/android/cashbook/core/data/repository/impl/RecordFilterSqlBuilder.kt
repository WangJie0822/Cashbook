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

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest

internal data class RecordFilterQueries(
    val records: SupportSQLiteQuery,
    val summary: SupportSQLiteQuery,
    val statistics: SupportSQLiteQuery,
    val requiredBindCount: Int,
)

internal object RecordFilterSqlBuilder {
    fun build(request: RecordFilterRequest): RecordFilterQueries {
        val args = mutableListOf<Any>(request.bookId, request.fromInclusive, request.toExclusive)
        val predicates = mutableListOf("r.books_id = ?", "r.record_time >= ?", "r.record_time < ?")

        if (request.typeCategories.isNotEmpty()) {
            val codes = request.typeCategories.map { it.code }.sorted()
            predicates += "(CASE WHEN r.type_id = ? THEN ? WHEN r.type_id = ? THEN ? ELSE t.type_category END) IN (${placeholders(codes.size)})"
            args.addAll(listOf(RECORD_TYPE_BALANCE_EXPENDITURE.id, RecordTypeCategoryEnum.EXPENDITURE.code, RECORD_TYPE_BALANCE_INCOME.id, RecordTypeCategoryEnum.INCOME.code))
            args.addAll(codes)
        }

        val categories = request.categories
        if (!categories.isUnrestricted) {
            val includes = mutableListOf<String>()
            if (categories.exactTypeIds.isNotEmpty()) {
                val ids = categories.exactTypeIds.sorted()
                // t.id 而非仅 r.type_id：被删除的分类即使留有历史引用，也不能变成有效选择。
                includes += "t.id IN (${placeholders(ids.size)})"
                args.addAll(ids)
            }
            if (categories.wholeGroupIds.isNotEmpty()) {
                val ids = categories.wholeGroupIds.sorted()
                includes += "EXISTS (SELECT 1 FROM db_type p WHERE p.id IN (${placeholders(ids.size)}) AND p.parent_id = -1 AND p.type_level = ? AND (r.type_id = p.id OR t.parent_id = p.id))"
                args.addAll(ids)
                args += TypeLevelEnum.FIRST.ordinal
            }
            predicates += if (includes.isEmpty()) "0" else includes.joinToString(" OR ", "(", ")")
            if (categories.excludedTypeIds.isNotEmpty()) {
                val ids = categories.excludedTypeIds.sorted()
                predicates += "r.type_id NOT IN (${placeholders(ids.size)})"
                args.addAll(ids)
            }
        }

        if (request.tagIds.isNotEmpty()) {
            val ids = request.tagIds.sorted()
            // EXISTS 保证一笔多标签账单只出现一次，JOIN 标签表使已删除标签不再命中。
            predicates += "EXISTS (SELECT 1 FROM db_tag_with_record tr INNER JOIN db_tag tg ON tg.id = tr.tag_id WHERE tr.record_id = r.id AND tg.id IN (${placeholders(ids.size)}))"
            args.addAll(ids)
        }

        val fromWhere = "FROM db_record r LEFT JOIN db_type t ON t.id = r.type_id WHERE " + predicates.joinToString(" AND ")
        val records = SimpleSQLiteQuery("SELECT r.* $fromWhere ORDER BY r.record_time DESC, r.id DESC", args.toTypedArray())
        val income = "CASE WHEN r.type_id IN (?, ?) THEN 0 WHEN t.type_category = ? THEN r.final_amount ELSE 0 END"
        val expenditure = "CASE WHEN r.type_id IN (?, ?) THEN 0 WHEN t.type_category = ? THEN r.final_amount WHEN t.type_category = ? THEN r.charge - r.concessions ELSE 0 END"
        val summaryArgs = listOf<Any>(
            RECORD_TYPE_BALANCE_EXPENDITURE.id,
            RECORD_TYPE_BALANCE_INCOME.id,
            RecordTypeCategoryEnum.INCOME.code,
            RECORD_TYPE_BALANCE_EXPENDITURE.id,
            RECORD_TYPE_BALANCE_INCOME.id,
            RecordTypeCategoryEnum.EXPENDITURE.code,
            RecordTypeCategoryEnum.TRANSFER.code,
        ) + args
        val summary = SimpleSQLiteQuery(
            "SELECT COUNT(*) AS totalCount, COALESCE(SUM($income), 0) AS income, COALESCE(SUM($expenditure), 0) AS expenditure $fromWhere",
            summaryArgs.toTypedArray(),
        )
        val statistics = RecordSearchStatisticsSqlBuilder.build(fromWhere, args)
        return RecordFilterQueries(records, summary, statistics, maxOf(records.argCount + 2, summary.argCount, statistics.argCount))
    }

    private fun placeholders(size: Int): String = List(size) { "?" }.joinToString(",")
}
