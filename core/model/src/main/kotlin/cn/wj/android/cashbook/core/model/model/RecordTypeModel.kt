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
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum

/**
 * 记录类型数据模型
 *
 * @param id 主键自增长
 * @param parentId 子类时父类 id
 * @param name 类型名称
 * @param iconName 图标资源信息
 * @param typeLevel 类型等级
 * @param typeCategory 类型分类
 * @param protected 是否受保护
 * @param sort 排序
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
data class RecordTypeModel(
    val id: Long,
    val parentId: Long,
    val name: String,
    val iconName: String,
    val typeLevel: TypeLevelEnum,
    val typeCategory: RecordTypeCategoryEnum,
    val protected: Boolean,
    val sort: Int,
    val needRelated: Boolean,
)

/** 固定类型 - 平账，支出 */
val RECORD_TYPE_BALANCE_EXPENDITURE: RecordTypeModel
    get() = RecordTypeModel(
        id = -1101L,
        parentId = -1L,
        name = "平账",
        iconName = "vector_balance_account",
        typeLevel = TypeLevelEnum.FIRST,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        protected = true,
        sort = 0,
        needRelated = false,
    )

/** 固定类型 - 平账，收入 */
val RECORD_TYPE_BALANCE_INCOME: RecordTypeModel
    get() = RecordTypeModel(
        id = -1102L,
        parentId = -1L,
        name = "平账",
        iconName = "vector_balance_account",
        typeLevel = TypeLevelEnum.FIRST,
        typeCategory = RecordTypeCategoryEnum.INCOME,
        protected = true,
        sort = 0,
        needRelated = false,
    )
