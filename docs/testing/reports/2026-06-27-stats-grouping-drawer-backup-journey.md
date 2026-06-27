# 模拟器 Journey 验证报告：统计页日期头自适应 + 首页抽屉返回 + 备份恢复

- 日期：2026-06-27
- 环境：android-cli 启 `Medium_Phone` 模拟器（API30，WHPX 加速）；`assembleOnlineDebug`（非 Dev flavor，恢复缓存自动清）安装；包名 `cn.wj.android.cashbook`
- 关联：spec `docs/superpowers/specs/2026-06-27-stats-grouping-drawer-backup-design.md`、plan `docs/superpowers/plans/2026-06-27-stats-grouping-drawer-backup.md`
- 脱敏声明：本报告**不含**真实金额/资产名/备注/卡号/DB 原始行；仅用非识别性聚合量（记录数、日期跨度、图片数）。拉取的 DB/zip/截图仅存会话 scratchpad（仓库外），验后删除；测试结束 `pm clear` + 删除设备备份文件清除真实数据。

## 1. Issue 2：首页抽屉返回键不收起 — 已修复并真机验证

### 复现（修复前 APK）
- 左缘右滑打开首页左侧导航抽屉 → 抽屉显示。
- 按系统返回键（`KEYCODE_BACK`）→ **抽屉未收起**（截图前后一致），且 **app 未退出**。
- 对照：纯首页（无抽屉）按返回 → app 正常退到 launcher（`ResumedActivity` 变 NexusLauncher）。证明无 always-on 返回消费者、BackdropScaffold 不吃返回。

### 根因（实证）
返回被「仅抽屉打开时启用」的 `BackHandler(enabled = drawerState.isOpen)` 消费，但其只调 `onRequestDismissDrawerSheet()`；**手势/滑动打开抽屉时 ViewModel 的 `shouldDisplayDrawerSheet` 未同步为 true（仍 false）**，于是该回调把 false 设成 false（无变化）→ `LaunchedEffect(shouldDisplayDrawerSheet)` key 未变不重触发 → `drawerState.close()` 从不执行 → 抽屉关不掉。双真源 desync。

### 修复 + 验证（含修复 APK）
- `LauncherScreen` 的 BackHandler 改为直接 `scope.launch { drawerState.close() }` + 仍调 `onRequestDismissDrawerSheet()`（VM 意图源同步，菜单再开仍可用）。
- 真机：左缘右滑开抽屉（确认打开）→ 按返回 → **抽屉收起、回到首页** ✓。
- 回归测试：`LauncherScreenBackHandlerTest`（Robolectric 交互）模拟 desync 态（drawerState Open + shouldDisplay false）→ 返回 → 断言收起；移除 fix 行该测试 FAILED、恢复后 PASS（真 red/green）。

## 2. Issue 1：统计页日期头按周期自适应 — 已修复并真机验证

在**资产统计页**（`AssetInfoContentScreen`，三页之一；分类/标签统计共用同一 `RecordDayHeader` 与纯函数 `recordDayHeaderDateText`，行为一致）用恢复的跨年数据验证：

- 周期切「**全年/按年**」（2025，用户原话场景）→ 日期头显 **「M月D日」**（如「5月22日」「5月21日」）。修复前仅「22日」、无月份上下文。✓
- 周期切「**全部**」→ 日期头显 **「YYYY年M月D日」**（如「2025年5月23日」）。✓
- 底层按日分组不变（`recordDaySeparator`），仅日期头渲染按周期带上下文、与首页 `DayHeaderItem` 单一真源一致。
- 纯函数单测 `RecordDayGroupingTest` 覆盖 5 种周期 × dayType × BY_MONTH 跨自然月（monthStartDay≠1）分支。

## 3. 备份恢复 — 已真机验证（健壮性 + 成功路径 + 新格式闭环）

### 3a. 损坏 zip 的健壮性（原测试数据 `..._20260627164133.zip`）
- 该文件已损坏：**开头恰好 1 MiB（1048576 字节）被零填充、全文件无本地文件头 `PK\x03\x04`**（中央目录/EOCD 完好故 `unzip -l` 可列、`unzip -t` 报 `bad zipfile offset`，不可恢复）。
- app 恢复：`startRecovery result = <-3013>`，logcat 记 `java.util.zip.ZipException: invalid LOC header`，**优雅失败**——无崩溃、记录数仍 0、无部分导入。新增的 `copyZipEntryBounded` 边界拷贝正确拒绝畸形 zip。✓

### 3b. legacy 备份恢复 + 跨版本迁移（替代数据 `..._20250524123538.zip`，DB_VERSION 11）
- 经 SAF 目录授权（Download/CashbookBackup）→ 长按恢复 → 从备份路径恢复 → 选该 zip。
- `startRecovery result = <2002>`（成功）；导入 **4874 条记录、27 资产、跨 2021-06 ~ 2025-05（5 年 48 个月）**；DB **v11→v14 迁移**成功（db_budget / db_image_with_related 等新表就位）。✓

### 3c. 新格式备份创建（新增需求写侧）
- app 内点「备份」→ `startBackup result = <2001>`，生成 `..._20260627154554.zip`。
- 拉取检查（`unzip -t` 无错、可恢复）：**27 entries** = `cashbook.db`（647KB，较旧 5MB 瘦身——图片已移出 BLOB）+ `manifest.json` + `settings.json` + **24 张 `record_images/img_*.jpg`**。
- `manifest.json` = `{"formatVersion":2,"appVersion":"v1.2.0_26062721"}`（格式版本 2，≤ 上限可恢复）。
- `settings.json` 含 monthStartDay 等偏好、**零凭据字段**（无 password/account/webdav/token——符合「不含账号密码 WebDAV 凭据」安全约束）。
- 完整跑通图片迁文件系统链路：v11 BLOB 图 → 恢复 → 启动 backfill 物化为文件 → 新备份纳入 record_images/。✓

### 3d. 新格式恢复 round-trip
- 恢复新格式 zip：`startRecovery result = <2002>`（manifest 解析 + settings 导入 + 图片合并 + db 拷贝全分支无异常执行）。
- 恢复后 `files/record_images/` 含 **24 张 img_*.jpg**（图片合并生效）；`cache/pre-restore/pre-restore-images/` 存在（恢复前快照安全机制）。✓

## 4. 已知限制 / 备注
- 真机 happy-path 恢复不覆盖恶意输入防护（zip-slip/解压炸弹/版本门）——由 JVM 单测（BackupEntryPolicyTest / BackupManifestTest / RecordImageFileStorageTest）覆盖。
- 截图基线由 CI 管理，本机不录不判。
- SAF DocumentsUI 目录授权步骤对合成点击不响应，由用户手动完成一次（半手动 journey 步骤）。

## 结论
Issue 1（统计页日期头按周期自适应）、Issue 2（首页抽屉返回收起）、新增备份恢复功能（创建新格式 + 跨版本迁移恢复 + 新格式 round-trip + 损坏输入健壮性）均真机验证通过。
