package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.util.CalculatorUtils

@OptIn(ExperimentalMaterial3Api::class)
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
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = primaryColor),
            modifier = Modifier.fillMaxWidth(),
        )

        Row {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = "0" },
                ) {
                    Text(text = "C")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "1") }) {
                    Text(text = "1")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "4") }) {
                    Text(text = "4")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "5") }) {
                    Text(text = "5")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onBracketClick(text) }) {
                    Text(text = "()")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "÷") }) {
                    Text(text = "÷")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "2") }) {
                    Text(text = "2")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "5") }) {
                    Text(text = "5")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "8") }) {
                    Text(text = "8")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "0") }) {
                    Text(text = "0")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "×") }) {
                    Text(text = "×")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "3") }) {
                    Text(text = "3")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "6") }) {
                    Text(text = "6")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onNumberClick(text, "9") }) {
                    Text(text = "9")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onPointClick(text) }) {
                    Text(text = ".")
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                IconButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onBackPressed(text) }) {
                    Icon(imageVector = Icons.Default.Backspace, contentDescription = null)
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "-") }) {
                    Text(text = "-")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { text = CalculatorUtils.onComputeSignClick(text, "+") }) {
                    Text(text = "+")
                }
                TextButton(
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
                    }) {
                    Text(
                        text = if (CalculatorUtils.needShowEqualSign(text)) "=" else "确认",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CalculatorPreview() {
    CashbookTheme {
        Calculator(
            defaultText = "120",
            primaryColor = Color.Red,
            onConfirmClick = {},
        )
    }
}