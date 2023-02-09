package cn.wj.android.cashbook.feature.record.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.feature.record.viewmodel.RecordMainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun MainRoute(
    modifier: Modifier = Modifier,
    viewModel: RecordMainViewModel = koinViewModel()
) {
    MainScreen()
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen() {
    ModalNavigationDrawer(drawerContent = {
        for (i in 0 until 10) {
            Text(text = "目录 $i")
        }
    }) {
        Scaffold {
            Column {
                for (i in 0 until 10) {
                    Text(text = "记录 $i")
                }
            }
        }
    }
}