# 待报销记录手动「标记已报销」设计文档

> 创建于 2026-06-24。superpowers brainstorming → 本 spec →（节点 1 四维 team-review 已通过、finding 已并入）→ writing-plans → TDD 实施。
> 关联既有功能：待报销管理界面（`2026-06-13-reimbursement-management-design.md`）、finalAmount 净自付重构（`2026-06-05-...`）。

## 1. 背景与目标

Cashbook 的「待报销列表」（`ReimbursementScreen`）列出当前账本所有「可报销支出（`RecordTable.reimbursable=1`）且未关联任何报销款(-2002)/退款款(-2001)」的记录（`GetReimbursableUnrelatedRecordViewsUseCase` + DAO `queryReimbursableUnrelated` 的 SQL `NOT EXISTS` 过滤，`RecordDao.kt:371-387`）。

现状痛点：待报销列表里存在大量多年前的支出，**现实中早已报销、但当时的报销款收入记录已无法追踪/重建**。当前模型里「已报销」**不是独立字段**，只是派生状态——仅当支出关联了一条报销款收入(-2002)、`relatedNature==REIMBURSED` 时详情弹窗才显示「已报销」（`RecordDetailsSheet.kt:193-200`）。因此这些老记录无法摆脱「待报销」标签，长期堆积。

**目标**：在记录详情弹窗新增手动「标记已报销」快捷动作（带二次确认），把这类记录移出待报销列表并显示「已报销」标签；**金额统计完全不变**（实际报销额已不可考，不动 finalAmount/任何金额口径）；支持「改回待报销」撤销。

## 2. 已锁定决策（用户选定）

| 决策点 | 结论 | 来源 |
|---|---|---|
| 「已报销」语义 | 新增**持久化布尔字段** `reimbursed`，显示「已报销」标签 + 移出待报销列表，**金额统计保持原值** | 用户选「标记已报销·金额不变」 |
| 交互形态 | **仅详情弹窗**单条按钮 + **二次确认弹窗**（不做批量多选） | 用户：「仅在详情弹窗添加吧，需要二次确认弹窗」 |
| 可逆性 | 已标记记录在详情弹窗提供「改回待报销」按钮（同样二次确认） | 用户选「要可撤销」 |
| 按钮范围 | 加在**共用**详情弹窗 `RecordDetailSheetContent` → 任意记录列表（主列表/日历/资产/搜索/待报销）打开符合资格的记录详情都可标记 | 用户选「共用弹窗处处可标记」 |
| 金额影响 | finalAmount / recordAmount / analyticsPie 口径 / 吸收簇重算 / 月度汇总**全不触碰** | 派生自语义决策 |

## 3. 详细设计

分层改动面：`core:common`(DB_VERSION) → `core:database`(Table/列名/迁移/DAO) → `core:model`(模型字段+mapper) → `core:data`(Repository) → `core:domain`(UseCase+转换透传) → `feature:records`(详情弹窗按钮+确认+VM+编辑保留+状态显示) → `core:ui`(字符串)。

### 3.1 「已报销 / 待报销」两态互斥不变量（核心约束）

`reimbursed` 标记**独立于**真实关联表 `db_record_with_related`。为杜绝「标记后又被真实关联 → stale flag 脏态/孤儿」（节点 1 reverse R1），通过两条约束保证「已手动标记」与「已真实关联」**由构造互斥**：

1. **标记按钮仅在未关联时出现**：资格 = `EXPENDITURE && reimbursable && relatedRecord.isEmpty()`（已关联记录不显示「标记已报销」按钮）。
2. **已标记记录排除出报销关联选择器**：给「编辑报销款 → 选关联支出」选择器的数据源查询加 `AND reimbursed=$SWITCH_INT_OFF`（已手动标记的记录不进选择器，无法被真实关联）。

> 数据源已核验：`SelectRelatedRecordViewModel` → `GetRelatedRecordViewsUseCase.kt:53,59` → `RecordRepository.getLastThreeMonthReimbursableRecordList(ByKeyword)`（`RecordRepositoryImpl.kt:416,453`）→ DAO `getExpenditureReimburseRecordListAfterTime`（`RecordDao.kt:356-368`）/ `getLastThreeMonthExpenditureReimburseRecordListByKeyword`（`RecordDao.kt:418-432`）。

由此：linked 记录不能被标记，marked 记录不能被 linked → 不存在「同时 `reimbursed=1` 且 `relatedNature!=NONE`」的脏态，无需改 `TransactionDao` 关联路径。

### 3.2 数据层 —— `core:common` / `core:database`

- `core/common/.../ApplicationInfo.kt:29`：`DB_VERSION` 13 → **14**。
- `ColumnNames.kt`：新增 `const val TABLE_RECORD_REIMBURSED = "reimbursed"`（紧邻 `TABLE_RECORD_REIMBURSABLE`，:57）。
- `RecordTable.kt`：新增列
  ```kotlin
  @ColumnInfo(name = TABLE_RECORD_REIMBURSED) val reimbursed: Int = SWITCH_INT_OFF,
  ```
  （加默认值不影响 Room；`reimbursable: Int` 同款，:66）。
- 新增 `Migration13To14`（纯加列，非 `_new` 重建）：
  ```sql
  ALTER TABLE db_record ADD COLUMN reimbursed INTEGER NOT NULL DEFAULT 0
  ```
  注册进 `DatabaseMigrations.MIGRATION_LIST`（`DatabaseMigrations.kt:35-49` 末尾追加）；导出 schema `14.json`；新增 `DatabaseTest.migrate13_14`（`runMigrationsAndValidate(..., 14, true, Migration13To14)`，校验列存在 + 默认值 + 无残留临时表）。
- DAO（`RecordDao.kt`）：
  1. `queryReimbursableUnrelated`（:371-387）SQL 增 `AND reimbursed = $SWITCH_INT_OFF`。
  2. `getExpenditureReimburseRecordListAfterTime`（:356-368）+ `getLastThreeMonthExpenditureReimburseRecordListByKeyword`（:418-432）各增 `AND reimbursed = $SWITCH_INT_OFF`（§3.1 不变量②）。
  3. 新增写方法（booksId 守护，节点 1 security S1）：
     ```kotlin
     @Query("UPDATE db_record SET reimbursed=:reimbursed WHERE id=:recordId AND books_id=:booksId")
     suspend fun updateRecordReimbursed(recordId: Long, booksId: Long, reimbursed: Int)
     ```
- **安全守门**：仅命名参数绑定 + `$SWITCH_INT_OFF`/`$SWITCH_INT_ON` 编译期整型常量插值（`Constants.kt:44,47`），无运行时字符串拼接（节点 1 security ① 无注入面）。

### 3.3 模型透传 —— `core:model`（加 `reimbursed: Boolean = false`，末位默认参）

`reimbursed` 贯穿 4 个 data class，**统一加为最后一个参数并赋默认值 `= false`**（节点 1 impact Critical 缓解：~25+ 处构造/预览/测试站点零改动编译通过，仅真实 mapper 显式赋值，blast radius 收窄至 ~6 核心文件）：

- `RecordModel`（`RecordModel.kt:37-50`）、`RecordViewsModel`（`RecordViewsModel.kt:38-56`）、`RecordViewsEntity`（`RecordViewsEntity.kt:40-70`）、`RecordEntity`（mapper 对称：`RecordModel.asEntity()`/`RecordEntity.asModel()` 往返不丢值，`RecordEntity.kt`）。
- 真实 mapper 显式赋值：
  - `RecordRepository.kt` `RecordTable.asModel()`（:227-242）：`reimbursed = this.reimbursed == SWITCH_INT_ON`；`RecordModel.asTable()`（:244-259）：`reimbursed = if (this.reimbursed) SWITCH_INT_ON else SWITCH_INT_OFF`。
  - `RecordModelTransToViewsUseCase` 单条（:69-87）+ 批量（:162-180）：`reimbursed = recordModel.reimbursed`。
  - `ModelTransfer.kt`：`RecordViewsModel.asEntity()`（:77-103）`reimbursed = this.reimbursed`；`RecordModel.asEntity()`/`RecordEntity.asModel()`（:26-58）互映。
- **`RecordViewsRelation` 不改**（节点 1 impact 维独立证实）：主列表 `RecordListItem` 消费完整 `RecordViewsEntity`（`SELECT *` → RecordModel → ... 链路），月度汇总 `RecordViewSummaryModel` 刻意不携带报销态。显式列投影查询（`queryViewsBetweenDate`/`query`）无需加列。

> Compose 稳定性：新增不可变 `Boolean` 保持 data class 稳定，`compose_compiler_config.conf` 无需改。

### 3.4 Repository / Domain

- `RecordRepository` 接口新增 `suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean)`。
- `RecordRepositoryImpl`：取 `currentBookId`（`combineProtoDataSource.recordSettingsData.first().currentBookId`，仿 `getReimbursableUnrelatedRecordList`）→ 调 DAO `updateRecordReimbursed(recordId, booksId, if (reimbursed) SWITCH_INT_ON else SWITCH_INT_OFF)` → **先写库后** `recordDataVersion.updateVersion()`（节点 1 security S2 顺序；触发待报销列表/主列表/详情自动刷新）。
- 新增 `UpdateRecordReimbursedUseCase`（委托 repo，`@Dispatcher(IO)`）。
- `getReimbursableUnrelatedRecordList` 链路不变（SQL 已排除 reimbursed）。

### 3.5 UI —— `feature:records`

**详情弹窗 ViewModel（节点 1 feasibility：命名 VM 类）**
- 新增 `RecordDetailsSheetViewModel`（`@HiltViewModel`，注入 `UpdateRecordReimbursedUseCase`）：`fun markReimbursed(id, reimbursed: Boolean)`，`viewModelScope.launch { useCase(id, reimbursed) }`。

**`RecordDetailSheetContent`（共用包装器，`RecordNavigation.kt:302`）**
- 加内部默认参 `viewModel: RecordDetailsSheetViewModel = hiltViewModel()`，**对外签名不变** → `MainApp.kt` 各 `recordDetailSheetContent` lambda 与 4 处调用零改动。
- 向 `RecordDetailsSheet` 传 `onMarkReimbursed = { viewModel.markReimbursed(it, true); onRequestDismissSheet() }`、`onRevertReimbursed = { viewModel.markReimbursed(it, false); onRequestDismissSheet() }`。

**`RecordDetailsSheet`（`RecordDetailsSheet.kt`）**
- 新增 2 参 `onMarkReimbursed: (Long) -> Unit`、`onRevertReimbursed: (Long) -> Unit`。
- 标题行（编辑/删除旁，:146-171）按资格条件显示其一：
  - 资格 = `typeCategory==EXPENDITURE && reimbursable && relatedRecord.isEmpty()`
  - `!reimbursed` → 「标记已报销」按钮（点击 → `dialogState = MARK_REIMBURSED_CONFIRM`）
  - `reimbursed` → 「改回待报销」按钮（点击 → `dialogState = REVERT_REIMBURSED_CONFIRM`）
- 二次确认复用现有 `dialogState` 枚举机制（`RecordDetailsDialogEnum` 已有 DELETE_CONFIRM/IMAGE_PREVIEW）：新增 `MARK_REIMBURSED_CONFIRM` / `REVERT_REIMBURSED_CONFIRM`，渲染 `CbAlertDialog`（仿 `ConfirmDeleteRecordDialog.kt:47-68`），确认 → 调对应回调。
- 同步更新唯一直接调用方：`RecordDetailsSheetWithData` 预览（:477-491）补 2 参 `{}`。

**状态显示（两处加「已报销」分支，抽窄共享 fn 防漂移，节点 1 reverse/impact R3）**
- 抽 `internal fun` 判定「EXPENDITURE 未关联记录的报销显示态」→ 返回 sealed/enum `{ MARKED_REIMBURSED, PENDING, NONE }`，供两处共用：
  - `RecordListItem`（`LauncherContentScreen.kt:709-727`）：`relatedRecord` 非空走现有 relatedNature 分支；否则按共享 fn：MARKED→`R.string.reimbursed`、PENDING→`R.string.pending_reimbursement`。
  - `RecordDetailsSheet`（:193-200）：`relatedNature` 非 NONE 走现有；NONE 时按共享 fn 同上。
- 复用现有串 `R.string.reimbursed`（「已报销」）、`R.string.pending_reimbursement`（「待报销」），不新增标签串。

**编辑保留（`EditRecordViewModel.kt:452,469-472`）**
- 编辑态持有 **`RecordModel`**（`_mutableRecordData: MutableStateFlow<RecordModel?>`，:104；保存处 `recordEntity = _displayRecordData.first()` 变量名虽叫 entity，实为 `RecordModel`，:452）。
- `recordEntity.copy(...)` 自动保留 `reimbursed`；仿 `reimbursable` 加守卫：
  ```kotlin
  reimbursed = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursed,
  ```
  （改类别离开支出时清零，避免「非支出却 reimbursed」脏态）。加载路径经 `RecordTable.asModel()`（`RecordRepository.kt:227-242` 已映射）把 `reimbursed` 带回 `RecordModel`。

### 3.6 字符串 —— `core:ui`

`core/ui/src/main/res/values/strings_records.xml` 新增：
- `mark_as_reimbursed`="标记已报销"
- `revert_to_pending_reimbursement`="改回待报销"
- `mark_reimbursed_confirm_hint`="确认将该记录标记为已报销？标记后将移出待报销列表，金额统计不变。"
- `revert_reimbursed_confirm_hint`="确认改回待报销？"
- 复用 `reimbursed`/`pending_reimbursement`/`confirm`/`cancel`。

## 4. 节点 1 四维评审 finding 与处置（全量，已 hands-on 核验）

| # | 维度/严重度 | finding | 处置 |
|---|---|---|---|
| 1 | Impact Critical→High | 4 核心 data class 加非空字段 → ~25+ 构造站点编译失败 | **采纳缓解**：末位 `=false` 默认参（§3.3），收窄至 ~6 核心文件 |
| 2 | Reverse High | stale-flag：标记后又被真实关联/解联致脏态/孤儿 | **采纳闭合**：选择器排除 reimbursed + 按钮仅未关联时显示 → 两态互斥（§3.1，数据源已核验） |
| 3 | Reverse/Impact High | 字段须完整贯穿 RecordModel 否则 @Update 重置 flag | **已在设计**：全链透传（§3.3）+ 编辑保留（§3.5） |
| 4 | Feasibility Blocking | `hiltViewModel()` 缺命名 VM 类 | **采纳**：`RecordDetailsSheetViewModel`（§3.5） |
| 5 | Security Medium | UPDATE 无 booksId 约束 | **采纳**：WHERE 加 `books_id`，Repo 取 currentBookId（§3.2/§3.4） |
| 6 | Reverse/Impact Medium | 状态逻辑两处重复易漂移 | **采纳**：抽窄共享 fn + 一致性回归测试（§3.5/§5） |
| 7 | Security Low | 写库与 bump 顺序 | **采纳**：先写后 bump（§3.4） |
| 8 | Impact High | DB 升版/14.json/迁移测试/两 Fake 同步 | **采纳**：全部纳入任务（§5） |
| 9 | Reverse Medium | 更简方案：直接关 reimbursable | **驳回**：用户已选需「已报销」标签语义，toggle 无法表达；记录为已权衡 |
| 10 | Reverse Low | 撤销仅详情弹窗可达、无 snackbar undo | **暂缓**：符合「仅详情弹窗+二次确认」最小范围；撤销可从主列表详情进入；列为可选增强（§7） |
| — | Feasibility | 误指设计含 RecordViewsRelation | **驳回**：设计本不改它（impact 维独立证实「不需改」，§3.3） |

## 5. 测试策略

| 模块 | 测试 | 任务名 | 备注 |
|---|---|---|---|
| `core:database` | `DatabaseTest.migrate13_14`：加列 + 默认 0 + 无残留临时表（`validateDroppedTables=true`） | androidTest（`connectedDebugAndroidTest`） | 本机受代理 TLS 环境影响，compile-verified + 真机补跑 |
| `core:database` | `RecordDaoTest`：`queryReimbursableUnrelated` 排除 `reimbursed=1`；选择器两查询排除 `reimbursed=1`；`updateRecordReimbursed` 置位/清除 + booksId 守护（他账本同 id 不被改） | androidTest | |
| `core:data` | `FakeRecordDao` 加 `updateRecordReimbursed` 忠实实现 + 三处查询桩补 `reimbursed==SWITCH_INT_OFF`（`FakeRecordDao.kt:314-327` 及选择器桩 :303-312）；`RecordRepositoryImplTest` 验委托 currentBookId + bump | `:core:data:testDebugUnitTest` | 忠实桩，勿空桩 |
| `core:testing` | `FakeRecordRepository` 加 `updateRecordReimbursed` 忠实实现 + `getReimbursableUnrelatedRecordList` 桩排除 reimbursed；`TestDataFactory.createRecordModel/...ViewsModel` 加 `reimbursed=false` 参 | （随依赖模块） | 否则全 core:testing 消费方编译失败 |
| `core:domain` | `UpdateRecordReimbursedUseCaseTest`；`RecordModelTransToViewsUseCaseTest` 补 `reimbursed` 透传（单条+批量逐字段等价） | `:core:domain:testDebugUnitTest` | |
| `feature:records` | `RecordDetailsSheetViewModel` 测试（mark/revert 调 useCase）；`EditRecordViewModelTest` 补 `reimbursed` 保留 + 离开支出清零；状态显示一致性回归（list 与 detail 对同一 marked 记录均显「已报销」） | `:feature:records:testDebugUnitTest` | |
| `feature:records` | `RecordDetailsSheet` 截图：标记按钮态 / 改回按钮态 / 二次确认弹窗 / 已报销标签；`ReimbursementScreenScreenshotTests` 补 marked 态不在列表 | `:feature:records`（Roborazzi） | 截图基准重录 |

## 6. 影响评估（现有功能回归）

- **模型构造全枚举**：`reimbursed: Boolean = false` 默认参 → 全部既有 `RecordModel(`/`RecordViewsModel(`/`RecordViewsEntity(`/`RecordEntity(` 构造站点（含 `RecordDetailsSheetPreviewParameterProvider`、各 `createXxx` 测试工厂、各模块 ViewModel/截图测试）**零改动编译通过**；仅 §3.3 列出的真实 mapper 显式赋值。
- **接口加方法**：`RecordRepository` + `RecordDao` 各加方法 → `RecordRepositoryImpl` + `FakeRecordRepository`(core:testing) + `FakeRecordDao`(core:data test) 必须同步实现，否则依赖 core:testing 的全部 test 模块编译失败（CLAUDE.md 已警示）。
- **`RecordDetailsSheet` 签名 +2 参** → 仅 `RecordDetailSheetContent`（供值）+ 预览函数（补 `{}`）两处；`RecordDetailSheetContent` 对外签名不变 → MainApp 与各宿主零改动。
- **DB schema 升版** → 必须导出 `14.json` + 迁移真机测试，否则 Room KSP 编译失败/老用户升级 crash。
- **备份/恢复/导出零代码改动**：`copyData`（`DatabaseMigrations.kt:98-114`）泛型逐列复制，新列自动随；旧备份(v13)经 `recoveryFromDb`（:61-83）先迁到 v14 补列默认 0；CSV 导出（`queryExportRecords`）不含报销态、无关。
- **金额/统计零触碰**：finalAmount / recordAmount / `analyticsPieNetAmount` / 吸收簇重算 / 月度汇总均不读 `reimbursed`（节点 1 impact 维证实）。
- **选择器查询加条件**：`getExpenditureReimburseRecordListAfterTime` / `...ByKeyword` 加 `AND reimbursed=0` → 仅多一过滤项，对未标记记录行为不变（marked 记录本就不应被关联）。

## 7. 非目标（YAGNI，本次不做）

- 批量多选标记（用户明确选「仅详情弹窗单条」）。
- 撤销 snackbar / 最近标记区（节点 1 reverse R10 暂缓；撤销经主列表详情进入；可选未来增强）。
- 「已报销标记」在编辑页作为独立可勾选项暴露（仅经详情弹窗动作维护）。
- 标记时联动归零金额 / 创建虚拟报销款（用户选「金额不变」）。
- 跨账本聚合 / 待报销提醒。

## 8. 实施阶段划分（产物 + 依赖，不含工时）

> 自底向上，每阶段可独立编译 + 测试通过，原子提交。

- **Phase 1 — 数据库层**：`ApplicationInfo.DB_VERSION` 13→14；`ColumnNames` + `RecordTable.reimbursed` 列；`Migration13To14` + 注册 + `14.json` 导出；DAO 三处查询加 `AND reimbursed=0` + 新增 `updateRecordReimbursed`；`DatabaseTest.migrate13_14` + `RecordDaoTest`（compile-verified，真机补跑）。产物：数据库可加列/查询/写入。
- **Phase 2 — 模型透传**：`core:model` 4 个 data class 加 `reimbursed=false` + 全 mapper（`RecordRepository.asModel/asTable`、`ModelTransfer`、`RecordModelTransToViewsUseCase` 单条+批量）；`TestDataFactory` 工厂补参；`RecordModelTransToViewsUseCaseTest` 透传断言。依赖 Phase 1。
- **Phase 3 — Repository/Domain**：`RecordRepository.updateRecordReimbursed` 接口+Impl（currentBookId + 先写后 bump）；`FakeRecordRepository` + `FakeRecordDao` 忠实实现；`UpdateRecordReimbursedUseCase` + 单测；`RecordRepositoryImplTest`。依赖 Phase 2。
- **Phase 4 — UI**：`core:ui` 字符串；`RecordDetailsSheetViewModel`；`RecordDetailsSheet` 加 2 参 + 资格按钮 + 二次确认弹窗 enum；`RecordDetailSheetContent` 接线（内部 VM 默认参）；状态显示共享 fn 两处接入 + `EditRecordViewModel` 保留守卫；预览补参；ViewModel/截图/一致性测试。依赖 Phase 3。
- **Phase 5 — 端到端校验**：全链路自检（任意列表打开待报销记录详情 → 标记已报销 → 移出待报销列表 + 主列表显「已报销」→ 改回待报销 → 回到待报销列表；编辑保留；选择器不含已标记记录）；模块测试全绿 + spotless + lint。
