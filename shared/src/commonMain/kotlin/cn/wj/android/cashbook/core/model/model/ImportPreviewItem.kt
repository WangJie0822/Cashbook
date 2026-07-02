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

/**
 * 单条导入预览
 *
 * @param billItem 原始账单条目
 * @param mappedTypeId 自动匹配的分类 ID
 * @param mappedTypeName 分类名称
 * @param mappedAssetId 映射的资产 ID
 * @param duplicateStatus 重复检测结果
 * @param selected 用户是否选择导入此条
 */
data class ImportPreviewItem(
    val billItem: ImportedBillItem,
    val mappedTypeId: Long,
    val mappedTypeName: String,
    val mappedAssetId: Long,
    val duplicateStatus: DuplicateStatus,
    val selected: Boolean,
)

/** 重复检测结果 */
enum class DuplicateStatus {
    /** 无重复 */
    NONE,

    /** 可能重复（金额+时间匹配） */
    POSSIBLE,

    /** 精确重复（交易单号匹配） */
    EXACT,
}
