package cn.wj.android.cashbook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.model.AppDataModel
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
        .mapLatest { MainActivityUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainActivityUiState.Loading,
        )
}

sealed interface MainActivityUiState {
    object Loading : MainActivityUiState

    data class Success(val appDataModel: AppDataModel) : MainActivityUiState
}