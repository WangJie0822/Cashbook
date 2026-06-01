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

package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [BillCategoryMatcher] 账单分类关键词匹配测试。
 *
 * 全部使用脱敏数据：商户名为虚构占位，关键词使用公开品牌/通用词（非个人隐私）。
 */
class BillCategoryMatcherTest {

    @Test
    fun matches_food_category_by_keyword() {
        val types = listOf(createType(1L, "餐饮"), createType(2L, "交通"))
        // "外卖" 命中餐饮关键词
        val result = BillCategoryMatcher.match("测试商户A", "外卖订单", types)
        assertThat(result?.name).isEqualTo("餐饮")
    }

    @Test
    fun matches_transport_category_by_keyword() {
        val types = listOf(createType(1L, "餐饮"), createType(2L, "交通"))
        // "停车" 命中交通关键词
        val result = BillCategoryMatcher.match("测试停车场", "停车费", types)
        assertThat(result?.name).isEqualTo("交通")
    }

    @Test
    fun returns_null_when_no_keyword_hit() {
        val types = listOf(createType(1L, "餐饮"))
        val result = BillCategoryMatcher.match("未知商户", "无关描述零零", types)
        assertThat(result).isNull()
    }

    @Test
    fun returns_null_when_keyword_hit_but_type_absent() {
        // 命中"餐饮"规则，但用户分类列表里没有餐饮分类
        val types = listOf(createType(1L, "交通"))
        val result = BillCategoryMatcher.match("测试商户B", "外卖", types)
        assertThat(result).isNull()
    }

    private fun createType(id: Long, name: String) = RecordTypeModel(
        id = id,
        parentId = -1L,
        name = name,
        iconName = "icon",
        typeLevel = TypeLevelEnum.FIRST,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        protected = false,
        sort = 0,
        needRelated = false,
    )
}
