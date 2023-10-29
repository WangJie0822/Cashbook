package cn.wj.android.cashbook.compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    settingRepository: SettingRepository
) : ViewModel() {

    val uiState = settingRepository.appDataMode
        .mapLatest {
            ActivityUiState.Success(
                darkMode = it.darkMode,
                dynamicColor = it.dynamicColor,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityUiState.Loading,
        )
}