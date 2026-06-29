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

package cn.wj.android.cashbook.core.data.uitl

/** 孤儿图片扫描节流窗口：7 天。删资产/账本/编辑路径已各自删文件后，孤儿扫描退为纯兜底，无需每启动跑。 */
const val ORPHAN_SCAN_THROTTLE_MS: Long = 7L * 24 * 60 * 60 * 1000

/**
 * 孤儿图片扫描节流判定：从未扫（[lastScanMs] ≤ 0）即扫，否则距上次扫描达到 [throttleMs] 才再扫。
 *
 * @param lastScanMs 上次扫描时间戳（≤0=从未扫/已复位，首启即扫）
 * @param nowMs 当前时间戳
 * @param throttleMs 节流窗口
 */
internal fun shouldRunOrphanScan(lastScanMs: Long, nowMs: Long, throttleMs: Long): Boolean =
    lastScanMs <= 0L || nowMs - lastScanMs >= throttleMs
