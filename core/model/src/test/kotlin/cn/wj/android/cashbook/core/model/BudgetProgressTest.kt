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

package cn.wj.android.cashbook.core.model

import cn.wj.android.cashbook.core.model.entity.BudgetStateEnum
import cn.wj.android.cashbook.core.model.entity.buildBudgetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [buildBudgetItem] 进度/状态组装纯函数测试
 */
class BudgetProgressTest {

    @Test
    fun normal_below_80_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 300)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0.3f, item.progress!!, 0.001f)
        assertEquals(0L, item.overAmount)
    }

    @Test
    fun near_at_80_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 800)
        assertEquals(BudgetStateEnum.NEAR, item.state)
    }

    @Test
    fun near_upper_bound_100() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 1000)
        assertEquals(BudgetStateEnum.NEAR, item.state)
        assertEquals(1f, item.progress!!, 0.001f)
    }

    @Test
    fun over_above_100_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 500, spent = 650)
        assertEquals(BudgetStateEnum.OVER, item.state)
        assertEquals(150L, item.overAmount)
        assertEquals(1f, item.progress!!, 0.001f) // clamp
    }

    @Test
    fun limit_zero_progress_null_normal() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 0, spent = 300)
        assertNull(item.progress)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0L, item.overAmount)
    }

    @Test
    fun spent_zero() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 0)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0f, item.progress!!, 0.001f)
    }
}
