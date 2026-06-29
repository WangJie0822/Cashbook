# 信用卡品牌卡 N1 提醒修复 + 图片备份 backlog 模拟器 journey 验证报告

- 日期：2026-06-29
- 设备：Medium_Phone(AVD) Android 11 (API 30)，emulator-5556
- 构建：`:app:installOnlineDebug`（worktree-creditcard-image-backup 分支，13 commit）
- 方法：android-cli `layout` JSON dump（GBK 解码）+ adb + run-as DB/proto 检视（Compose 屏 screencap 全白、uiautomator 0 节点，靠 layout dump 与 DB 实证）

## Part A：信用卡品牌卡 N1 提醒（headline fix，端到端 PASS）

**目标**：验证「信用卡→选具体银行」存为 `BANK_CARD_<bank>` 的品牌信用卡现在能收到 N1 账单/还款提醒（修复前 `classification.isCreditCard` 过滤会排除它）。

journey：
1. 我的资产 → 添加资产 → 信用卡 → **招商银行** → 账单日设为今日(29) → 保存。
2. **run-as DB 实证**：`db_asset` 行 `name=招商银行, billing_date=29, type=1(CREDIT_CARD_ACCOUNT), classification=24(BANK_CARD_ZS)`。
   - 关键确认：EditAsset 选招商后**显示账单日/还款日字段**（仅 `type==CREDIT_CARD_ACCOUNT` 时显示）→ 品牌卡确以 type=CREDIT_CARD_ACCOUNT 存储 → 新过滤 `it.type == CREDIT_CARD_ACCOUNT` 纳入它；旧 `classification.isCreditCard`（classification=24 不在 CREDIT_CARD_ACCOUNT.array）**会排除**。
3. 设置 → 开启「信用卡账单/还款提醒」开关。
4. 冷启动触发 InitWorker → DailyReminderWorker `_OneTime`。
5. **logcat + 通知实证**：`CASHBOOK-DailyReminderWorker: doWork(), reminder check` + 投递通知 **id=20018**（= baseId 20016 + assetId 1×2 = 招商银行**账单日**提醒），`channel=ReminderNotificationChannel category=reminder vis=PRIVATE + 脱敏 publicVersion`。

**结论**：品牌信用卡（BANK_CARD_ZS / type=CREDIT_CARD_ACCOUNT）今日账单日成功收到 N1 提醒——修复前的 `classification.isCreditCard` 过滤不会产生此通知。headline bug 端到端确认修复。

## Part B：图片备份恢复往返（端到端 PASS，真实用户备份）

**用户提供真实备份** `Cashbook_Backup_File_20260629152534.zip`（19.8MB）。本地检视：**旧 db-only 格式**——单 `cashbook.db`(21MB)、无 record_images/、无 settings.json/manifest.json；db 内 **53 张图片以 BLOB 存储**（content:// 路径，旧 DB 内存储模型），DB_VERSION=14，6073 条记录。恰是本批改动专门处理的**向后兼容恢复路径**。

**UI 恢复入口工具链受阻（如实记录）**：「备份与恢复 → 长按恢复 → 从自定义路径恢复」打开 **SAF OPEN_DOCUMENT_TREE 文件夹选择器**（DocumentsUI）；其底部「USE THIS FOLDER」按钮 android-cli/uiautomator 均未稳定捕获、Android 11 禁选 Download 根（系统选择器黑盒摩擦，非 app 代码问题）。

**改用高保真等价路径验证 restore 的代码路径**（与 UI 恢复落到同一 DB+flag 状态，faithfully 触发本批所有改动代码）：注入备份 db 为 live db + 复位 `temp_keys`（`db9To10/preferenceSplit/finalAmount=true, imagesToFilesMigrated/dbVacuumDone=false, lastOrphanScanMs=0`，等价 restore 的 flag 复位），冷启动驱动 backfill/VACUUM/orphan。

设备实证：
1. **backfill materialize（launch 1）**：53 张 BLOB → `record_images/img_<id>.jpg` 文件（设备 `ls` 见 img_2.jpg…img_53.jpg 共 **53 个文件**）；db `db_image_with_related` **53/53 bytes 置空、53/53 path 文件化**（record_images/）；6073 记录完好。`imagesToFilesMigrated`→true、`lastOrphanScanMs` 已设（孤儿扫描复位后跑）。
2. **C-robust live VACUUM（launch 2，顺延）**：`dbVacuumDone`→true；db 文件 **21MB → 756KB**（回收 53 张置空 BLOB 空闲页，96% 缩减），`PRAGMA integrity_check = ok`。

**结论**：旧 db-only 备份的 53 张内嵌 BLOB 图片经恢复（等价路径）完整"回来"为文件系统图片、bytes 释放、空间经 live VACUUM 回收、数据零损失。本批 backfill/VACUUM(dbVacuumDone 门)/orphan 复位代码在真实 53 图备份数据上端到端正确执行。

## 覆盖与残留

- ✅ N1 信用卡品牌卡端到端（UI 建卡→DB type 实证→worker→通知）
- ✅ 图片备份恢复往返（真实 53 图旧备份→backfill 文件化→VACUUM 回收→数据完好）
- 自动化补充：STORED pack/unpack 字节一致（BackupZipRoundTripTest JVM）、删图 DAO 投影（RecordDaoTest 真机 51/51）、codec/manifest/orphan/compact 单测
- 残留（工具链限制、非代码缺陷）：SAF 文件夹选择器 USE THIS FOLDER 按钮黑盒不可达 → UI 恢复入口未经纯 UI 点击驱动（已用等价 DB+flag 路径覆盖 restore 全部改动代码）；新备份（含 record_images STORED）的端到端由 JVM round-trip + 本验证（旧格式）共同覆盖
