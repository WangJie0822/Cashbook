# 设计：图片存储迁移到文件系统 + 备份恢复增强（分阶段·本期不删列）

- 日期：2026-06-26
- 状态：已通过节点 1 四维评审（2 Critical + 多项 High 已折入并重塑设计），待用户审阅
- 子项目：合并子项目 3（图片存储）+ 2（备份恢复），共用 worktree `worktree-asset-backup-image-improvements`
- 决策前提（用户确认）：图片均为**原图**、体积大；备份图片**保持原图不有损压缩**；设置备份**排除 WebDAV 凭据**与设备绑定项

## 背景与动机

当前图片以 **BLOB 存数据库**（`ImageWithRelatedTable.bytes`，`db_image_with_related`）。用户保存原图、体积大 → DB 与备份膨胀、整行 BLOB 读入内存。目标：图片移到应用私有文件系统、DB 精简；备份纳入图片与（设备无关）设置项；无损压实备份体积。

## 节点 1 四维评审：两个 Critical（已 hands-on 核验）决定了"分阶段不删列"架构

1. **删列先于 app 层迁移 → 存量图片永久丢失**：Room 迁移在 DB 打开时先于 app 代码执行（`DatabaseModule` `.addMigrations`），且注册的迁移全是**无 Context 的 `object`**（`DatabaseMigrations.kt:35-50`），无法写文件。若在 Room 迁移里删 `bytes`，app 层 backfill 读不到已删 BLOB。
2. **旧备份（含 BLOB）恢复丢图 + copyData 列不匹配致恢复整体失败**：`recoveryFromDb`（`DatabaseMigrations.kt:73-81`）先对**备份库**跑无 Context 迁移再 `copyData`；`copyData`（:99-115）逐表读所有列含 BLOB（`Utils.kt:66-68`）`CONFLICT_REPLACE` insert。删列后旧备份恢复必丢图且 insert 抛异常。

→ **结论：本期绝不删 `bytes` 列、不动 Room schema。** 改用双轨读 + app 层 backfill + 置空 bytes，靠 VACUUM 回收空间。删列推迟到未来版本，届时用**带 Context 的迁移**就地提取残余 BLOB。

## 总体架构（分阶段，本期 = Release N）

### A. 图片存储：双轨读 + 文件落盘（无 schema 变更）

- **存储布局**：`context.filesDir/record_images/<name>.jpg`（应用私有目录，minSdk 24 无需权限）。`name` 由图片行 id 确定性派生（幂等、可重入；新图未有 id 时用 uuid，落库后规整）。
- **path 存相对值**：`db_image_with_related.path` 改存**相对文件名/相对路径**（如 `record_images/<name>.jpg`），读取时 `File(context.filesDir, path)` 实时解析——**禁存绝对路径**（绝对路径跨设备/重装失效，节点 1 impact High）。
- **新图写入**：选图压缩后写文件 + path 存相对值 + `bytes` 置**空数组**（非 null，满足 NOT NULL）。
- **双轨读（关键，消 Critical）**：显示优先 `File(filesDir, path)` 存在则 Coil 加载 **`File` 对象**（非裸字符串，节点 1 feasibility Low）；文件不存在则回退 `bytes`（旧未 backfill 行）。读容忍缺文件→占位、不崩。
- **存量 backfill**：app 层启动迁移，`TempKeys` 加 `imagesToFilesMigrated` 标志（镜像 `db9To10DataMigrated`/`finalAmountNetRecalcDone` 全链：proto + serializer + `TempKeysModel` + `CombineProtoDataSource` 读写 + `FakeCombineProtoDataSource` + `LauncherContentViewModelTest`）。**逐行幂等**：仅处理 bytes 非空且无有效文件的行 → 写文件 → 改 path → 置空 bytes → **逐行提交**；崩溃可重入（已迁移行跳过，文件名确定性不重复）；bytes 列保留至该行提交。**bytes 仍在列中故 app 层可读**（不是 Room 迁移、无 Context 限制）。非阻塞首屏（后台静默 + 幂等重试，参照现有 gate try/catch）。
- **空间回收**：backfill 置空 bytes 后，DB 留 free page，由备份副本 VACUUM 压实回收（实时库 VACUUM 可选/推迟，避免阻塞）。
- **孤儿清理**：① 在 DB 删点提交后删对应文件——`TransactionDao` 的 `deleteOldRelatedImages`（编辑替换）、`deleteImageRelationsByRecordIds`（批量删）、`deleteRecordsBatch` 路径，删行后按 path 删文件；② 启动兜底扫描——**仅在 backfill 标志置位后**、限定 `record_images/` 子目录直接子项、**按文件名**与 DB 引用集比对、跳过非常规文件、grace window 排除新写文件，绝不按 DB 提供的绝对路径删（节点 1 security Low）。
- **原子性**：写文件（temp + rename，同卷原子）先于 DB；删 DB 行提交后再 best-effort 删文件（失败留孤儿待扫描，宁可孤儿不可丢）。
- **本期不删 `bytes` 字段**：`ImageModel`/`ImageWithRelatedTable` 保留 bytes（双轨需要），**避免节点 1 impact High 的跨 6 源集编译连锁**。`ImageViewModel` 改为承载 `path`（相对）+ 既有 bytes 回退，显示按双轨选 File 或 bytes。

### B. 备份格式升级

- **zip 结构**：`cashbook.db`（VACUUM 压实副本）+ `record_images/<files>` + `settings.json` + `manifest.json`（**格式版本戳**）。
- **VACUUM 备份副本**（节点 1 feasibility/reverse）：对**复制出的** db 文件 `openOrCreateDatabase` → `execSQL("VACUUM")`（in-place，minSdk 24 兼容；**不用** `VACUUM INTO`，需 SQLite 3.27+/约 API30）→ `PRAGMA integrity_check` → close → zip。**失败/ENOSPC 兜底**：回退 zip 未 VACUUM 的 checkpointed 副本，不让备份失败或产出半文件。
- **最高压缩**：`zos.setLevel(Deflater.BEST_COMPRESSION)`（图片为 JPEG 无损挤不动，主要压实 db；图片**保持原图不有损**）。
- **向后兼容旧 db-only 备份**：因本期**不删列**，旧备份 db 含 bytes，恢复后双轨读 + backfill 自然转文件，**完全可恢复**。
- **恢复侧重写**（节点 1 feasibility/security/impact High #5）：解压循环改为——**entry 白名单**（`cashbook.db` / `record_images/` 前缀 / `settings.json` / `manifest.json`，替代现 `comment.isNullOrBlank()` 跳过判据）；每 entry 先 `parentFile?.mkdirs()` 再写；**保留 Zip Slip `canonicalPath` 防护**（`:647`）对所有 entry 含嵌套；目录 entry `mkdirs` 后 continue。解压后：`record_images/` 拷回 `filesDir/record_images/`、`settings.json` 按白名单恢复、`cashbook.db` 走原 recoveryFromDb。
- **前向不兼容守护**（节点 1 reverse High #6）：读 `manifest.json` 格式版本，> 当前支持版本 → fail-fast 明确提示"备份版本过新"，不静默部分恢复。旧版 app 收到新备份的兜底由该版本戳 + 旧 app 的白名单缺失共同决定（旧 app 无此逻辑，属已知前向限制，不强求）。
- **恢复前快照含图片**（节点 1 reverse High #7）：`createPreRestoreBackup` 扩展为**同时**快照 `record_images/`（移到 `pre-restore-images/`），与 db 快照同为一个回滚单元；图片恢复采用**合并**语义（解压叠加、不清空现有目录），镜像 copyData 并集语义，避免清空-替换中途失败毁图。

### C. 设置项备份（白名单·设备无关·排除凭据）

- **白名单导出**（节点 1 security/impact，改黑名单为白名单）——仅设备无关偏好：`darkMode`、`dynamicColor`、`imageQuality`、`useGithub`、`autoCheckUpdate`、`ignoreUpdateVersion`、`mobileNetworkDownloadEnable`、`mobileNetworkBackupEnable`、`canary`、`logcatInRelease`、`monthStartDay`（recordSettings 纯偏好）。
- **显式排除**：全部 security（`passwordIv`/`fingerprintIv`/`passwordInfo`/`fingerprintPasswordInfo`/`verificationMode`/`needSecurityVerificationWhenLaunch`/`enableFingerprintVerification`）；**WebDAV 全部**（`webDAVDomain`/`webDAVAccount`/`webDAVPassword`，用户确认排除）；设备绑定（`backupPath`/`lastBackupMs`/`autoBackup`/`agreedProtocol`）；DB 耦合的 recordSettings ids（`currentBookId`/`defaultTypeId`/各 type id）。
- **格式**：`org.json`（零新依赖，白名单扁平字段；避免给 `core/model`/`core/data` 引 kotlinx.serialization 插件——二者当前未 wiring，节点 1 feasibility Medium）。
- **恢复**：仅覆盖白名单字段；**严格校验**（枚举 ordinal 在范围、`monthStartDay` 1-28、`imageQuality` 边界）；任一解析/校验失败则**整体跳过设置恢复**、不部分应用；**绝不恢复** WebDAV/backupPath/autoBackup（设备本地，节点 1 security High #4 防注入改备份地址外传）。
- **新增接口**：`SettingRepository` 加 `exportSettings(): String` / `importSettings(json: String)`（白名单），同步 `FakeSettingRepository` + `SettingRepositoryImplTest` + `BackupAndRecoveryViewModel`/UI 暴露"设置项纳入备份"开关。

## 实施阶段（writing-plans 细化；不计工时）

- **Phase 1 图片存储**：path 相对化 + 新图写文件 + 双轨读 + Coil File 显示；ImageViewModel/mapper 适配（保留 bytes）。
- **Phase 2 backfill + 孤儿**：TempKeys 标志全链 + app 层逐行幂等 backfill + 删点删文件 + 启动兜底扫描 + 原子性。
- **Phase 3 备份格式**：manifest 版本戳 + record_images/ 打包 + VACUUM 副本 + 最高 zip + 恢复侧重写（白名单/mkdirs/Zip Slip/合并语义/前向守护）+ 恢复前快照含图片。
- **Phase 4 设置备份**：白名单 export/import + org.json + 校验 + UI 开关。
- **Phase 5 测试与验证**：见下。

## 测试计划

- **androidTest（`core:database`/`core:data`）**：① backfill BLOB→file（含崩溃重入幂等：半迁移再跑）；② **旧 db-only 备份恢复 → 图片经 backfill materialize 为文件**（当前 `BackupRecoveryManagerSchemeTest` 对 image/zip 零覆盖）；③ 新格式 zip 往返（db+record_images+settings+manifest）；④ **Zip Slip**：含 `../` traversal entry 的恶意 zip → 断言无文件写出 cacheDir 外；⑤ 孤儿清理不误删（grace window/并发写）；⑥ 恢复前图片快照 + 合并语义。
- **JVM**：settings.json 白名单 序列化/反序列化 + 排除项断言 + 校验拒绝非法；双轨读 选 File/bytes 分支；`FakeRecordDao`/`FakeTransactionDao`/`FakeSettingRepository` 同步；`MappingTest` image 映射（保留 bytes，故改动小）。
- **回归**：现有备份/恢复测试；**至少一个 `relatedImage` 非空的记录详情截图基线**（守护 path→Coil 显示路径，当前 8 处截图均 emptyList）。
- **完整链路**：真机走"加图→保存→重启 backfill→备份→卸载重装→恢复→图片显示"端到端。

## 影响评估与非回归

- **本期无 Room schema 变更**：path/bytes 列均不变（path 仍 String、bytes 仍 ByteArray NOT NULL，仅取值语义变）→ **无 migration / 无 schema JSON / 无 version bump / 无删字段编译连锁**（节点 1 多条 Medium/High 因不删列而消解）。
- **跨任务耦合**：任务 1 默认资产按 `ORDER BY id DESC` 派生；恢复 copyData 用 `CONFLICT_REPLACE` 保留原 record id（不重排）→ 与本设计无新冲突（impact 维已确认）。备份恢复重排 id 的耦合点（架构维曾提）因 copyData 保留 id 而不成立。
- **删列推迟**：未来版本删 `bytes` 列时，必须用带 Context 的迁移就地把残余 BLOB 提取到文件后再删，并 `DatabaseTest.migrateX_Y`（`validateDroppedTables=true`）守护——属后续 spec。

## 未决/后续（不在本期）

- 删 `bytes` 列（未来版本，Context-bearing 迁移）。
- 备份 zip 整体口令加密（若日后要纳入凭据再评估）。

## 节点 1 四维评审 finding 处置表

| 维度 | 严重度 | finding | 处置 |
|---|---|---|---|
| reverse/impact/feasibility | **Critical** | 删列先于 app 迁移 → 存量图片丢失 | 本期不删列，双轨读 + app 层 backfill（A） |
| reverse/impact | **Critical** | 旧备份含 BLOB 恢复丢图 + copyData 列不匹配 | 不删列 → 旧备份 db 含 bytes，恢复后 backfill 转文件（B 向后兼容） |
| security | High | WebDAV 凭据明文进可上传备份 | 用户确认排除 WebDAV 全部凭据（C） |
| security | High | 恢复不可信 settings.json 改备份地址外传 | 恢复不覆盖 WebDAV/backupPath/autoBackup + 严格校验（C） |
| feasibility/security/impact | High | 恢复 zip 解析不兼容多 entry | 恢复侧重写：白名单+mkdirs+Zip Slip+目录处理（B） |
| reverse/impact | High | 新备份对旧 app 不兼容无守护 | manifest 格式版本戳 + fail-fast（B） |
| reverse | High | 恢复前快照不含图片 | 快照含 record_images + 合并语义（B） |
| reverse | High | backfill 崩溃不幂等 | 逐行幂等 + 确定性文件名 + 保留 bytes 至提交（A） |
| impact | High | 删 bytes 字段跨 6 源集编译连锁 | 本期不删字段（保留 bytes 双轨）（A） |
| impact | High | path 绝对路径破坏跨设备恢复 | path 存相对值、按 filesDir 解析（A） |
| feasibility | Medium | settings.json 无 serialization wiring | org.json 零依赖（C） |
| feasibility | Low | Coil 应传 File 非裸字符串 | 显示传 `File(filesDir,path)`（A） |
| reverse | Medium | VACUUM 损坏面 | in-place VACUUM 副本 + integrity_check + ENOSPC 回退（B） |
| reverse/impact | Medium | 孤儿扫描竞态误删 | 删点删文件 + 扫描限定/grace window/标志后（A） |
| reverse | Medium | 两阶段原子性 | 写文件先于 DB、删 DB 先于文件、读容忍缺文件（A） |
| reverse/impact | Medium | 设置恢复反噬设备配置/currentBookId | 白名单排除设备项 + 不碰 recordSettings ids（C） |
| impact | Medium | DB version/migrate 配套 | 本期不删列故不需要；删列推迟（未决） |
| impact | Medium | equals/hashCode | 本期不删 bytes 字段，不动（A） |
| impact | Medium | TempKeys 连锁 | 镜像现有 flag 全链（A） |
| impact | Low | 备份/恢复/图片显示回归测试零覆盖 | 补 androidTest + 截图基线（测试计划） |
| security | Low | 孤儿扫描路径限定 | 限定 record_images/ 子目录、文件名匹配（A） |
