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
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery

/** 数据库内完成有符号分组，汇总由同一组结果安全加总。 */
internal object RecordSearchStatisticsSqlBuilder {
    fun keyword(query: RecordKeywordQuery): SupportSQLiteQuery = build(
        "FROM db_record r LEFT JOIN db_type t ON t.id = r.type_id " +
            "WHERE r.books_id = ? AND (r.remark LIKE '%' || ? || '%' OR (? != -1 AND (r.amount = ? OR r.final_amount = ?)))",
        listOf(query.bookId, query.keyword, query.amountCent, query.amountCent, query.amountCent),
    )

    fun build(fromWhere: String, args: List<Any>): SupportSQLiteQuery {
        val constants = listOf<Any>(
            RECORD_TYPE_BALANCE_EXPENDITURE.id,
            RECORD_TYPE_BALANCE_INCOME.id,
            RecordTypeCategoryEnum.INCOME.code,
            RecordTypeCategoryEnum.EXPENDITURE.code,
            RecordTypeCategoryEnum.TRANSFER.code,
            TypeLevelEnum.FIRST.ordinal,
            TypeLevelEnum.SECOND.ordinal,
            -1L,
        )
        val sql = """
            WITH constants(balanceExpense, balanceIncome, incomeCategory, expenseCategory, transferCategory, firstLevel, secondLevel, noParent) AS (VALUES (?, ?, ?, ?, ?, ?, ?, ?)),
            matched AS (
                SELECT r.type_id AS recordTypeId, t.name AS typeName, t.parent_id AS parentId,
                    t.type_category AS typeCategory, t.type_level AS typeLevel, r.final_amount AS finalAmount,
                    r.charge AS charge, r.concessions AS concessions
                $fromWhere
            ), contributions AS (
                SELECT CASE WHEN p.id IS NOT NULL THEN p.id ELSE m.recordTypeId END AS typeId,
                    CASE WHEN p.id IS NOT NULL THEN p.name ELSE m.typeName END AS typeName,
                    m.typeCategory AS typeCategory,
                    (m.recordTypeId IN (c.balanceExpense, c.balanceIncome) OR m.typeCategory IS NULL
                        OR m.typeCategory NOT IN (c.incomeCategory, c.expenseCategory, c.transferCategory)) AS excluded,
                    CASE WHEN m.recordTypeId IN (c.balanceExpense, c.balanceIncome) THEN 0
                        WHEN m.typeCategory = c.incomeCategory THEN m.finalAmount ELSE 0 END AS income,
                    CASE WHEN m.recordTypeId IN (c.balanceExpense, c.balanceIncome) THEN 0
                        WHEN m.typeCategory = c.expenseCategory THEN m.finalAmount
                        WHEN m.typeCategory = c.transferCategory THEN
                            CASE WHEN typeof(m.charge - m.concessions) = 'integer' THEN m.charge - m.concessions ELSE 0 END
                        ELSE 0 END AS expenditure,
                    CASE WHEN m.recordTypeId NOT IN (c.balanceExpense, c.balanceIncome)
                        AND m.typeCategory = c.transferCategory AND typeof(m.charge - m.concessions) != 'integer'
                        THEN 1 ELSE 0 END AS invalidAmounts
                FROM matched m CROSS JOIN constants c
                LEFT JOIN db_type p ON m.typeLevel = c.secondLevel AND p.id = m.parentId AND p.parent_id = c.noParent
                    AND p.type_level = c.firstLevel AND p.type_category = m.typeCategory
            )
            SELECT typeId, typeName, typeCategory, excluded, COUNT(*) AS totalCount,
                SUM(income) AS income, SUM(expenditure) AS expenditure, SUM(invalidAmounts) AS invalidAmounts
            FROM contributions GROUP BY typeId, typeName, typeCategory, excluded
        """.trimIndent()
        return SimpleSQLiteQuery(sql, (constants + args).toTypedArray())
    }
}
