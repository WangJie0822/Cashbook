# 测试套件全量评估 审计执行计划

> **For agentic workers:** REQUIRED SUB-SKILL: 本计划为**混合执行**——Phase 1（T1 金额核心）由 controller 亲自 inline 执行（亲审，不派 subagent）；Phase 2/3（T2/T3）用 `Workflow` 只读 fan-out 产 finding 后 controller 逐条核验；Phase 4 由 controller 综合。整体走 `superpowers:executing-plans` 的 batch + checkpoint 模式，**非** pure subagent-driven。步骤用 checkbox（`- [ ]`）跟踪。

**Goal:** 对 Cashbook 全项目测试套件做三维（功能覆盖 / 逻辑正确性含假阳性桩 / 反向边界异常）审计，产出带 `file:line` 证据的审计报告，不改代码。

**Architecture:** 方案 C 混合编排——金额核心模块 controller 亲审（最易翻车的假阳性桩判定不外包），其余深审模块与广度模块用只读 agent fan-out + controller 核验。findings 增量写入单一报告文件。

**Tech Stack:** Kotlin / Room / Hilt / Roborazzi；审计为纯静态（Read 源码对照），不实跑构建/androidTest。

**Spec:** `docs/superpowers/specs/2026-06-11-test-suite-audit-design.md`

**报告产物:** `docs/superpowers/specs/2026-06-11-test-suite-audit-report.md`（每 Phase 增量追加对应 section）

---

## 约定（所有 Task 共用）

- **Finding 格式**（写入报告）：`[严重度] 维度 | 模块 | 源 file:line ↔ 测试 file:line | 问题 | 修复建议`
- **严重度**：Critical（假阳性覆盖致真实 bug 可漏网）/ High（关键路径无覆盖或弱断言掩盖逻辑错误）/ Medium（反向/边界/异常缺失）/ Low（边角缺口）
- **三维**：①覆盖广度 ②逻辑正确性 ③反向/边界/异常
- **数据类豁免**：纯 data class / enum / sealed（无逻辑分支、无计算）不计入「无覆盖」缺口，但需在报告标注「已豁免清单」证明是有意识排除而非遗漏
- **强断言纪律**：任何「无覆盖 / 桩不忠实 / 口径错」结论必须附 `file:line`，agent 产出的此类结论 controller 必须 Read 核验后才入报告

---

## Phase 1 — T1 金额核心 Controller 亲审

### Task 1: core/model 审计

**Files to read:**
- 真有逻辑（重点审）：`core/model/src/main/.../model/RecordAmount.kt`、`core/model/src/main/.../transfer/ModelTransfer.kt`、`core/model/src/main/.../ext/Ext.kt`、`core/model/src/main/.../model/BillSummary.kt`、`core/model/src/main/.../model/ImportedBillItem.kt`、`core/model/src/main/.../model/PaymentMethodMapping.kt`、`core/model/src/main/.../model/Expandable.kt`、`core/model/src/main/.../model/Selectable.kt`
- 既有测试：`core/model/src/test/.../model/RecordAmountTest.kt`、`core/model/src/test/.../model/AnalyticsPieAmountTest.kt`

- [ ] **Step 1: 枚举 core/model 可测单元**，对 50 个源文件逐一分类「数据类豁免 / 有逻辑需测」。把豁免清单与有逻辑清单写入报告 §T1-model 开头。
- [ ] **Step 2: RecordAmount.kt 三口径逻辑正确性核查**。对照 `RecordAmountTest.kt` + `AnalyticsPieAmountTest.kt`，确认 `recordAmount` / `analyticsPieAmount` / `analyticsPieNetAmount` 三函数的 TRANSFER 分支（CLAUDE.md 记录两口径处理相反）是否都有判别性测试（即测试能区分选错口径——TRANSFER 用错口径时测试应失败）。记录 finding。
- [ ] **Step 3: ModelTransfer.kt / Ext.kt 覆盖核查**。这两个有逻辑但 core/model test 目录无对应测试文件——确认是否真无覆盖，或被其它模块测试间接覆盖（grep 调用方）。记录 ②覆盖 finding。
- [ ] **Step 4: 反向核查**。RecordAmount 三口径对边界值（0、负 charges/concessions、finalAmount 非负约束）是否有反向测试。记录 ③ finding。
- [ ] **Step 5: 把本 Task 全部 finding 追加写入** `docs/superpowers/specs/2026-06-11-test-suite-audit-report.md` 的 `## T1 core/model` section。

### Task 2: core/data 假阳性桩深审（最高优先）

**Files to read（真实 DAO ↔ Fake 桩逐一对照）:**
- `FakeTransactionDao.kt` ↔ `core/database/src/main/.../dao/TransactionDao.kt`（吸收簇 BFS / `recalculateFinalAmountForCluster` / `discoverClusterIds` / `calculateRecordAmount` / `queryByTimeAndAmount`）
- `FakeRecordDao.kt` ↔ `dao/RecordDao.kt`（`queryByWechatTransactionId` 方括号定界 LIKE、分页、按时间金额查询）
- `FakeAssetDao.kt` ↔ `dao/AssetDao.kt`、`FakeBooksDao.kt` ↔ `dao/BooksDao.kt`、`FakeTagDao.kt` ↔ `dao/TagDao.kt`、`FakeTypeDao.kt` ↔ `dao/TypeDao.kt`
- `FakeCombineProtoDataSource.kt`、`FakeRemoteDataSource.kt`

- [ ] **Step 1: 逐个 Fake DAO 方法对照真实 SQL 语义**。对每个 Fake 方法，Read 真实 DAO 的 `@Query` SQL，核查 Fake 实现是否忠实复刻（单位分/元、匹配条件、`LIKE` 方括号定界、排序、`WHERE` 过滤、JOIN 语义）。任何偏离 = Critical/High finding（附真实 SQL 行号 + Fake 实现行号）。
- [ ] **Step 2: 重点核 FakeTransactionDao 吸收簇逻辑**。`recalculateFinalAmountForCluster` / `discoverClusterIds` / 吸收者 id 升序贪心 在 Fake 中是否真实复现（对照 `TransactionDaoLogicTest.kt` 验证的是 Fake 还是真实算法）。记录 finding。
- [ ] **Step 3: 核查桩驱动的测试是否假阳性**。对 `TransactionDaoLogicTest.kt`、`SpecialTypeMigrationTest.kt` 及各 RepositoryImplTest，确认其断言走的是真实逻辑还是被宽松桩骗过（如 `emptyList()` 桩使某分支从未触达）。
- [ ] **Step 4: 写入报告** `## T1 core/data (假阳性桩)` section。

### Task 3: core/data Repository + helper 审计

**Files to read:**
- Repository impl：`AssetRepositoryImpl.kt`、`BooksRepositoryImpl.kt`、`RecordRepositoryImpl.kt`、`SettingRepositoryImpl.kt`、`TagRepositoryImpl.kt`、`TypeRepositoryImpl.kt` ↔ 对应 `*ImplTest.kt`
- helper：`WechatBillParser.kt`↔`WechatBillParserTest.kt`、`BillCategoryMatcher.kt`↔`BillCategoryMatcherTest.kt`、`BillPaymentMatcher.kt`↔`BillPaymentMatcherTest.kt`、`DailyAccountExporter.kt`↔`DailyAccountExporterTest.kt`、`AssetHelper.kt`（无测试？）
- util：`BackupRecoveryManagerImpl.kt`↔`BackupRecoveryManagerSchemeTest.kt`、`AppUpgradeManager.kt`、`ConnectivityManagerNetworkMonitor.kt`

- [ ] **Step 1: Repository impl 覆盖 + 断言强度核查**。每个 impl 的 public 方法是否有测试触达；断言是否验证关键输出（非仅「不抛异常」）。`RecordRepositoryImpl` 重点（CLAUDE.md 记录其曾不可实例化测试降级——核实当前状态）。
- [ ] **Step 2: helper 解析器反向核查**。`WechatBillParser` / `BillCategoryMatcher` / `BillPaymentMatcher` 对非法/异常输入（格式错账单、空字段、未知品类）是否有反向测试。`AssetHelper.kt` 是否真无测试。
- [ ] **Step 3: BackupRecoveryManager 核查**。备份恢复的 scheme 校验、IO 失败、版本迁移路径反向覆盖。
- [ ] **Step 4: 写入报告** `## T1 core/data (Repository+helper)` section。

### Task 4: core/domain UseCase 审计

**Files to read:** 25 个 UseCase（`domain/usecase/*.kt`）↔ 27 个测试。重点金额相关：`GetAssetMonthSummaryUseCase`、`TransRecordViewsToAnalyticsPieUseCase`、`TransRecordViewsToAnalyticsPieSecondUseCase`、`TransRecordViewsToAnalyticsBarUseCase`、`SaveRecordUseCase`、`DeleteRecordUseCase`、`RecordModelTransToViewsUseCase`。

- [ ] **Step 1: 覆盖对照**。25 UseCase 逐一确认有测试。注意测试目录多出 2 个（`AssetRecordBetweenDateRepositoryTest`、`TypeFirstSortRepositoryTest`）——确认对应哪些源、是否有 UseCase 缺测。
- [ ] **Step 2: 金额口径核查**。两个 analytics pie usecase 是否用 `analyticsPieNetAmount`（CLAUDE.md 约定），TRANSFER 分支是否守 #10b 金丝雀。测试是否有判别性用例。
- [ ] **Step 3: 反向核查**。UseCase 对空数据/空账本/无记录/边界日期的反向测试覆盖。
- [ ] **Step 4: 写入报告** `## T1 core/domain` section。

### Task 5: core/database 静态审计（androidTest 不实跑）

**Files to read:** 7 个 DAO/DatabaseTest androidTest ↔ 真实 DAO 与 12 个 Migration（`migration/Migration*.kt`）。

- [ ] **Step 1: DAO androidTest 覆盖核查**。6 个 DAO（Asset/Books/Record/Tag/Transaction/Type）的 `@Query`/`@Insert`/`@Update`/`@Delete` 方法是否被对应 `*DaoTest.kt` 覆盖。`TransactionDaoTest` 重点（吸收簇/批量删/重算）。
- [ ] **Step 2: Migration 覆盖核查**。12 个 Migration（1To2…11To12）仅 `DatabaseTest.kt` 覆盖——核实是否测了每个 migration 的 schema 迁移正确性，还是只测了最终建库。记录覆盖缺口（迁移漏测是数据丢失风险，按 High）。
- [ ] **Step 3: 标注「androidTest 未实跑，仅静态审」** 于报告 §T1-database 开头。
- [ ] **Step 4: 写入报告** `## T1 core/database` section。

- [ ] **Phase 1 Checkpoint:** 报告 T1 四 section 完成，向用户汇报 T1 findings 概况，确认继续 Phase 2。

---

## Phase 2 — T2 深审（Workflow fan-out + controller 核验）

### Task 6: T2 四模块 Workflow 只读 fan-out

**模块:** `sync/work`（10 源/1 测）、`core/common`（31/3）、`core/network`（13/3）、`core/datastore`（8/1）

- [ ] **Step 1: 编写并运行 Workflow 脚本**，4 个 agent 各审一模块。脚本约束（CLAUDE.md Workflow 规范）：
  - `meta` 纯字面量开头；agent 全程只读（不 Write/Edit/构建）
  - 用 `agent(prompt, {schema})` 强校验，schema = `{findings: [{severity, dimension, module, sourceRef, testRef, problem, suggestion}]}`
  - 每 agent prompt 给定：模块源文件清单 + 既有测试清单 + 三维判据 + 数据类豁免规则 + 「严禁无 file:line 的结论」
  - 模块审计点：sync/work（Worker 的 doWork 成功/失败/重试路径、AutoBackup 仅 1 测；ApkDownload/Sync/Init Worker 无测）；core/common（Money/Number/String 已测，Time/LunarUtils/Regex/Patterns/ext 多数无测）；core/network（WebDAVHandler/OfflineDataSource/RemoteDataSource 覆盖、序列化已测）；core/datastore（6 个 Serializer 仅 CombineProtoDataSource 有测）
- [ ] **Step 2: 收集 4 agent 的结构化 findings**（`.filter(Boolean)` 防 null）。

### Task 7: T2 findings controller 核验入报告

- [ ] **Step 1: 逐条核验**。对每条 agent finding，Read 其 `sourceRef`/`testRef` 确认结论属实（无覆盖 → 确认源方法确实无 test 触达；桩/口径问题 → Read 对照）。撤下核验不通过的，标注核验过的。
- [ ] **Step 2: 写入报告** `## T2 sync/work`、`## T2 core/common`、`## T2 core/network`、`## T2 core/datastore` 四 section，每条标「controller 已核验」。

---

## Phase 3 — T3 广度扫描（Workflow fan-out + 低成本核验）

### Task 8: T3 广度模块 Workflow 只读 fan-out

**模块:** `feature/assets`、`feature/books`、`feature/records`、`feature/record-import`、`feature/settings`、`feature/tags`、`feature/types`、`core/ui`、`core/design`、`app`、`lint`

- [ ] **Step 1: 编写并运行 Workflow 脚本**，每模块一 agent（11 agent，并发上限自动排队）。schema 同 Task 6。agent prompt 约束：**仅查覆盖缺口 + 明显反向缺失**；ViewModel 查 public 方法/UI 事件是否有 ViewModelTest 触达；截图测试**不深审渲染逻辑**，只确认存在性；`core/design`/`core/ui` 多为截图测试，只报非截图逻辑（如 CbPieChart/CbLineChart 自绘计算）的覆盖缺口。
- [ ] **Step 2: 收集 11 agent findings**。

### Task 9: T3 findings controller 低成本核验入报告

- [ ] **Step 1: 低成本核验**。覆盖缺口类 finding 核验 = 确认源文件确实无对应 test（Read/grep）。抽样核验弱断言类。
- [ ] **Step 2: 写入报告** `## T3 <module>` 各 section。

- [ ] **Phase 3 Checkpoint:** 向用户汇报 T2+T3 概况，确认进入综合报告。

---

## Phase 4 — 综合报告

### Task 10: 报告聚合与落盘

- [ ] **Step 1: 覆盖矩阵摘要**。汇总各模块「可测单元数 / 有覆盖 / 无覆盖 / 假阳性」，写报告顶部矩阵表。
- [ ] **Step 2: 全局 findings 去重 + 按严重度排序**。把分散在各 section 的 finding 汇成一张「按严重度排序」总表（Critical→Low），每条带模块与 file:line。
- [ ] **Step 3: 统计汇总**。总 finding 数、各严重度计数、各维度计数、假阳性桩计数。
- [ ] **Step 4: 写「建议优先补齐顺序」**（仅建议，不实施——呼应用户「先报告后分批补」）。
- [ ] **Step 5: 更新** `D:\Vault\.meta\pending-docs.json` 增报告条目（仅 4 个 LLM 字段）。
- [ ] **Step 6: 提交报告**：`git add docs/superpowers/specs/2026-06-11-test-suite-audit-report.md` + 提交（`[docs|test|测试套件全量评估][公共]审计报告`）。
- [ ] **Step 7: 向用户汇报** 报告路径 + Critical/High 概况，等用户决定分批补哪些。

---

## Self-Review（计划对照 spec）

- **Spec §3 三维** → Task 1-9 每个都按三维核查 ✓
- **Spec §4 分层（T1 亲审 / T2 fan-out+核验 / T3 广度）** → Phase 1 / Phase 2 / Phase 3 ✓
- **Spec §4 约束（agent 只读、禁构建、database 静态标注、强断言核验）** → Task 5 Step3、Task 6 Step1、Task 7 Step1、约定节 ✓
- **Spec §5 Finding 结构与严重度** → 约定节 ✓
- **Spec §6 四 Phase** → Phase 1-4 ✓
- **Spec 交付物（报告落盘 + pending-docs + 不改代码）** → Task 10 ✓
- 无 placeholder（文件路径均为实际清单采集所得）；方法/字段名一致（schema 字段 sourceRef/testRef 跨 Task 6/8 一致）
