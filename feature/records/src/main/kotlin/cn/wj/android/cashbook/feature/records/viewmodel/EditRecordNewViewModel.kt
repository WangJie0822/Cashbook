package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/5
 */
@HiltViewModel
class EditRecordNewViewModel @Inject constructor(
    recordRepository: RecordRepository,
    getDefaultRecordUseCase: GetDefaultRecordUseCase,
) : ViewModel() {

    /** 显示提示类型 */
    var shouldDisplayBookmark by mutableStateOf(EditRecordBookmarkEnum.NONE)

    /** 记录 id */
    private val recordIdData = MutableStateFlow(-1L)

    /** 默认记录数据 */
    private val defaultRecordData = recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }

//    private val mutableTypeId

    fun updateRecordId(id: Long) {
        recordIdData.tryEmit(id)
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = EditRecordBookmarkEnum.NONE
    }
}