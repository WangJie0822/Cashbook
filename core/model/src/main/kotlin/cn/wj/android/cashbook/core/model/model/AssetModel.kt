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

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum

/**
 * 资产信息数据库表
 *
 * @param id 资产主键，自增长
 * @param booksId 所属账本 id
 * @param name 资产名称
 * @param iconResId 图标资源 id
 * @param totalAmount 总额度，信用卡使用
 * @param billingDate 账单日，信用卡使用
 * @param repaymentDate 还款日，信用卡使用
 * @param type 资产大类
 * @param classification 资产分类
 * @param invisible 是否隐藏
 * @param openBank 开户行
 * @param cardNo 卡号
 * @param remark 备注
 * @param sort 排序
 * @param modifyTime 修改时间
 * @param balance 显示余额，信用卡为已使用额度
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
data class AssetModel(
    val id: Long,
    val booksId: Long,
    val name: String,
    val iconResId: Int,
    val totalAmount: String,
    val billingDate: String,
    val repaymentDate: String,
    val type: ClassificationTypeEnum,
    val classification: AssetClassificationEnum,
    val invisible: Boolean,
    val openBank: String,
    val cardNo: String,
    val remark: String,
    val sort: Int,
    val modifyTime: String,
    val balance: String,
)
