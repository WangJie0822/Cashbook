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

package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [recordAmount] DAO/月度结余口径金额计算测试
 */
class RecordAmountTest {

    @Test
    fun income_subtracts_charges() {
        assertThat(recordAmount(RecordTypeCategoryEnum.INCOME, 10000, 200, 50)).isEqualTo(9800)
    }

    @Test
    fun expenditure_adds_charges_minus_concessions() {
        assertThat(recordAmount(RecordTypeCategoryEnum.EXPENDITURE, 10000, 200, 50)).isEqualTo(10150)
    }

    @Test
    fun transfer_follows_non_income_branch() {
        // 关键金丝雀：TRANSFER 走"非收入"分支 = amount + charges - concessions（当支出）
        assertThat(recordAmount(RecordTypeCategoryEnum.TRANSFER, 10000, 200, 50)).isEqualTo(10150)
    }
}
