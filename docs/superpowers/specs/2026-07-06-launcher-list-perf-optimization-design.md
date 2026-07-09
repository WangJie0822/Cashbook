# 首页记录列表加载性能优化设计（R6a：Room `@Relation` 分页）

- 日期：2026-07-06
- 状态：设计（待评审）
- 分支/worktree：`worktree-perf-poc`（`D:/wt/Cashbook/perf-poc`）

## 1. 背景与问题现象

用户反馈首页记录列表「加载速度较慢，有明显卡顿」。经确认，现象集中在三处，均为「数据准备/刷新延迟」而非「滚动掉帧」：

- 进首页时列表要等一会儿才出现；
- 切换月份/日期后新数据刷新慢；
- 新建记录后返回首页列表要等一会儿。

且用户确认「看**当前月**（默认）就明显慢」——即单月数据量下即已明显，指向逐条转换开销而非大范围全表扫描。

## 2. 实证根因（附代码出处）

### 根因 A（主因）——列表 N+1 查询

首页列表数据流为 `Pager(pageSize=20)` → Room `PagingSource` → 逐元素转换：

- `LauncherContentViewModel.kt:187-202` 的 `recordPagingData` 对 `PagingData` 逐元素 `map { recordModelTransToViewsUseCase(recordModel) }`；
- `PagingData.map` 是**逐元素**调用，走 `RecordModelTransToViewsUseCase` 的**单条版**（`RecordModelTransToViewsUseCase.kt:52-89`），每条记录独立发起 **≥6 次数据库查询**（type / asset / relatedAsset / 关联记录 / tags / images）；
- 一页 20 条 ≥120 次查询，切月/新建后重刷时再次累积。

项目已有批量版 `transBatch`（`RecordModelTransToViewsUseCase.kt:98-184`，IN 批量查询消 N+1），但 `PagingData.map` 逐元素调用**用不上它**。

### 根因 B（次要，当前月非主因）——汇总无 LIMIT + Main 线程聚合

- `RecordDao.queryViewsBetweenDate`（`RecordDao.kt:92-113`）无 LIMIT，返回整段记录到内存；
- `computeDailySummaries`（`LauncherContentViewModel.kt:278-318`）与 `uiState` 聚合循环（`:213-238`）在 **Main 线程**（无 `flowOn`）；
- 「全部」周期 = 全表（`DateSelectionEntity.kt:83-85` 返回 `0L..Long.MAX_VALUE`）。
- 单月数据量下该项非主要瓶颈，故本设计将其列为**次要/可选**改进。

### 根因 C（次要）——LazyColumn 无 `contentType`

`LauncherContentScreen.kt:520-566` 的 `items(...)` 有 `key` 无 `contentType`，DayHeader/Record 异质项复用打折。零成本改进，纳入。

## 3. 方案选型

### 候选与对比

| 方案 | 消 N+1 | 回顶（gate） | invalidation（gate） | 代价 |
|---|---|---|---|---|
| 方案 1：自定义 PagingSource + `transBatch` | ✅ | ❌ 需手写 offset `getRefreshKey` 保位（脆、无先例、需真机 PoC） | ❌ 手写 `addObserver`（易漏表/泄漏） | 复用现成 transBatch |
| 方案 R6b：纯 JOIN 投影分页 | 部分 | ✅ Room 免费 | ✅ Room 免费 | 一对多（tags/images）JOIN 行乘积、无法单行展开，只消 type/asset |
| **方案 R6a：`@Transaction`+`@Relation` 分页** ✅ | **✅ 全 IN 批量** | **✅ Room 免费保位** | **✅ Room 自动列全表** | 需写 POJO→RecordViewsModel mapper |

### 选定：R6a，附编译级 PoC 硬证据

在 `worktree-perf-poc` 写了一个 `@Transaction @Query` 返回 `PagingSource<Int, LauncherRecordViewPocRelation>` 的 `@Relation` POJO（type/asset/intoAsset/tags/images + relatedRecord 双向），KSP 编译 `:core:database:compileDebugKotlin` **BUILD SUCCESSFUL**。查 Room 生成的 `RecordDao_Impl.kt` 证实：

- PagingSource 实现 = **`LimitOffsetPagingSource`**（`:175`）——即首页现状同款、已验证不回顶的官方保位实现，**零手写 `getRefreshKey`**；
- Room **自动列全 7 张关联表** `db_type / db_asset / db_image_with_related / db_tag_with_record / db_tag / db_record_with_related / db_record`（`:176-177`）触发 invalidate——**不漏表、无 observer 泄漏、零手写**；
- **双向自关联生成两个独立正确查询**：`JOIN db_record ON related_record_id=id WHERE record_id IN(...)`（`:3591`）与 `JOIN db_record ON record_id=id WHERE related_record_id IN(...)`（`:3688`），不混淆；
- 所有关联（type/asset/tags/images/related）均 `IN (...)` **批量查询**——每页固定几次批量，N+1 消除比 `transBatch` 更彻底（后者 type/asset 仍逐 distinct id 查）；
- 平账合成类型（typeId=`-1101`/`-1102`，`RecordTypeModel.kt:51/69`）：`@Relation` 天然 LEFT（主查询 `SELECT * FROM db_record` 不 JOIN db_type），无匹配 → type 关联为空 List、**主记录不丢**。

结论：R6a 把方案 1 最难的两个 gate（手写保位 + 手写 invalidation）由 Room 免费且正确地解决，故方案 1 的真机回顶 PoC 无必要。

## 4. 详细设计

### 4.1 架构分层与数据流

```
core:database  → LauncherRecordViewRelation（@Relation POJO） + RecordDao.@Transaction @Query（Room 生成 LimitOffsetPagingSource）
core:data      → sumRelatedAmount/computeRelatedNature 纯函数（勘误：原设想 core:model，因其零依赖 jvm library 无法访问 core:common 的 FIXED_TYPE_ID_*，改放 core:data；见 plan Task 1），供 mapper 与 transBatch 共用
core:data      → RecordRepositoryImpl.getRecordPagingData 内部 map POJO→RecordViewsModel，暴露 Flow<PagingData<RecordViewsModel>>
feature:records→ LauncherContentViewModel 去掉逐元素 transToViews，改 map { asEntity } + insertSeparators
```

依据（已核验）：`core:data` 依赖 `core:database`+`core:model`（`core/data/build.gradle.kts:47-48`），可同时访问 POJO 与 RecordViewsModel，mapper 放此处无循环依赖；`core:domain` 不依赖 `core:database`（`core/domain/build.gradle.kts:26-34`），故 mapper **不能**放 core:domain。

### 4.2 组件清单

| 模块 | 改动 | 说明 |
|---|---|---|
| core:database | **新增** `relation/LauncherRecordViewRelation.kt` | `@Embedded record` + `@Relation`：type/asset/intoAsset（List 取 firstOrNull）、images、tags（`@Junction db_tag_with_record`）、relatedAsRecordId / relatedAsRelatedId（双向自关联）。以 PoC 版为蓝本正式命名 |
| core:database | **新增** `RecordDao` 方法 | `@Transaction @Query(... books_id + record_time 范围 + ORDER BY record_time DESC): PagingSource<Int, LauncherRecordViewRelation>`；WHERE 必须含 `books_id=:booksId`（账本隔离，security #3） |
| core:model | **提取** `sumRelatedAmount`/`computeRelatedNature` + 平账特判为纯函数 | 从 `RecordModelTransToViewsUseCase`（core:domain）提到 core:model，作为 RecordViewsModel 组装的单一来源，供 core:data mapper 与 core:domain transBatch 共用，防口径漂移 |
| core:data | **改造** `RecordRepositoryImpl.getRecordPagingData` | 改用新 DAO 方法；`Pager.flow.map { pagingData.map { pojo → RecordViewsModel } }`；mapper 内 relatedRecord 按 typeCategory 选向（INCOME 用 relatedAsRecordId，其余用 relatedAsRelatedId）、平账 type 特判、relatedAmount/relatedNature 内存计算（零查询） |
| core:data | **改** `RecordRepository` 接口返回类型 | `getRecordPagingData(...): Flow<PagingData<RecordViewsModel>>`（原 `PagingData<RecordModel>`）；同步 `FakeRecordRepository`（core:testing）、`FakeRecordDao`（core:data test，新增 DAO 方法须忠实复刻 LIMIT/OFFSET+DESC 语义，禁 emptyList 桩） |
| feature:records | **改** `LauncherContentViewModel.recordPagingData` | 去掉 `recordModelTransToViewsUseCase(recordModel)` 逐条转换，改 `pagingData.map { it.asEntity() }`；`insertSeparators` **保留在 VM 层**（reverse R3：移进 load() 会跨页重复 DayHeader → LazyColumn 重复 key 崩溃） |
| feature:records | **改** `LauncherContentScreen.kt:521` | `items(...)` 补 `contentType`（DayHeader/Record 两类；placeholder null 返回稳定值）——根因 C |

### 4.3 关键设计决策

- **relatedRecord 双向选向**：POJO 同时物化两个方向，mapper 按 `type.typeCategory` 选取（复刻单条 `invoke` 的 `if (INCOME) getRelatedIdListById else getRecordIdListFromRelatedId`）。
- **relatedAmount/relatedNature**：在 mapper 内对已物化的关联做**纯内存计算**（`sumRelatedAmount`/`computeRelatedNature`），无 DB 查询。
- **平账特判**：type 关联空 List 时，按 typeId 映射 `RECORD_TYPE_BALANCE_INCOME/EXPENDITURE`（复刻现有特判）。
- **一对一取 firstOrNull**：type/asset/intoAsset 的 `@Relation` 返回 List（0/1），取 `firstOrNull`；asset 语义与现有 `getAssetById(-1)→null` 一致。
- **等价性保证**：mapper 产出的 RecordViewsModel 必须与现有单条/`transBatch` 逐字段等价（金额口径 finalAmount/recordAmount、报销对冲 relatedAmount/relatedNature、tags/images），由 4.4 的等价性测试守护。

## 5. 四维评审要点如何处理

| 评审 finding | 处理 |
|---|---|
| 🔴 回顶（feasibility 1b / impact F1 / reverse R1） | R6a 用 Room 生成 `LimitOffsetPagingSource` 保位，天然规避；PoC 已证实 |
| 🔴 invalidation 表覆盖（feasibility 4 / impact F1 / reverse R2） | Room 自动列全 7 表，PoC 已证实；无手写漏表/泄漏 |
| 🟠 separators 移进 load() 崩溃（reverse R3） | **保留 VM 层**，不移进 load() |
| 🟠 load() 异常触发 finishAllActivity（reverse R8） | R6a 用 Room 生成 `LimitOffsetPagingSource`，load 异常处理为其框架行为（非手写）；**实现时须确认**查询异常不逃逸到全局 handler（避免 `finishAllActivity`），必要时在 mapper/上游 catch |
| 🟠 flowOn 改 VM 构造/测试（impact F2 / reverse R9） | 根因 B 列为次要/可选，见 §7；若做则走注入 dispatcher 并同步测试构造 |
| 🟠 测试替身同步（impact F3） | 同步 `FakeRecordRepository`/`FakeRecordDao`，忠实复刻语义 |
| 🟢 复用/等价（impact F4 / feasibility 2 / reverse R4） | mapper 复用提取的纯函数；等价性测试守护；relatedRecord 顺序对金额/性质 order-independent |
| 🟢 books_id 隔离（security #3） | DAO WHERE 含 `books_id=:booksId` |
| 🟢 count 不需要（feasibility 3 / impact F6） | R6a 用 Room 分页，无需 count |

## 6. 测试策略

- **mapper 等价性单测**（core:data，JVM）：构造 POJO（含普通支出/收入/关联收入/关联支出/平账/tags/images/多关联混合），断言 `map(pojo)` 与现有单条 `RecordModelTransToViewsUseCase(recordModel)` 逐字段等价——复用 `RecordModelTransToViewsUseCaseTest` 的数据构造思路。
- **@Relation 加载正确性 androidTest**（core:database，`connectedDebugAndroidTest`）：真库插入记录 + 关联，断言 `pocPagingLauncherRecordViews` 一页返回的 POJO 各 `@Relation` 集合正确（尤其双向 relatedRecord 不混淆、平账 type 空 List）。
- **ViewModel 测试**（feature:records）：`LauncherContentViewModelTest` 适配返回类型变更；验证 recordPagingData 产出 + insertSeparators + dailySummaries 不回归。
- **真机 journey 验收**（CLAUDE.md 完整链路验证）：模拟器首页——① 首屏列表正确加载（金额/报销显示正确）；② 滚到中部后新增/编辑/删除记录 **列表刷新且不回顶**；③ 切月/切「全部」数据正确。
- **截图测试**：`contentType` 不进快照；返回类型变更若触及 `LauncherContentScreenScreenshotTests` 构造需同步。

## 7. 次要/可选改进（根因 B）

因用户主诉为「当前月就慢」（根因 A 主导），且 flowOn 改造有测试成本（`LauncherContentViewModel` 构造加注入 dispatcher → 改 9 处测试构造，impact F2），根因 B 的 flowOn 作为**可选次要项**：

- 若纳入：给 `dailySummaries` 的 `.map { computeDailySummaries }` 与 `uiState` 的 `combine` 加 `flowOn(注入 Default dispatcher)`（位置须在重活 map 之后的上游，reverse R9），并同步改测试构造。
- 建议在 R6a 主交付验收后**单独评估**是否需要，避免扩大本次改动面。

## 8. 影响面与兼容性

- `getRecordPagingData` 的**唯一运行时消费方是首页**（impact 核验：`LauncherContentViewModel.kt:190`；Search/Asset/Typed 走各自 offset PagingSource，不受影响）——改动隔离。
- `transBatch` 仍被 Search/Asset/Typed 的 UseCase 使用（`GetAssetRecordViewsUseCase.kt:49` 等），**非 dead code**，不删除。
- 提取纯函数后，`RecordModelTransToViewsUseCase` 的单条/批量改调 core:model 版本，保持行为不变。

## 9. 回滚/退路

改动自包含（新 POJO + 新 DAO 方法 + Repository mapper + ViewModel 一行 map + contentType）。若验收发现问题，可回退 `getRecordPagingData` 到原 Room `pagingQueryByBooksIdBetweenDate` + 逐条 transToViews。R6a 因继承 Room 生成 PagingSource，行为风险低于方案 1。

## 10. 非目标（YAGNI）

- 不改金额计算口径（finalAmount/recordAmount/analyticsPie 三口径）。
- 不动 Search/Asset/Typed 三屏分页（本次仅首页）。
- 不引入 count 查询。
- 根因 B 的大范围（全部/按年）汇总优化不在本次主交付（见 §7）。
