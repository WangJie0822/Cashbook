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

package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.R

val RecordTypeCategoryEnum.typeColor: Color
    @Composable get() = when (this) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

val RecordTypeCategoryEnum.typeContainerColor: Color
    @Composable get() = when (this) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

val RecordTypeCategoryEnum.onTypeColor: Color
    @Composable get() = when (this) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.onExpenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.onIncome
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.onTransfer
    }

val RecordTypeCategoryEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expend
            RecordTypeCategoryEnum.INCOME -> R.string.income
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer
        },
    )

val RecordTypeCategoryEnum.percentText: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expenditure_percent
            RecordTypeCategoryEnum.INCOME -> R.string.income_percent
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer_percent
        },
    )
