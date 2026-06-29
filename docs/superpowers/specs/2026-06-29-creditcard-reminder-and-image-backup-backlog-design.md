# 信用卡品牌卡 N1 提醒修复 + 图片备份 backlog 清理 设计

- 日期：2026-06-29
- 状态：设计已获用户初步批准，待节点1四维评审 + 用户 spec 评审
- 范围：bug 修复（信用卡 N1 提醒）+ 图片迁文件系统/备份恢复增强的 full-review Low backlog 清理 + 测试/验证补全
- 工作位置：git worktree 隔离（用户决策），改完 worktree 内验证 → `--ff-only` 合入本地 main
- 模块：`sync:work` / `core:data` / `core:database` / `core:datastore` / `feature:records` / `feature:settings` / build（version catalog）

---

## 背景与动机

两块来源：

1. **信用卡 N1 提醒 journey 观察**（2026-06-27 提醒收尾 journey 报告）：「信用卡 → 选具体银行」存为 `BANK_CARD_<bank>`（如招商 `BANK_CARD_ZS`），带账单日/还款日却收不到 N1 提醒。需确认是否缺陷并修复。
2. **图片迁文件系统 + 备份恢复增强**（main `4db4494a`）的 full-review Low backlog：删点删文件对称、孤儿扫描节流、live DB VACUUM、恢复端到端 androidTest、图片 entry STORED 不压、integrity_check gate、ignoreUpdateVersion 出白名单、org.json 走 catalog；外加截图基线（CI 管理）与手动黑盒往返验证。

---

## 第一部分：信用卡品牌卡 N1 提醒修复

### 根因（hands-on 核验）

资产同时持久化 `type`（大类）与 `classification`（分类）：`core/database/.../table/AssetTable.kt:61-62`、`core/model/.../model/AssetModel.kt:52-53`。

- 「信用卡 → 招商」的存储：`type = CREDIT_CARD_ACCOUNT`、`classification = BANK_CARD_ZS`。流程见 `feature/assets/.../viewmodel/EditAssetViewModel.kt:116-140`——选「信用卡」设 `typeTemp=CREDIT_CARD_ACCOUNT` 并因 `classification.isBankCard` 进入选银行 sheet；选具体银行时 `updateClassification(type=null, …)`，`type==null` 故 `typeTemp` 保留 CREDIT_CARD_ACCOUNT，最终 `type=CREDIT_CARD_ACCOUNT, classification=BANK_CARD_ZS`。
- EditAsset 用 **`type == CREDIT_CARD_ACCOUNT`** 决定显示账单/还款日字段：`EditAssetViewModel.kt:83`。→ 用户能为品牌信用卡填日期。
- 但 N1 过滤用 **`it.classification.isCreditCard`**：`sync/work/.../workers/DailyReminderWorker.kt:88-94`。`isCreditCard` = `this in CREDIT_CARD_ACCOUNT.array`（`AssetClassificationEnum.kt:83-84`），该 array 仅含 5 个原生分类（CREDIT_CARD/ANT_CREDIT_PAY/JD_IOUS/DOUYIN_MONTH/OTHER_CREDIT_CARD，`ClassificationTypeEnum.kt:43-51`），**不含任何 BANK_CARD_***。

**结论**：UI 与提醒过滤判据不一致——这是 bug，非产品歧义。

### 改动

- `DailyReminderWorker` 过滤条件 `it.classification.isCreditCard` → **`it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT`**（与 EditAsset 同一真源）。
  - 正确性：`type==CREDIT_CARD_ACCOUNT` 是 `classification.isCreditCard` 的严格超集（5 个原生信用卡分类 type 均为 CREDIT_CARD_ACCOUNT），额外纳入品牌信用卡；银行品牌**借记卡** type=CAPITAL_ACCOUNT 仍正确排除。
- 按项目「抽纯函数便于单测」惯例：抽 top-level `internal fun selectReminderCreditCards(assets: List<AssetModel>): List<CreditCardReminderInfo>`（过滤 + map），`doWork` 退化薄壳调用。

### 测试

`selectReminderCreditCards` 纯函数 JVM 单测（`sync:work`）：
- 品牌信用卡（type=CREDIT_CARD_ACCOUNT, classification=BANK_CARD_ZS）→ 纳入
- 品牌借记卡（type=CAPITAL_ACCOUNT, classification=BANK_CARD_ZS）→ 排除
- 5 个原生信用卡分类 → 纳入
- 普通资产（现金/储蓄卡大类）→ 排除

---

## 第二部分：图片备份 backlog 清理

### A. 删点删文件对称（数据一致性）

**现状**：单删 `core/data/.../repository/impl/RecordRepositoryImpl.kt:107-115`（`deleteRecord`）删前捕获图片路径、删后调 `deleteManagedImageFiles` 删文件；批量删 `deleteRecordsWithAsset:498-503`（删资产）与 `BooksRepositoryImpl.deleteBook:69-74`（删账本）只删 DB 关联（`deleteAssetRelatedData`/`deleteBookTransaction`），**不删图片文件**，靠启动孤儿扫描兜底。

**改动**：
- 新增 DAO 路径投影查询（只取 path、不读 BLOB，仿 `queryAllImagePaths:480`）：
  - `RecordDao.queryImagePathsByAssetId(booksId, assetId): List<String>`
  - `RecordDao.queryImagePathsByBookId(bookId): List<String>`
  - 同步 `FakeRecordDao`（core:data test）忠实实现。
- `RecordRepositoryImpl.deleteRecordsWithAsset`：删前捕获 asset 图片路径 → `deleteAssetRelatedData` → `deleteManagedImageFiles(paths, recordImageFileStorage)`。
- `BooksRepositoryImpl.deleteBook`：注入 `RecordDao` + `RecordImageFileStorage`，删前捕获 book 图片路径 → `deleteBookTransaction` → 复用 top-level `deleteManagedImageFiles`（core:data 内 `internal` 可见，`RecordRepositoryImpl.kt:699`）。

**测试**：路径投影 DAO 真实 SQL 落 androidTest（`RecordDaoTest`）；Repository 层删文件行为——JVM 用 Fake（`FakeRecordDao` 投影 + 临时目录 storage）断言批量删后文件被删。

### B. 孤儿扫描节流（启动性能）

**现状**：`feature/records/.../viewmodel/LauncherContentViewModel.kt:110-113` init 每次启动无条件全量 `cleanupOrphanImageFiles()`（`RecordRepositoryImpl.kt:649-660` 全目录 `listFiles()`），仅 60s grace 无节流。

**改动**（与 A 配套：A 让批量删自清文件后，孤儿扫描退为纯兜底——覆盖崩溃中途删 / backfill 中断 / 外部损坏，无需每启动跑）：
- `temp_keys.proto` 新增 `lastOrphanScanMs`（int64）；`CombineProtoDataSource` 全链镜像 + setter。
- 抽纯函数 `internal fun shouldRunOrphanScan(lastScanMs: Long, nowMs: Long, throttleMs: Long): Boolean`（`nowMs - lastScanMs >= throttleMs`，含首次 lastScanMs=0）。
- LauncherContentViewModel：`shouldRunOrphanScan` 为真才扫，扫后回写 `lastOrphanScanMs = nowMs`。节流窗口 **`ORPHAN_SCAN_THROTTLE_MS = 7 天`**。
- 时钟通过参数注入（`System.currentTimeMillis()` 在调用点取），保持纯函数可测。

**测试**：`shouldRunOrphanScan` JVM 单测（窗口内跳过 / 超窗口执行 / 首次执行）；LauncherContentViewModel gate 行为单测（FakeRecordRepository 计数 + FakeSettingRepository 注入 lastOrphanScanMs）。

### C. live DB VACUUM（空间回收）

**现状**：仅对**备份副本** VACUUM（`BackupRecoveryManagerImpl.kt:416-434`）；backfill 置空 BLOB（`backfillImagesToFiles`，gated `imagesToFilesMigrated`，触发 `LauncherContentViewModel.kt:96-101`）后 live DB 空闲页不回收。

**改动**：backfill **全部完成**（`imagesToFilesMigrated` false→true）后**一次性** VACUUM live DB 回收空闲页：
- 在 `RecordRepositoryImpl.backfillImagesToFiles` 成功置位 `imagesToFilesMigrated=true` 后，best-effort `try { db.execSQL("VACUUM") } catch { log }`（ENOSPC/锁不回收无害，下次不重试因 flag 已置）。
- VACUUM 不能在事务中执行：走 `database.openHelper.writableDatabase.execSQL("VACUUM")`（或 `@RawQuery` 薄封装），后台 IO 协程。
- 仅一次（gated 同一 flag），规避每启动 VACUUM 的锁库/2x 空间风险。

**测试**：VACUUM 是 SQLite 真实行为，落 androidTest 验「backfill 后 DB 文件大小回收 + 数据完好」；JVM 层仅验「backfill 成功后调用了 compact 路径一次」（Fake 计数）。

### (a) 图片 entry STORED 不压

**现状**：备份 zip 所有 entry `Deflater.BEST_COMPRESSION`（`BackupRecoveryManagerImpl.kt:441-442` 附近）。

**改动**：`record_images/*` 改 `ZipEntry.STORED`（JPEG 已压缩，DEFLATE 浪费 CPU 几乎不省空间）。STORED 需手填 `size` + `CRC32`；db/settings.json/manifest.json 保持 DEFLATE。抽纯辅助 `fun storedImageEntry(name, bytes): ZipEntry`（设 method/size/crc）便于单测。

**测试**：`storedImageEntry` JVM 单测（method=STORED、size/crc 正确）；恢复侧解压不区分 method 故 round-trip 不变（现有恢复测试覆盖）。

### (b) integrity_check gate

**现状**：副本 in-place VACUUM 后 `integrity_check != ok` 仅 warn，仍用（可能损坏的）VACUUMed 副本（`BackupRecoveryManagerImpl.kt:417-430`）。

**改动**：VACUUM 后 `integrity_check != ok` 时，从 checkpointed 源 `databaseFile` 重新 `copyTo(databaseCacheFile, overwrite=true)` 覆盖回未 VACUUM 的好副本（源 live DB 已 checkpoint 完好）。最小改动、不引入额外临时文件（minSdk24 无 `VACUUM INTO`）。

**测试**：纯逻辑「integrity 结果 → 是否回退」可抽小判定函数 JVM 测；真实损坏注入难造，端到端正常路径由 D androidTest 覆盖。

### (c) ignoreUpdateVersion 出白名单

**现状**：`SettingsBackupCodec.kt:30/46/72` 把 `ignoreUpdateVersion`（设备本地「跳过此更新版本」状态）纳入 settings.json 备份。

**改动**：移出白名单——export 不写、import 不读（恢复时保留本地值不覆盖）。settings.json 格式少一字段，import 改为容忍缺失（不 `getString` 该键）。更新 `SettingsBackupCodecTest`（字段数、round-trip、缺失键容忍）。

**兼容性**：旧备份含该字段 → import 忽略即可（不报错）；新备份不含 → 旧 app 若强校验该字段会失败，但旧 app 不会读未来格式（向后非向前兼容，符合既有格式版本戳策略）。需确认 codec import 当前是否严格校验全字段（实现时核对）。

### (d) org.json 走 catalog

**现状**：`core/data/build.gradle.kts:62` 硬编码 `testImplementation("org.json:json:20180813")`。

**改动**：`gradle/libs.versions.toml` 加 library 条目（版本固定 `20180813`，离线缓存有 metadata）→ `testImplementation(libs.<alias>)`。纯构建配置，零运行时影响。

**测试**：`:core:data:testDebugUnitTest` 仍绿即可（org.json 解析正常）。

---

## 第三部分：测试 / 验证

### D. 恢复端到端 androidTest（instrumented）

新建 instrumented 测试（`core/data` 或 `core/database` androidTest），覆盖完整往返：
1. 构造含图片记录的真实 DB + record_images 文件 + settings；
2. `startBackup` 打包 zip（含 db/record_images/settings.json/manifest.json，验 STORED 图片 entry）；
3. 清空环境后 `recoveryFromXxx` 解压恢复；
4. 校验：DB 行/图片文件/settings 还原正确、ZipSlip 白名单/manifest 版本戳生效。

需设备/模拟器（UTP 经代理拉，本机已验证可行）。

### E. 截图基线（CI 管理）

`recordDetailsSheet_withImage`（`feature/records/.../view/RecordDetailsSheetScreenshotTests.kt:118`）与备份页只读说明行截图用例：代码已入库、基线 PNG 缺。按 CLAUDE.md「Roborazzi 截图基线由 CI 管理，本机不录/不判」——**本地不录制**；开 PR 时 `Build.yaml` 在 `pull_request` 事件自动 `recordRoborazziDevDebug` + auto-commit 回 PR 分支。本设计不含本地截图录制动作。

### 手动黑盒往返

模拟器（Medium_Phone）跑「建带图记录 → 备份 → pm clear/卸载重装 → 恢复 → 验图片回来 + 数据完整」。android-cli + `android layout` JSON dump（截图全白）。

---

## 决策锁定（用户已批准）

1. 信用卡过滤改用 `type == CREDIT_CARD_ACCOUNT`（与 UI 一致）。
2. B 节流窗口 **7 天**；A 落地后孤儿扫描退为纯兜底。
3. C **一次性** live VACUUM（仅 backfill 完成后跑一次、best-effort），不每启动跑。
4. (c) ignoreUpdateVersion 移出备份白名单，import 容忍缺失。
5. Low 范围 = 点名 4 项（A/B/C/D）+ cheap 4 项（STORED/integrity gate/whitelist/catalog）。
6. E 截图基线 CI 管理，本地不录。
7. D androidTest + 手动黑盒均本会话在模拟器跑（用户决策「全部我来」）。

## 不在范围（明确排除）

- 删 `bytes` BLOB 列 / Room schema 变更（spec `2026-06-26-image-fs-backup-design.md` 节点1 已定「本期不删列」，删列推迟未来 Context 迁移）。
- 重写历史 migration。
- 信用卡借记卡/其它资产类型的提醒（仅修信用卡大类判据）。
- 全局 Snackbar 机制等无关重构。

## 风险与回滚

- 每项原子 commit，commit 落 git 后可独立回退。
- C VACUUM best-effort、gated 一次性 → 失败无副作用。
- (c) 改 settings.json 格式 → 实现时核对 codec import 严格校验逻辑，确保缺失键不抛异常（向后兼容旧备份）。
- A 给 BooksRepositoryImpl 注入新依赖 → 检查 Hilt 图与所有构造/测试桩。

## 提交粒度

- 信用卡修复：独立 commit（`sync:work`）。
- 图片备份：A / B / C / (a) / (b) / (c) / (d) 各自或按强相关分组原子 commit。
- D androidTest：独立 commit。
