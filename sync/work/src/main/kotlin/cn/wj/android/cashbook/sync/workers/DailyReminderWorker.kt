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

package cn.wj.android.cashbook.sync.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.sync.R
import cn.wj.android.cashbook.sync.initializers.ReminderNotificationBaseId
import cn.wj.android.cashbook.sync.initializers.reminderNotificationBuilder
import cn.wj.android.cashbook.sync.reminder.CreditCardReminderInfo
import cn.wj.android.cashbook.sync.reminder.ReminderItem
import cn.wj.android.cashbook.sync.reminder.reminderCheckDates
import cn.wj.android.cashbook.sync.reminder.reminderDeepLinkIntent
import cn.wj.android.cashbook.sync.reminder.reminderNotificationId
import cn.wj.android.cashbook.sync.reminder.reminderRun
import cn.wj.android.cashbook.sync.reminder.toNotificationSpec
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/** 提醒触发时刻（本地时区，每日对齐到此小时） */
private const val REMINDER_HOUR = 10

/**
 * 每日提醒任务（薄壳）：取数据 → 调纯函数 [reminderRun] 编排判定 → 发系统通知。
 *
 * 始终注册（[cn.wj.android.cashbook.sync.workers.InitWorker] 中），两开关全关时直接 success 空转。
 * 补发由 [reminderRun] 内按逻辑日期区间逐日判定，缓解 Doze/OEM 漏触发。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/26
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    private val assetRepository: AssetRepository,
    private val recordRepository: RecordRepository,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        this@DailyReminderWorker.logger().i("doWork(), reminder check")
        val settings = settingRepository.appSettingsModel.first()
        if (!settings.creditCardReminderEnable && !settings.reimbursementReminderEnable) {
            // 两开关全关，跳查 repo 空转
            return@withContext Result.success()
        }
        val zone = ZoneId.systemDefault()
        val todayMs = System.currentTimeMillis()
        // 补发区间为空（当日已查过）时短路，避免无谓 repo 读取（与重构前行为一致）
        if (reminderCheckDates(settings.lastReminderCheckMs, todayMs, zone).isEmpty()) {
            return@withContext Result.success()
        }
        val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
        val creditCards = if (settings.creditCardReminderEnable) {
            assetRepository.currentVisibleAssetListData.first()
                .filter { it.classification.isCreditCard }
                .map { CreditCardReminderInfo(it.id, it.name, it.billingDate, it.repaymentDate) }
        } else {
            emptyList()
        }
        val reimbursableCount = if (settings.reimbursementReminderEnable) {
            recordRepository.getReimbursableUnrelatedRecordList().size
        } else {
            0
        }

        val run = reminderRun(
            lastReminderCheckMs = settings.lastReminderCheckMs,
            todayMs = todayMs,
            zone = zone,
            creditCardEnable = settings.creditCardReminderEnable,
            reimbursementEnable = settings.reimbursementReminderEnable,
            creditCards = creditCards,
            monthStartDay = monthStartDay,
            reimbursableCount = reimbursableCount,
        )
        run.items.forEach { notify(it) }
        run.newLastCheckMs?.let { settingRepository.updateLastReminderCheckMs(it) }
        Result.success()
    }

    private fun notify(item: ReminderItem) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val id = reminderNotificationId(ReminderNotificationBaseId, item)
        val spec = item.toNotificationSpec()
        sendNotification(
            nm = nm,
            id = id,
            text = appContext.getString(spec.textRes, *spec.formatArgs.toTypedArray()),
            intent = reminderDeepLinkIntent(appContext, spec.target, spec.assetId),
        )
    }

    private fun sendNotification(nm: NotificationManager, id: Int, text: String, intent: PendingIntent) {
        val publicVersion = appContext.reminderNotificationBuilder()
            .setContentText(appContext.getString(R.string.reminder_public_title))
            .build()
        val notification = appContext.reminderNotificationBuilder()
            .setContentText(text)
            .setContentIntent(intent)
            .setPublicVersion(publicVersion)
            .build()
        nm.notify(id, notification)
    }

    companion object {

        /** 启动周期提醒任务（每日，对齐到下一个 [REMINDER_HOUR] 点） */
        fun startUpPeriodicReminderWork(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<DelegatingWorker>(Duration.ofDays(1))
                .setInitialDelay(initialDelayToNext(REMINDER_HOUR), TimeUnit.MILLISECONDS)
                .setInputData(DailyReminderWorker::class.delegatedData())
                .build()
    }
}

/**
 * 计算从现在 [nowMs] 到下一个指定小时 [hour]（本地时区 [zone]）的毫秒延迟。
 * 若当前已过该时刻，则对齐到次日同一时刻。
 */
internal fun initialDelayToNext(
    hour: Int,
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val now = Instant.ofEpochMilli(nowMs).atZone(zone)
    var target = now.toLocalDate().atTime(hour, 0).atZone(zone)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return target.toInstant().toEpochMilli() - nowMs
}
