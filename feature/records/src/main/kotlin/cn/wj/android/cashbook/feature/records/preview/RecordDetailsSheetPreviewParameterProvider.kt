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

package cn.wj.android.cashbook.feature.records.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.R

/**
 * 记录详情 Sheet 预览参数提供者
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/3/30
 */
class RecordDetailsSheetPreviewParameterProvider : PreviewParameterProvider<RecordViewsEntity?> {
    override val values: Sequence<RecordViewsEntity?>
        get() = RecordDetailsSheetData.recordViewsList.asSequence()
}

object RecordDetailsSheetData {

    private val tagList = listOf(
        TagModel(1L, "标签1", true),
        TagModel(2L, "标签2", false),
        TagModel(3L, "标签3", false),
    )

    private val relatedRecordList = listOf(
        RecordModel(1L, 1L, 1L, 1L, 1L, "20", "", "", "", false, "2024-01-01"),
    )

    private val nullRecordViewsData: RecordViewsEntity? = null

    private val expenditureRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "三餐",
        typeIconResName = "vector_type_three_meals_24",
        assetName = "微信",
        assetIconResId = R.drawable.vector_wechat_circle_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "66.6",
        charges = "0.4",
        concessions = "7",
        remark = "支出数据",
        reimbursable = false,
        relatedTags = tagList,
        relatedRecord = emptyList(),
        relatedAmount = "",
        recordTime = "2024-04-01 ",
    )

    private val reimbursedExpenditureRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "酒店",
        typeIconResName = "vector_type_hotel_24",
        assetName = "支付宝",
        assetIconResId = R.drawable.vector_alipay_circle_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "66.6",
        charges = "",
        concessions = "",
        remark = "已报销支出数据",
        reimbursable = true,
        relatedTags = emptyList(),
        relatedRecord = relatedRecordList,
        relatedAmount = "20",
        recordTime = "2024-04-01 ",
    )

    private val reimbursableExpenditureRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "酒店",
        typeIconResName = "vector_type_hotel_24",
        assetName = "支付宝",
        assetIconResId = R.drawable.vector_alipay_circle_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "66.6",
        charges = "",
        concessions = "",
        remark = "可报销支出数据",
        reimbursable = true,
        relatedTags = emptyList(),
        relatedRecord = emptyList(),
        relatedAmount = "",
        recordTime = "2024-04-01 ",
    )

    private val refundExpenditureRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "购物",
        typeIconResName = "vector_type_shopping_24",
        assetName = "招商银行",
        assetIconResId = R.drawable.vector_bank_zs_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "66.6",
        charges = "0.4",
        concessions = "7",
        remark = "已退款支出数据",
        reimbursable = false,
        relatedTags = tagList,
        relatedRecord = relatedRecordList,
        relatedAmount = "20",
        recordTime = "2024-04-01 ",
    )

    private val incomeRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.INCOME,
        typeName = "薪资",
        typeIconResName = "vector_type_salary_24",
        assetName = "民生银行",
        assetIconResId = R.drawable.vector_bank_ms_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "20000",
        charges = "10",
        concessions = "",
        remark = "收入数据",
        reimbursable = false,
        relatedTags = tagList,
        relatedRecord = emptyList(),
        relatedAmount = "",
        recordTime = "2024-04-01 ",
    )

    private val refundIncomeRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.INCOME,
        typeName = "退款",
        typeIconResName = "vector_type_refund_24",
        assetName = "抖音",
        assetIconResId = R.drawable.vector_douyin_circle_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "200",
        charges = "",
        concessions = "",
        remark = "退款收入数据",
        reimbursable = false,
        relatedTags = emptyList(),
        relatedRecord = relatedRecordList,
        relatedAmount = "2000",
        recordTime = "2024-04-01 ",
    )

    private val reimbursedIncomeRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.INCOME,
        typeName = "报销",
        typeIconResName = "vector_type_reimburse_24",
        assetName = "现金",
        assetIconResId = R.drawable.vector_cash_circle_24,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = "66.6",
        charges = "",
        concessions = "",
        remark = "已报销收入数据",
        reimbursable = true,
        relatedTags = tagList,
        relatedRecord = relatedRecordList,
        relatedAmount = "20",
        recordTime = "2024-04-01 ",
    )

    private val transferRecordViewsData = RecordViewsEntity(
        id = 1L,
        typeCategory = RecordTypeCategoryEnum.TRANSFER,
        typeName = "还信用卡",
        typeIconResName = "vector_type_credit_card_payment_24",
        assetName = "北京银行",
        assetIconResId = R.drawable.vector_bank_bj_24,
        relatedAssetName = "花呗",
        relatedAssetIconResId = R.drawable.vector_ant_credit_pay_circle_24,
        amount = "66.6",
        charges = "",
        concessions = "",
        remark = "转账数据",
        reimbursable = true,
        relatedTags = emptyList(),
        relatedRecord = emptyList(),
        relatedAmount = "",
        recordTime = "2024-04-01 ",
    )

    val recordViewsList = arrayListOf(
        nullRecordViewsData,
        expenditureRecordViewsData,
        reimbursedExpenditureRecordViewsData,
        reimbursableExpenditureRecordViewsData,
        refundExpenditureRecordViewsData,
        incomeRecordViewsData,
        refundIncomeRecordViewsData,
        reimbursedIncomeRecordViewsData,
        transferRecordViewsData,
    )
}
