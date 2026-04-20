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

package cn.wj.android.cashbook.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 圆角 Design Token
 *
 * 数值与 Material 3 默认保持一致，显式声明便于未来调整与单测约束
 *
 * - [Shapes.extraSmall]  4dp  — Chip、Badge
 * - [Shapes.small]       8dp  — 小按钮、TextField
 * - [Shapes.medium]     12dp  — Card
 * - [Shapes.large]      16dp  — 容器、Dialog
 * - [Shapes.extraLarge] 28dp  — BottomSheet、大容器
 */
val CashbookShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
