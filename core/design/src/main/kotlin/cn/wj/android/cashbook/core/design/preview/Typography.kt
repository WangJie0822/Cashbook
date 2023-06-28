package cn.wj.android.cashbook.core.design.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
internal fun TypographyPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            CashbookScaffold(
                topBar = {
                    CashbookTopAppBar(onBackClick = {}, text = "titleLarge")
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier.padding(paddingValues),
                ) {
                    Text(text = "displayLarge", style = MaterialTheme.typography.displayLarge)
                    Text(text = "displayMedium", style = MaterialTheme.typography.displayMedium)
                    Text(text = "displaySmall", style = MaterialTheme.typography.displaySmall)
                    Text(text = "headlineLarge", style = MaterialTheme.typography.headlineLarge)
                    Text(text = "headlineMedium", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "headlineSmall", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "titleLarge", style = MaterialTheme.typography.titleLarge)
                    Text(text = "titleMedium", style = MaterialTheme.typography.titleMedium)
                    Text(text = "titleSmall", style = MaterialTheme.typography.titleSmall)
                    Text(text = "bodyLarge", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "bodyMedium", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "bodySmall", style = MaterialTheme.typography.bodySmall)
                    Text(text = "labelLarge", style = MaterialTheme.typography.labelLarge)
                    Text(text = "labelMedium", style = MaterialTheme.typography.labelMedium)
                    Text(text = "labelSmall", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}