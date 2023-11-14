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
 * 编辑记录提示枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
enum class EditRecordBookmarkEnum {

    /** 无提示 */
    NONE,

    /** 金额不能为 0 */
    AMOUNT_MUST_NOT_BE_ZERO,

    /** 类型不能为空 */
    TYPE_MUST_NOT_BE_NULL,

    /** 类型不匹配 */
    TYPE_NOT_MATCH_CATEGORY,

    /** 保存失败 */
    SAVE_FAILED,
}
