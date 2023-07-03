package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoViewModel

@Composable
internal fun AssetInfoRoute(
    assetId: Long,
    assetRecordListContent: LazyListScope.() -> Unit,
    onEditAssetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssetInfoViewModel = hiltViewModel<AssetInfoViewModel>().apply {
        updateAssetId(assetId)
    },
) {

    val assetName by viewModel.assetName.collectAsStateWithLifecycle()
    val isCreditCard by viewModel.isCreditCard.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()
    val billingDate by viewModel.billingDate.collectAsStateWithLifecycle()
    val repaymentDate by viewModel.repaymentDate.collectAsStateWithLifecycle()

    AssetInfoScreen(
        assetName = assetName,
        isCreditCard = isCreditCard,
        balance = balance,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        assetRecordListContent = assetRecordListContent,
        onEditAssetClick = onEditAssetClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssetInfoScreen(
    assetName: String,
    isCreditCard: Boolean,
    balance: String,
    totalAmount: String,
    billingDate: String,
    repaymentDate: String,
    assetRecordListContent: LazyListScope.() -> Unit,
    onEditAssetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                text = assetName,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onEditAssetClick) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            content = {
                item {
                    AssetInfoContent(
                        isCreditCard = isCreditCard,
                        balance = balance,
                        totalAmount = totalAmount,
                        billingDate = billingDate,
                        repaymentDate = repaymentDate,
                    )
                }

                assetRecordListContent()
            },
        )
    }
}

@Composable
private fun AssetInfoContent(
    isCreditCard: Boolean,
    balance: String,
    totalAmount: String,
    billingDate: String,
    repaymentDate: String
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = if (isCreditCard) R.string.current_debt else R.string.asset_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = balance.withCNY(),
                    style = MaterialTheme.typography.titleLarge
                )
                if (isCreditCard) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            Text(
                                text = totalAmount.withCNY(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.billing_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            val billingDateText = if (billingDate.isBlank()) {
                                stringResource(id = R.string.un_set)
                            } else {
                                billingDate + stringResource(id = R.string.day)
                            }
                            Text(
                                text = billingDateText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.repayment_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            val repaymentDateText = if (repaymentDate.isBlank()) {
                                stringResource(id = R.string.un_set)
                            } else {
                                repaymentDate + stringResource(id = R.string.day)
                            }
                            Text(
                                text = repaymentDateText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun AssetInfoScreenPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            AssetInfoScreen(
                assetName = "现金",
                isCreditCard = false,
                balance = "200",
                totalAmount = "0",
                billingDate = "",
                repaymentDate = "",
                assetRecordListContent = {
                    item {
                        Empty(
                            imagePainter = painterResource(id = R.drawable.vector_no_data_200),
                            hintText = "当前资产还没有记录数据"
                        )
                    }
                },
                onEditAssetClick = {},
                onBackClick = { },
                modifier = Modifier,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CreditAssetInfoScreenPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            AssetInfoScreen(
                assetName = "招商银行",
                isCreditCard = true,
                balance = "200",
                totalAmount = "2000",
                billingDate = "20",
                repaymentDate = "",
                assetRecordListContent = {
                    item {
                        Empty(
                            imagePainter = painterResource(id = R.drawable.vector_no_data_200),
                            hintText = "当前资产还没有记录数据"
                        )
                    }
                },
                onEditAssetClick = {},
                onBackClick = { },
                modifier = Modifier,
            )
        }
    }
}