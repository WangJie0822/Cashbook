/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.domain.usecase.UpdateRecordReimbursedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * 记录详情弹窗 ViewModel：承载「标记已报销 / 改回待报销」写动作。
 *
 * 写库后由 Repository bump 全局 recordDataVersion，触发待报销列表/主列表/详情自动刷新。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
@HiltViewModel
class RecordDetailsSheetViewModel @Inject constructor(
    private val updateRecordReimbursedUseCase: UpdateRecordReimbursedUseCase,
) : ViewModel() {

    /**
     * 标记/改回报销状态。写库成功返回 `true`，失败（如 DB 异常）返回 `false` 供 UI 提示重试。
     *
     * 设计为 `suspend` 而非内部 `launch`：由调用方在自身协程作用域 await，成功后再关闭弹窗，
     * 失败则保留弹窗并 Toast 提示，避免「fire-and-forget 静默吞异常」。
     */
    suspend fun markReimbursed(recordId: Long, reimbursed: Boolean): Boolean =
        try {
            updateRecordReimbursedUseCase(recordId, reimbursed)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            funLogger("RecordDetailsSheet")
                .e(t, "markReimbursed(recordId = <$recordId>, reimbursed = <$reimbursed>) failed")
            false
        }
}
