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

package cn.wj.android.cashbook.feature.schedule.model

import cn.wj.android.cashbook.core.model.model.ScheduleModel

/**
 * 周期记账列表展示数据实体类
 *
 * @param schedule 周期规则数据
 * @param typeName 类型名称
 * @param typeIconResName 类型图标资源名
 * @param assetName 资产名称
 * @param tagNameList 标签名称列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/21
 */
data class ScheduleViewsEntity(
    val schedule: ScheduleModel,
    val typeName: String,
    val typeIconResName: String,
    val assetName: String?,
    val tagNameList: List<String>,
)
