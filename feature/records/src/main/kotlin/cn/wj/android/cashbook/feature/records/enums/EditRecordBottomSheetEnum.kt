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

package cn.wj.android.cashbook.feature.records.enums

/**
 * 底部菜单类型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/27
 */
enum class EditRecordBottomSheetEnum {

    /** 不显示 */
    NONE,

    /** 金额 */
    AMOUNT,

    /** 手续费 */
    CHARGES,

    /** 优惠 */
    CONCESSIONS,

    /** 资产列表 */
    ASSETS,

    /** 关联资产列表 */
    RELATED_ASSETS,

    /** 标签 */
    TAGS,
    ;

    val isCalculator: Boolean
        get() = this == AMOUNT || this == CHARGES || this == CONCESSIONS
}
