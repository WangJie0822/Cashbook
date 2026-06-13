# Cashbook 模拟器全量功能测试报告（2026-06-13）

- 设备：Medium_Phone（OfflineDebug，APK = `app/build/outputs/apk/offline/debug/app-Offline-debug.apk`）
- 方法：android-cli journey（controller 手工驱动、LLM 评估、**非 CLI 托管可重放套件、不可 CI 化**）
- 说明：journey 为广覆盖探索；精确金额确定性由现有 JVM+DAO 测试守护（见 §1）。
- 构建：`BUILD SUCCESSFUL in 54s`（`:app:assembleOfflineDebug --offline --no-daemon`）。

> 状态：执行中，逐 journey 即时落盘。

## 0. 环境冒烟
（Task 0.3 Step 4 结果：协议 gate 是否过、主界面是否渲染）

- 待填

## 1. 金额基线（现有测试）
- JVM 金额单测（`:core:model:test` / `:core:data:testDebugUnitTest` / `:core:domain:testDebugUnitTest` / `:feature:records:testDebugUnitTest`）：待填
- `:core:database:connectedDebugAndroidTest`（设备级 DAO，TransactionDaoTest 1009 行覆盖 finalAmount/簇重算/余额）：待填

## 2. journey 结果汇总
（逐 journey 的 PASSED/FAILED/SKIPPED，每步 status + comment）

| journey | 结果 | 备注 |
|---|---|---|
| 00-seed | 待跑 | |
| 01-records | 待跑 | |
| 02-view | 待跑 | |
| 03-tags | 待跑 | |
| 04-types | 待跑 | |
| 05-assets | 待跑 | |
| 06-analytics | 待跑 | |
| 07-reimbursement | 待跑 | |
| 08-settings | 待跑 | |
| 09-import | 待跑 | |
| 10-search-calendar | 待跑 | |
| 11-books | 待跑 | |

## 3. semantics 命中缺口清单
（seed 阶段盘点：各 Screen 可命中元素 / 需 annotate 兜底的元素）

- 待填

## 4. Bug 清单
（journey 暴露的崩溃/错屏/逻辑异常，含界面/复现步骤/证据图文件名/严重度）

- 待填

## 5. 超范围项（不计 PASS/FAIL）
- WebDAV 备份恢复（OfflineDebug 下 `OfflineWebDAVHandler` no-op）：SKIP
- Online 更新检查：N/A

## 6. 按需 instrumented（若触发）
（Phase 4 结果，未触发则标"本轮未触发"）

- 待填
