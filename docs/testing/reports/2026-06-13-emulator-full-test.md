# Cashbook 模拟器全量功能测试报告（2026-06-13）

- 设备：Medium_Phone（OfflineDebug，APK = `app/build/outputs/apk/offline/debug/app-Offline-debug.apk`）
- 方法：android-cli journey（controller 手工驱动、LLM 评估、**非 CLI 托管可重放套件、不可 CI 化**）
- 说明：journey 为广覆盖探索；精确金额确定性由现有 JVM+DAO 测试守护（见 §1）。
- 构建：`BUILD SUCCESSFUL in 54s`（`:app:assembleOfflineDebug --offline --no-daemon`）。

> 状态：执行中，逐 journey 即时落盘。

## 0. 环境冒烟（PASS）
- 安装 OfflineDebug APK 成功，包名 `cn.wj.android.cashbook`，前台 `cn.wj.android.cashbook/.ui.MainActivity`。
- 首启隐私协议对话框正常弹出（标题"用户协议和隐私政策"，按钮"取消"/"确认"）；点"确认"后进入主界面。
- 主界面渲染正常：月收入/月支出/月结余 ¥0.00（全新安装空库），添加 FAB、菜单、搜索、日历、分析入口均在。
- 结论：构建→安装→启动→过协议→主界面 全链路 PASS。

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

**全局实证（印证评审 M4）**：
- Compose **未开 `testTagsAsResourceId`** → `core/common/TestTag.kt` 的 testTag（含 `launcher_protocol_confirm`/`launcher_title`）**不在 `android layout` 输出暴露**，journey 无法用 testTag 命中，只能靠 text/contentDesc/坐标。
- **好于预期**：首屏关键图标带 `content-desc`（添加/菜单/搜索/日历/分析），文本元素（金额 ¥x.xx、月份）可读 → 主要导航与金额回显可命中。
- 协议同意按钮实际文本 **"确认"**（journey/seed 中描述的"确定"应按"确认"解析）。
- ⚠️ **DoKit 调试浮窗**（`float_icon_id` ~[76,139]、`dokit_contentview_id`）叠加在左上角，与"菜单"[74,147] 重叠，可能拦截点击——执行抽屉导航时若菜单点击无效，先处理/拖开浮窗。
- 各管理界面（账本/资产/标签/类型/统计/报销/设置）的命中元素在对应 journey 执行时增量补录。

## 4. Bug 清单
（journey 暴露的崩溃/错屏/逻辑异常，含界面/复现步骤/证据图文件名/严重度）

- 待填

## 5. 超范围项（不计 PASS/FAIL）
- WebDAV 备份恢复（OfflineDebug 下 `OfflineWebDAVHandler` no-op）：SKIP
- Online 更新检查：N/A

## 6. 按需 instrumented（若触发）
（Phase 4 结果，未触发则标"本轮未触发"）

- 待填
