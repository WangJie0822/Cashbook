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

package cn.wj.android.cashbook.core.model.enums

/**
 * 周期记账频率枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
enum class ScheduleFrequencyEnum(val code: Int) {

    /** 每天 */
    DAILY(0),

    /** 每周 */
    WEEKLY(1),

    /** 每月 */
    MONTHLY(2),

    /** 每年 */
    YEARLY(3),
    ;

    companion object {

        val size: Int
            get() = entries.size

        /** 通过持久化的 code 值查找枚举，与 ordinal 解耦，安全添加新值 */
        fun codeOf(code: Int): ScheduleFrequencyEnum {
            return entries.first { it.code == code }
        }

        /** @see codeOf */
        fun ordinalOf(ordinal: Int): ScheduleFrequencyEnum = codeOf(ordinal)
    }
}
