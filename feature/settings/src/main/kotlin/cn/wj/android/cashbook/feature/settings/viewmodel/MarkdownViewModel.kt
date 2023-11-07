package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MarkdownViewModel @Inject constructor(
    settingRepository: SettingRepository,
) : ViewModel() {

    private val _mutableMarkdownType = MutableStateFlow<MarkdownTypeEnum?>(null)

    val markdownData = _mutableMarkdownType.mapLatest {
        settingRepository.getContentByMarkdownType(it)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    fun updateMarkdownType(type: MarkdownTypeEnum?) {
        _mutableMarkdownType.tryEmit(type)
    }
}