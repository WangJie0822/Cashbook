package cn.wj.android.cashbook.core.design.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar

/**
 * 字体排版
 */
internal val CashbookTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun TypographyPreview() {
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
