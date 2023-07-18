package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.SyncManager
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MarkdownViewModel @Inject constructor(
    settingRepository: SettingRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _mutableMarkdownType = MutableStateFlow<MarkdownTypeEnum?>(null)

    @OptIn(ExperimentalEncodingApi::class)
    val markdownData = combine(_mutableMarkdownType, settingRepository.gitDataModel) { type, data ->
        try {
            when (type) {
                MarkdownTypeEnum.CHANGELOG -> {
                    Base64.Mime.decode(data.changelogData).decodeToString()
                }

                MarkdownTypeEnum.PRIVACY_POLICY -> {
                    Base64.Mime.decode(data.privacyPolicyData).decodeToString()
                }

                else -> "No selected type"
            }
        } catch (throwable: Throwable) {
            logger().e(throwable, "markdownData")
            "Data decode error"
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    val isSyncing = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun onRetryClick() {
        viewModelScope.launch {
            if (!isSyncing.first()) {
                syncManager.requestSync()
            }
        }
    }

    fun updateMarkdownType(type: MarkdownTypeEnum?) {
        _mutableMarkdownType.tryEmit(type)
    }
}