package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 确认删除记录弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/4
 */
@HiltViewModel
class ConfirmDeleteRecordDialogViewModel @Inject constructor(
    private val deleteRecordUseCase: DeleteRecordUseCase,
) : ViewModel() {

    fun onDeleteRecordConfirm(recordId: Long, onResult: (ResultModel) -> Unit) {
        viewModelScope.launch {
            try {
                deleteRecordUseCase(recordId)
                // 删除成功
                onResult.invoke(ResultModel.success())
            } catch (throwable: Throwable) {
                this@ConfirmDeleteRecordDialogViewModel.logger()
                    .e(throwable, "onDeleteRecordConfirm(recordId = <$recordId>) failed")
                // 提示
                onResult.invoke(ResultModel.failure(throwable))
            }
        }
    }
}