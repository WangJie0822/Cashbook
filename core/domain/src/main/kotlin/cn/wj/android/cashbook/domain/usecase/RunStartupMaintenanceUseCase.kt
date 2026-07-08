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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 启动维护编排（从 [LauncherContentViewModel][cn.wj.android.cashbook.feature.records] init 收编）：
 * 迁移 / 净自付重算 / 图片 backfill / DB 压实 / 孤儿扫描。
 *
 * [onFirstScreenReady] 在正确时机回调决定首屏放行——gate 语义与原 init 逐行等价、逻辑零改动：
 * - db9To10 未迁移：先 [RecordRepository.migrateAfter9To10]（final_amount 全为 Migration9To10 DEFAULT 0，
 *   首屏须待迁移完成，**不包 try/catch**——异常逃逸触发全局 UncaughtExceptionHandler.finishAllActivity，
 *   标志未置位下次幂等重试）再放行；
 * - 已迁移：立即放行，后台按 tempKeys 标志跑净自付重算 / 图片 backfill / DB 压实。
 * - 孤儿扫描每次启动兜底（批量删账本/资产、编辑替换可能留孤儿文件）。
 */
class RunStartupMaintenanceUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val settingRepository: SettingRepository,
) {

    suspend operator fun invoke(onFirstScreenReady: () -> Unit) {
        val tempKeys = settingRepository.tempKeysModel.first()
        if (!tempKeys.db9To10DataMigrated) {
            recordRepository.migrateAfter9To10()
            onFirstScreenReady()
        } else {
            onFirstScreenReady()
            if (!tempKeys.finalAmountNetRecalcDone) {
                runCatchingMaintenance("netRecalc") { recordRepository.recalculateAllFinalAmount() }
            }
            if (!tempKeys.imagesToFilesMigrated) {
                runCatchingMaintenance("backfill") { recordRepository.backfillImagesToFiles() }
            }
            if (tempKeys.imagesToFilesMigrated && !tempKeys.dbVacuumDone) {
                runCatchingMaintenance("compact") { recordRepository.compactDatabaseIfNeeded() }
            }
        }
        runCatchingMaintenance("orphanScan") { recordRepository.cleanupOrphanImageFiles() }
    }

    /** 后台维护步骤统一容错：CancellationException 先 rethrow（不吞协程取消），其余记日志不连累后续步骤。 */
    private suspend fun runCatchingMaintenance(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            this.logger().e(t, "startup maintenance <$tag> failed, will retry next launch")
        }
    }
}
