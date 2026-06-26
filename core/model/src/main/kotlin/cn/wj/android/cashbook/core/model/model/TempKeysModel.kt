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
 * 临时开关数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/1/20
 */
data class TempKeysModel(
    /** 数据库数据是否执行版本9到版本10迁移 */
    val db9To10DataMigrated: Boolean,
    /** proto app_preferences 是否拆分 */
    val preferenceSplit: Boolean,
    /** finalAmount 净自付重算是否已执行 */
    val finalAmountNetRecalcDone: Boolean = false,
    /** 图片 BLOB→文件系统 backfill 是否已完成 */
    val imagesToFilesMigrated: Boolean = false,
)
