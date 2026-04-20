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
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum

/**
 * 周期记账规则数据实体类
 *
 * @param id 主键自增长
 * @param booksId 所属账本主键
 * @param typeId 类型主键
 * @param assetId 资产主键
 * @param amount 金额（单位：分）
 * @param charges 手续费（单位：分）
 * @param concessions 优惠（单位：分）
 * @param remark 备注
 * @param typeCategory 类型分类（支出/收入）
 * @param frequency 周期频率
 * @param startDate 开始日期（时间戳）
 * @param endDate 结束日期（时间戳，可选）
 * @param recordTime 记账时间（一天中的具体时刻，时间戳）
 * @param lastExecutedDate 上次执行日期（时间戳）
 * @param enabled 是否启用
 * @param reimbursable 是否可报销
 * @param tagIdList 标签 id 列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
data class ScheduleModel(
    val id: Long,
    val booksId: Long,
    val typeId: Long,
    val assetId: Long,
    val amount: Long,
    val charges: Long,
    val concessions: Long,
    val remark: String,
    val typeCategory: RecordTypeCategoryEnum,
    val frequency: ScheduleFrequencyEnum,
    val startDate: Long,
    val endDate: Long?,
    val recordTime: Long,
    val lastExecutedDate: Long?,
    val enabled: Boolean,
    val reimbursable: Boolean,
    val tagIdList: List<Long>,
)
