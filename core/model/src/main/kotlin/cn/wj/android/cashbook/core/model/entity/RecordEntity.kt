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

package cn.wj.android.cashbook.core.model.entity

/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 *  @param booksId 关联账本 id
 * @param typeId 记录类型 id
 * @param assetId 关联资产 id
 * @param relatedAssetId 转账转入资产 id
 * @param amount 记录金额
 * @param finalAmount 最终金额
 * @param charges 转账手续费
 * @param concessions 优惠
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param recordTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordEntity(
    val id: Long,
    val booksId: Long,
    val typeId: Long,
    val assetId: Long,
    val relatedAssetId: Long,
    /** 金额，单位：分 */
    val amount: Long,
    /** 最终金额，单位：分 */
    val finalAmount: Long,
    /** 手续费，单位：分 */
    val charges: Long,
    /** 优惠，单位：分 */
    val concessions: Long,
    val remark: String,
    val reimbursable: Boolean,
    /** 记录时间，毫秒时间戳 */
    val recordTime: Long,
)
