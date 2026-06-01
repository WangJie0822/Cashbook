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

import cn.wj.android.cashbook.core.model.model.BillDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [WechatBillParser] 纯函数测试（parseDateTime / convertToItem）。
 *
 * parseDateTime 断言使用相对等价（ISO==标准）+ 非空，避免硬编码绝对毫秒带来的时区脆弱。
 * convertToItem 全部使用脱敏 row 数据。
 */
class WechatBillParserTest {

    @Test
    fun parseDateTime_iso_and_standard_are_equal() {
        // parseDateTime 把 'T' 替换为空格后用同一 DATE_FORMAT 解析，两者应等价
        val iso = WechatBillParser.parseDateTime("2026-03-26T11:50:04")
        val standard = WechatBillParser.parseDateTime("2026-03-26 11:50:04")
        assertThat(iso).isNotNull()
        assertThat(iso).isEqualTo(standard)
    }

    @Test
    fun parseDateTime_excel_serial_parses_to_non_null() {
        assertThat(WechatBillParser.parseDateTime("46107.493101851855")).isNotNull()
    }

    @Test
    fun parseDateTime_invalid_returns_null() {
        assertThat(WechatBillParser.parseDateTime("不是日期")).isNull()
    }

    @Test
    fun convertToItem_maps_fields_and_amount() {
        // 列：时间/类型/对方/商品/收支/金额/支付方式/状态/交易单号/商户单号/备注（全脱敏）
        val row = listOf(
            "2026-03-26 11:50:04", "商户消费", "测试商户A", "测试商品",
            "支出", "¥100.50", "零钱", "支付成功", "0000", "0000", "/",
        )
        val item = WechatBillParser.convertToItem(row)
        assertThat(item).isNotNull()
        assertThat(item!!.direction).isEqualTo(BillDirection.EXPENDITURE)
        assertThat(item.amount).isEqualTo(100.50)
        assertThat(item.counterparty).isEqualTo("测试商户A")
        // 备注为 "/" 时归一化为空串
        assertThat(item.remark).isEmpty()
    }

    @Test
    fun convertToItem_neutral_direction_returns_null() {
        val row = listOf(
            "2026-03-26 11:50:04", "x", "y", "z", "中性交易",
            "¥0", "/", "ok", "/", "/", "/",
        )
        assertThat(WechatBillParser.convertToItem(row)).isNull()
    }

    @Test
    fun convertToItem_too_few_columns_returns_null() {
        assertThat(WechatBillParser.convertToItem(listOf("2026-03-26 11:50:04", "支出"))).isNull()
    }
}
