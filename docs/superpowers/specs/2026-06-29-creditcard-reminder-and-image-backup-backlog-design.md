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

**隐藏信用卡（节点1 reverse Low，pre-existing 保持）**：N1 取 `currentVisibleAssetListData`，`invisible=true` 的信用卡不提醒——隐藏=刻意弱化，本次保持现状，`selectReminderCreditCards` 加注释说明该取数边界。

### 测试

`selectReminderCreditCards` 纯函数 JVM 单测（`sync:work`）：
- 品牌信用卡（type=CREDIT_CARD_ACCOUNT, classification=BANK_CARD_ZS）→ 纳入
- 品牌借记卡（type=CAPITAL_ACCOUNT, classification=BANK_CARD_ZS）→ 排除
- 5 个原生信用卡分类 → 纳入
- 普通资产（现金/储蓄卡大类）→ 排除
- type=CREDIT_CARD_ACCOUNT 但账单/还款日为空 → 纳入列表但 `computeReminders` 经 `parseCardDay` 安全跳过（不产空提醒，已核验 `ReminderLogic.kt:30`）

---

## 第二部分：图片备份 backlog 清理

### A. 删点删文件对称（数据一致性）

**现状**：单删 `core/data/.../repository/impl/RecordRepositoryImpl.kt:107-115`（`deleteRecord`）删前捕获图片路径、删后调 `deleteManagedImageFiles` 删文件；批量删 `deleteRecordsWithAsset:498-503`（删资产）与 `BooksRepositoryImpl.deleteBook:69-74`（删账本）只删 DB 关联（`deleteAssetRelatedData`/`deleteBookTransaction`），**不删图片文件**，靠启动孤儿扫描兜底。**编辑改图路径同样漏删**：`updateRecord:86-105` 只 `persistNewImages`（写新图）+ `updateRecordTransaction`（内部 `deleteImageRelationsByRecordIds` 删旧关联后重插），无 `deleteManagedImageFiles`——用户编辑时移除/替换的图片立即成孤儿（`LauncherContentViewModel.kt:110` 注释已承认）。

**改动**（节点1 reverse R1：必须覆盖编辑路径，否则 B 节流前提不成立）：
- 新增 DAO 路径投影查询（只取 path、不读 BLOB，仿 `queryAllImagePaths:480`）：
  - `RecordDao.queryImagePathsByAssetId(assetId): List<String>`——**逐字镜像** `deleteWithAsset` 的删除谓词（`RecordDao.kt:442-448`，含 `asset_id=:assetId OR into_asset_id=:assetId`），JOIN `db_record`→`db_image_with_related`（后者无 asset/book 列）。
  - `RecordDao.queryImagePathsByBookId(bookId): List<String>`——镜像 `queryRecordListByBookId`/`deleteBookTransaction` 的记录选择谓词。
  - 同步 `FakeRecordDao`（core:data test）**忠实复刻** join 语义（先按谓词筛 records 得 id 集，再 `images.filter{recordId in ids}.map{path}`），禁 `emptyList()`/宽松桩。
- `RecordRepositoryImpl.deleteRecordsWithAsset`：删前捕获 asset 图片路径 → `deleteAssetRelatedData` → `deleteManagedImageFiles(paths, recordImageFileStorage)`。
- `BooksRepositoryImpl.deleteBook`：注入 `RecordDao` + `RecordImageFileStorage`，删前捕获 book 图片路径 → `deleteBookTransaction` → 复用 top-level `deleteManagedImageFiles`（core:data 内 `internal` 可见，`RecordRepositoryImpl.kt:699`）。
- **编辑路径对称（R1）**：`RecordRepositoryImpl.updateRecord` 删前 `queryImagesByRecordId(record.id)` 捕获旧托管图路径 → `persistNewImages` + `updateRecordTransaction` → diff（旧托管路径 − 持久化后保留路径）→ `deleteManagedImageFiles(removed, …)`。保留图路径不变（`persistNewImages` 对已托管图原样透传），故 diff 精确得"被移除图"。

**图片 1:1 不共享（核验）**：新图 per-image UUID 路径（`newRelativePath`）、backfill 图 per-row-id 路径（`relativePathForId`），无路径被两行引用 → 删某记录/资产/账本的图不会误删他记录的图。

**测试**：路径投影 DAO 真实 SQL 落 androidTest（`RecordDaoTest`），**加负向断言**：他 asset/book 的图片不被返回（防过删）。Repository 层删文件行为——JVM 用 Fake（`FakeRecordDao` 忠实投影 + 临时目录 storage）断言批量删/编辑移除后对应文件被删、保留图文件不删。

### B. 孤儿扫描节流（启动性能）

**现状**：`feature/records/.../viewmodel/LauncherContentViewModel.kt:110-113` init 每次启动无条件全量 `cleanupOrphanImageFiles()`（`RecordRepositoryImpl.kt:649-660` 全目录 `listFiles()`），仅 60s grace 无节流。

**改动**（A 已覆盖删资产/账本/编辑路径后，孤儿扫描退为纯兜底——覆盖崩溃中途删 / backfill 中断 / 恢复合并 / 外部损坏，无需每启动跑）：
- `temp_keys.proto` 新增 `lastOrphanScanMs`（int64，默认 0）；`TempKeysModel` 加 `lastOrphanScanMs: Long = 0L`；`CombineProtoDataSource` 全链镜像 + `updateLastOrphanScanMs` setter（含测试桩 `FakeCombineProtoDataSource`）。
- 抽纯函数 `internal fun shouldRunOrphanScan(lastScanMs: Long, nowMs: Long, throttleMs: Long): Boolean`（`nowMs - lastScanMs >= throttleMs`，含首次 lastScanMs=0）。
- **节流逻辑下沉到 `RecordRepositoryImpl.cleanupOrphanImageFiles()`**（节点1 impact I1 规避 SettingRepository 接口 fan-out）：repo 直接持 `combineProtoDataSource`，读 `tempKeysData.first().lastOrphanScanMs` → `shouldRunOrphanScan(last, now, THROTTLE)` 为假则直接 return（跳过 `listFiles()`）；为真则扫描 + `updateLastOrphanScanMs(now)`。`LauncherContentViewModel` **不变**（仍无条件调用 `cleanupOrphanImageFiles()`，由 repo 内部决定是否真扫）→ `SettingRepository` 接口不动、现有 VM 测试 `launcher_always_runs_orphan_image_cleanup` 不改。节流窗口 **`ORPHAN_SCAN_THROTTLE_MS = 7 天`**。
- 时钟在 repo 调用点取 `System.currentTimeMillis()`，逻辑正确性由纯函数承载。
- **恢复复位（节点1 reverse R4）**：备份打包含目录全部图片（可能含孤儿），恢复合并会重新引入孤儿且复位 `imagesToFilesMigrated`。恢复成功路径同步 `updateLastOrphanScanMs(0)`，使下次启动强制扫一次（否则恢复引入的孤儿被节流压制最长 7 天）。

**测试**：`shouldRunOrphanScan` JVM 单测（窗口内跳过 / 超窗口执行 / 首次 lastScanMs=0 执行）；repo 内节流接线由 androidTest/集成覆盖（RecordRepositoryImpl 不 JVM 实例化，纯函数已守正确性）。

### C. live DB VACUUM（空间回收，C-robust 健壮版）

**现状**：仅对**备份副本** VACUUM（`BackupRecoveryManagerImpl.kt:416-434`）；backfill 置空 BLOB（`backfillImagesToFiles`，gated `imagesToFilesMigrated`，触发 `LauncherContentViewModel.kt:96-101`）后 live DB 空闲页不回收。

**改动**（节点1 reverse R3/feasibility：原"一次性 best-effort"对大库/低空间用户最易 ENOSPC/SQLITE_BUSY 静默失败且因 flag 已置永不重试，且无 JVM 测试接缝 → 用户选 C-robust 健壮版）：
- **可注入接缝**：新增 `interface DatabaseCompactor { suspend fun vacuum(): Boolean }`（impl 持 `CashbookDatabase`，`database.openHelper.writableDatabase.execSQL("VACUUM")` 真成功返 true、异常返 false；VACUUM 不能在事务中故走 SupportSQLiteDatabase），注入 `RecordRepositoryImpl`。JVM 用 `FakeDatabaseCompactor` 计数/控返回值。
- **独立成功门 flag**：`temp_keys.proto` 新增 `dbVacuumDone`（bool，默认 false），**仅 VACUUM 真成功才置位**（不与 `imagesToFilesMigrated` 耦合）。
- **空间预检**：VACUUM 前 `StatFs` 查可用空间 ≥ 当前 DB 文件大小（VACUUM 需 ~1x 临时空间）才尝试，否则跳过留待下次。
- **失败重试**：在 `LauncherContentViewModel` 首屏 gate（backfill 已完成 `imagesToFilesMigrated=true` 且 `!dbVacuumDone`）后台 best-effort 调 `recordRepository.compactDatabaseIfNeeded()`；StatFs 不足/VACUUM 失败 → 不置 `dbVacuumDone`，下次启动重试；成功 → 置位、永不再跑。
- 后台 IO 协程，不阻塞首屏（首屏仅 gate `_migrationCompleted`）。

**测试**：`compactDatabaseIfNeeded` 逻辑（已完成不重跑 / 空间不足跳过不置位 / 成功置位）JVM 单测（FakeDatabaseCompactor + 控 StatFs 经参数或可注入 provider）；真实 VACUUM 空间回收落 androidTest（backfill 后 DB 文件变小 + 数据完好）。

### (a) 图片 entry STORED 不压

**现状**：备份 zip 所有 entry `Deflater.BEST_COMPRESSION`（`BackupRecoveryManagerImpl.kt:441-442` 附近）。

**改动**：`record_images/*` 改 `ZipEntry.STORED`（JPEG 已压缩，DEFLATE 浪费 CPU 几乎不省空间）。STORED 需手填 `size` + `CRC32`；db/settings.json/manifest.json 保持 DEFLATE。
- **保持 O(1) 内存**（节点1 feasibility：现 `putZipFileEntry` 流式 copyTo，避免大图整文件入堆）：`storedImageEntry` 改**两遍流式**——第一遍 `FileInputStream` 流读算 CRC32 + size，第二遍流写 entry；不以 `ByteArray` 整图入参。

**测试**：CRC32/size 计算纯函数（喂已知字节）JVM 单测（method=STORED、crc/size 正确）；恢复侧 `getInputStream` 透明处理 STORED/DEFLATE 故 round-trip 不变（现有恢复测试 + D androidTest 覆盖）。

### (b) integrity_check gate

**现状**：副本 in-place VACUUM 后 `integrity_check != ok` 仅 warn，仍用（可能损坏的）VACUUMed 副本（`BackupRecoveryManagerImpl.kt:417-430`）。

**改动**：副本 in-place VACUUM 后 `integrity_check != ok` 时，回退为未 VACUUM 的好副本。节点1 reverse/feasibility 修正两点：
- 回退 re-copy 必须在 **VACUUM 句柄 `.use{}` 关闭之后**执行（捕获 integrity 结果为局部 val → 出块 → `if (!ok) ...copyTo(overwrite=true)`），否则覆盖正打开的 DB 文件不可靠。
- 回退前**重新 `wal_checkpoint(TRUNCATE)`** 再从 `databaseFile` 复制——因回退发生在 :405 首次 checkpoint 之后较晚时点，期间 WorkManager/提醒 worker 的写入可能落 -wal、不在 `databaseFile`，不重 checkpoint 会丢最近记录。

**测试**：纯逻辑「integrity 结果 → 是否回退」抽小判定函数 JVM 测；真实损坏注入难造，端到端正常路径由 D androidTest 覆盖。

### (c) ignoreUpdateVersion 出白名单

**现状**：`SettingsBackupCodec.kt:30/46/72` 把 `ignoreUpdateVersion`（设备本地「跳过此更新版本」状态）纳入 settings.json 备份，恢复时 `SettingRepositoryImpl.importSettings:297` `updateIgnoreUpdateVersion(backup.ignoreUpdateVersion)` 覆盖本地值。

**改动**（节点1 reverse R2/impact 修正：原"全删字段"方案有兼容陷阱——`decodeSettingsBackup:61-85` 严格 `getString` 且**无版本门**，全删后旧 app 读新备份会抛 JSONException → 静默丢弃**全部**设置）：
- 采**最小 compat-safe** 方案：字段保留在 `SettingsBackup`/encode/decode（JSON 形状不变，前后向全兼容），**仅 `importSettings` 不再 apply `ignoreUpdateVersion`**（恢复时保留设备本地值）→ 达成"设备本地跳过版本不随恢复转移"的真实意图，零兼容风险。
- 同步：`SettingRepositoryImpl.importSettings`（移除该 apply 行）+ `FakeSettingRepository.importSettings`（同步）。

**测试**：`SettingRepositoryImplTest`/`FakeSettingRepository` 行为测试断言 import 后本地 `ignoreUpdateVersion` **未被备份值覆盖**；codec round-trip 不变（字段仍在）。

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

## 决策锁定（用户已批准 + 节点1 后更新）

1. 信用卡过滤改用 `type == CREDIT_CARD_ACCOUNT`（与 UI 一致）。
2. B 节流窗口 **7 天**；**A 扩到编辑路径**后孤儿扫描才退为纯兜底（节点1 R1）；节流逻辑下沉 repo、不动 SettingRepository（节点1 I1）；恢复复位 lastOrphanScanMs（节点1 R4）。
3. C 走 **C-robust 健壮版**（DatabaseCompactor 接缝 + 独立 dbVacuumDone 成功门 + StatFs 预检 + 失败下次重试），非一次性 best-effort（节点1 R3）。
4. (c) **最小 compat-safe**：字段保留于 codec、仅 `importSettings` 不再 apply（节点1 R2，规避旧 app 恢复丢全部设置）。
5. Low 范围 = 点名 4 项（A/B/C/D）+ cheap 4 项（STORED/integrity gate/whitelist/catalog）。
6. E 截图基线 CI 管理，本地不录。
7. D androidTest + 手动黑盒均本会话在模拟器跑（用户决策「全部我来」）。

## 节点1 四维评审处置（已折入上文）

feasibility 0 阻塞 / security 0 阻塞（1 pre-existing Low：备份 zip 明文未加密，非本批引入、不处理）/ reverse 1 High+4 Med / impact 1 High+3 Med，全部 controller hands-on 核验：

| finding | 维度/严重度 | 处置 |
|---|---|---|
| 编辑改图路径漏删文件，B 前提失效 | reverse High | 采纳：A 扩到 `updateRecord`（diff 删被移除托管图） |
| B 回写需 SettingRepository 新抽象方法→级联 | impact High | 规避：节流下沉 `cleanupOrphanImageFiles()`，VM/接口/旧测试不动 |
| (c) 全删字段破坏旧 app 恢复（无版本门） | reverse/impact Med | 改方案：字段保留、仅 import 不 apply |
| C 一次性 VACUUM 对目标用户静默失效不重试 + 无测试接缝 | reverse/feasibility Med | C-robust（成功门 flag+StatFs+重试+可注入接缝） |
| 恢复重新引入孤儿、节流不复位 | reverse Med | 采纳：恢复成功 `lastOrphanScanMs=0` |
| `launcher_always_runs_orphan_image_cleanup` 与节流冲突 | impact Med | I1 下沉后 VM 无条件调用，测试不变 |
| A 投影查询须镜像 `deleteWithAsset`（含 into_asset_id） | feasibility/impact/security Low | 采纳：DAO 逐字镜像+FakeRecordDao 忠实+androidTest 负向断言 |
| (b) 回退须关句柄+重 checkpoint | reverse/feasibility Low | 采纳 |
| (a) STORED 破坏 O(1) 流式 | feasibility Low | 采纳：两遍流式 |
| 隐藏信用卡不提醒（pre-existing） | reverse Low | 保持，加注释 |

## 不在范围（明确排除）

- 删 `bytes` BLOB 列 / Room schema 变更（spec `2026-06-26-image-fs-backup-design.md` 节点1 已定「本期不删列」，删列推迟未来 Context 迁移）。
- 重写历史 migration。
- 信用卡借记卡/其它资产类型的提醒（仅修信用卡大类判据）。
- 全局 Snackbar 机制等无关重构。

## 风险与回滚

- 每项原子 commit，commit 落 git 后可独立回退。
- C VACUUM best-effort、StatFs 预检、失败不置 dbVacuumDone 下次重试 → 失败无副作用、目标用户最终可回收。
- (c) 不改 settings.json 格式（字段保留），仅 import 不 apply → 前后向全兼容、无格式风险。
- **fan-out 检查清单（impact 重点，漏改即编译级联）**：
  - DAO 新增 `queryImagePathsByAssetId`/`queryImagePathsByBookId` 抽象方法 → 必须同步唯一 `FakeRecordDao`（core:data test，忠实 join 复刻），否则 `:core:data:compileDebugUnitTestKotlin` 失败。
  - proto 新增 `lastOrphanScanMs`/`dbVacuumDone` → 贯穿 `TempKeysModel`（默认 `0L`/`false`）+ `CombineProtoDataSource`（映射+setter）+ `FakeCombineProtoDataSource`；现有具名构造点带默认值不破坏。
  - `BooksRepositoryImpl` 注入 `RecordDao`+`RecordImageFileStorage`、`RecordRepositoryImpl` 注入 `DatabaseCompactor` → Hilt 图已具备（`DatabaseModule`/`DataModule`），且无测试直接实例化二 Impl，构造变更编译安全；新增 `DatabaseCompactor` 需在 `DataModule` 加 `@Binds`/`@Provides`。
  - (c) 删 `importSettings` apply 行 → 同步 `SettingRepositoryImpl` + `FakeSettingRepository` 两处 import。

## 提交粒度

- 信用卡修复：独立 commit（`sync:work`）。
- 图片备份：A / B / C / (a) / (b) / (c) / (d) 各自或按强相关分组原子 commit。
- D androidTest：独立 commit。
