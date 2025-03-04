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

package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.util.CalculatorUtils

/**
 * 计算器组件
 *
 * @param defaultText 默认文本
 * @param primaryColor 主要颜色
 * @param onConfirmClick 确认点击事件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/7
 */
@Composable
fun Calculator(defaultText: String, primaryColor: Color, onConfirmClick: (String) -> Unit) {
    var text: String by remember { mutableStateOf(defaultText.ifBlank { "0" }) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = primaryColor),
            modifier = Modifier.fillMaxWidth(),
        )

        Row {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = "0" },
                ) {
                    Text(text = "C")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "1") },
                ) {
                    Text(text = "1")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "4") },
                ) {
                    Text(text = "4")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "7") },
                ) {
                    Text(text = "7")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onBracketClick(text) },
                ) {
                    Text(text = "()")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "÷") },
                ) {
                    Text(text = "÷")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "2") },
                ) {
                    Text(text = "2")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "5") },
                ) {
                    Text(text = "5")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "8") },
                ) {
                    Text(text = "8")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "0") },
                ) {
                    Text(text = "0")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "×") },
                ) {
                    Text(text = "×")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "3") },
                ) {
                    Text(text = "3")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "6") },
                ) {
                    Text(text = "6")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "9") },
                ) {
                    Text(text = "9")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onPointClick(text) },
                ) {
                    Text(text = ".")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                CbIconButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onBackPressed(text) },
                ) {
                    Icon(imageVector = CbIcons.Backspace, contentDescription = null)
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "-") },
                ) {
                    Text(text = "-")
                }
                CbTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "+") },
                ) {
                    Text(text = "+")
                }
                CbTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .background(primaryColor),
                    onClick = {
                        if (CalculatorUtils.needShowEqualSign(text)) {
                            text = CalculatorUtils.onEqualsClick(text)
                        } else {
                            onConfirmClick(text)
                        }
                    },
                ) {
                    Text(
                        text = if (CalculatorUtils.needShowEqualSign(text)) "=" else "确认",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun CalculatorPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            Calculator(
                defaultText = "120",
                primaryColor = Color.Red,
                onConfirmClick = {},
            )
        }
    }
}
