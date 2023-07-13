package cn.wj.android.cashbook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.enums.MarkdownTypeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MarkdownViewModel @Inject constructor(
    settingRepository: SettingRepository
) : ViewModel() {

    val uiState = settingRepository.appDataMode
        .mapLatest { ActivityUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityUiState.Loading,
        )

    private val _mutableMarkdownType = MutableStateFlow<MarkdownTypeEnum?>(null)

    @OptIn(ExperimentalEncodingApi::class)
    val markdownData = combine(_mutableMarkdownType, settingRepository.gitDataModel) { type, data ->
        try {
            when (type) {
                MarkdownTypeEnum.CHANGELOG -> {
                    Base64.decode(data.changelogData).decodeToString()
                }

                MarkdownTypeEnum.PRIVACY_POLICY -> {
                    Base64.decode(data.privacyPolicyData).decodeToString()
                }

                else -> ""
            }
        } catch (throwable: Throwable) {
            logger().e(throwable, "markdownData")
            ""
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    fun updateMarkdownType(ordinal: Int) {
        _mutableMarkdownType.tryEmit(MarkdownTypeEnum.ordinalOf(ordinal))
    }
}