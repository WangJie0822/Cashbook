# Cashbook 评审 Top10 剩余 5 项修复 — 设计方案(spec)

> 状态:已过节点 1 team-review 四维评审 + controller hands-on 仲裁 + 用户决策,待用户最终 review → writing-plans
> 创建:2026-06-01
> 背景:承接 `docs/review/2026-05-30-comprehensive-review.md`。Critical 与 7 项关键 High 已于 2026-05-30 修复合并 main。本方案处理评审 Top10 中尚未完成的 5 项(不含需真机回归的 #5 Keystore)。

## 范围

| 项 | 评审严重度 | 当前状态(已 hands-on 核实) |
|---|---|---|
| #7 N+1 其余 caller 批量化 | High | 部分(`transBatch` 已存在,1/9 接入;余 7 个非 Paging UseCase 单条) |
| #9a 微信导入解析/匹配/VM 测试 | High | parser/matcher 零测试;`RecordImportViewModelTest` 已存在(扩充) |
| #9b RepositoryImplTest 虚假覆盖 | High | **6 个**(非 4)`*RepositoryImplTest` 多测 Fake;`MappingTest.kt` 已全覆盖 asModel/asTable |
| #10a AutoBackupWorker 失败重试/上报 | Medium | `doWork()` 恒返回 `Result.success()` |
| #10b 抽公共 recordAmount 函数 | Medium | 公式分**两种口径**在 4 处重复 |

## 已核实的关键约束

1. `RecordModelTransToViewsUseCase.kt:92-173` 已有 `transBatch(records)`,与单条 `invoke` 逐字段等价(逐分支核对:空列表/asset=-1/平账类型 -1101/-1102/mapNotNull 丢弃/标签逐条均一致)。
2. `CombineProtoDataSource.kt:56-57` 为 `@Singleton class ...@Inject(6 DataStore<Proto>)` —— final class、无接口;4 个 Impl 全依赖它。→ "直接 `new Impl(fakeDao, fakeDataSource)`"不可落地。
3. `core/common` 无 `core/model` 依赖;`core/model` 是纯 JVM 库;`core/domain`、`core/database` 均依赖 `core/model`。→ 公共金额函数放 **core/model** 最优。
4. **金额公式两种口径**(经 controller 核实):
   - **DAO/月度口径**(`INCOME` vs 非`INCOME`,TRANSFER 当支出):`TransactionDao.calculateRecordAmount:156-162`(`charge`)、`GetAssetMonthSummaryUseCase.kt:65-69`(`charges`)。
   - **Pie 口径**(`EXPENDITURE` vs 非`EXPENDITURE`,TRANSFER 当收入):`TransRecordViewsToAnalyticsPieUseCase.kt:48-52/74-78`、`TransRecordViewsToAnalyticsPieSecondUseCase.kt:48-52/66-70`。
   - 两口径对 INCOME/EXPENDITURE 结果一致,**仅 TRANSFER 分叉**;`AnalyticsViewModel.kt:103-107` 把 EXPENDITURE/INCOME/TRANSFER 三种都传给 Pie UseCase,故 TRANSFER 可达。**单一公共函数无法统一**。
5. `BackupRecoveryManager.kt:27` 是 interface,`requestAutoBackup()` suspend 返回 Unit;`BackupRecoveryState` 是 sealed(`Failed(code)`/`Success(code)`)。`requestAutoBackup` 真实调用方仅 `AutoBackupWorker.kt:59` + `FakeBackupRecoveryManager.kt:72` +(接口实现)`BackupRecoveryManagerImpl.kt:146`;`BackupAndRecoveryViewModel` 调的是 `requestBackup()`(:184/240),**不调 requestAutoBackup**。
6. `androidx-work-testing`(2.10.1)已声明于 `gradle/libs.versions.toml:263`;`sync/work` 当前零单测,需新建 `src/test` + test 依赖块。

## 节点 1 team-review 修订纪要

| 来源 finding | 修订 |
|---|---|
| **Critical C1**(reverse,controller 核实成立;security S5 证伪)#10b 单一函数对 TRANSFER 不等价 | #10b 改为抽**两个**口径函数 |
| **High H2**(三方印证)`MappingTest.kt` 已存在 | #9b 删"新建映射测试",改"重命名 + 审计补缺" |
| **High H3**(feasibility+impact)parse() 纯 JVM XmlPullParser 可能 Stub | #9a parse() 测试**先 PoC** 再定策略 |
| **High H1**(security)#9a fixture PII | fixture 必须脱敏(硬约束) |
| **Medium M1**(impact)#10b 漏第 4 处 | 纳入 `GetAssetMonthSummaryUseCase`(DAO 口径) |
| **Medium M2**(三方)#10a 影响面 | ViewModel 不改;真实调用方 = Worker+Fake+Impl |
| **Medium M3**(feasibility)#10a 错误码 | UNAUTHORIZED 混瞬时 IO,记为已知局限 |
| **Medium M4**(feasibility+impact)#9a 模块 | parser/matcher 测试归 core/data,VM 测试归 feature/record-import |
| **Low**(impact+reverse)#9b 6 个非 4 个 | 计数订正 |
| **Low**(feasibility I1)#7 等价性 | 每 caller 单测显式断言"批量==单条逐元素" |

## 方案明细

### #7 N+1 其余 caller 批量化(改 7 个非 Paging UseCase)

复用 `transBatch`(逐字段等价已验证):
- **排序型 4 个**(`GetAssetRecordViewsUseCase.kt:51-52`、`GetSearchRecordViewsUseCase.kt:48-50`、`GetTagRecordViewsUseCase.kt:48-50`、`GetTypeRecordViewsUseCase.kt:65-67`):`list.sortedByDescending{it.recordTime}` → `recordModelTransToViewsUseCase(sorted)` → `.map{it.asEntity()}`
- **Flow 型** `GetCurrentMonthRecordViewsUseCase.kt:38-39`:`mapLatest{ recordModelTransToViewsUseCase(it).map{e->e.asEntity()} }`
- **filter 型** `GetRelatedRecordViewsUseCase.kt:63-66`:`records.filter{queryRelatedRecordCountById(it.id)<=0}` → `recordModelTransToViewsUseCase(filtered)` → `.map{it.asEntity()}`(filter 内 `queryRelatedRecordCountById` 逐条查询是另一处 N+1,**范围外不动**)

**不改**:`GetRecordViewsUseCase.kt:40-41`(单条非 N+1)、`LauncherContentViewModel.kt:115-116`(Paging 逐元素异步,无法按页批量,代码注释说明)。

测试:每个改动 UseCase 用 core:testing Fake **显式断言"批量结果 == 单条逐元素结果"**(含 relatedRecord 列表顺序、filter/排序语义保持)。

包名:`cn.wj.android.cashbook.domain.usecase`(非 .core.domain.usecase)。

### #9a 微信导入测试

**硬约束:测试 fixture 必须人工脱敏构造**(对手方用虚构名、卡号后四位用 0000、单号用全 0 占位、金额用整百;fixture 顶部注释声明"人工合成,无真实交易");commit 前 grep 校验无真实姓名/卡号模式。**严禁用真实导出账单。**

模块归属:parser/matcher 在 `core/data/helper`,测试归 **core/data/src/test**;仅 VM 测试归 feature/record-import。

- `core/data/src/test`:新建 `BillCategoryMatcherTest`(表驱动 `match`)、`BillPaymentMatcherTest`(表驱动 `matchAll` 4 阶 + `extractCardSuffix`)。
- `WechatBillParser`:`parseDateTime`(3 格式)、`convertToItem` 加 `internal`+`@VisibleForTesting` 纯函数单测。`parse()` 端到端:**plan 阶段先实跑最小 parse() unit test 验 XmlPullParser 可行性**——可跑→脱敏小样本 xlsx 端到端测;不可跑→退化(parse() 暂不端到端,纯函数覆盖为主),Robolectric 作为可选升级届时定。
- `feature/record-import/src/test`:扩充 `RecordImportViewModelTest` happy-path(预览金额分/方向/去重落库)。

### #9b RepositoryImplTest 测试债(重命名 + 审计补缺)

- 枚举 **6 个** `*RepositoryImplTest`(Asset/Books/Record/Setting/Tag/Type),逐一核实实际被测对象:
  - 纯测 Fake DAO/DataSource 委托的 → **诚实重命名**反映真实被测对象(消除"测了 Impl"的误导)。
  - 手工复刻 Impl 业务逻辑的(如 `AssetRepositoryImplTest:168-191`)→ **保留**(意图是覆盖 Impl 逻辑),不改名成 DaoTest。
- **审计 `MappingTest.kt`** 字段覆盖缺口并补齐(若有);**不新建映射测试**(已全覆盖 6 类 asModel/asTable 双向)。
- 不动生产架构(不抽 CombineProtoDataSource 接口),零回归。

### #10a AutoBackupWorker 失败重试(改接口)

- `BackupRecoveryManager.requestAutoBackup()` 与 `requestBackup()` 均改 `suspend ...: BackupRecoveryState`(让 requestAutoBackup 经 requestBackup 同步拿到终态);`BackupAndRecoveryViewModel` 调 `requestBackup()` 忽略返回值,Kotlin 兼容,**源码不破坏、无需改**。〔plan 阶段勘误:原写"requestBackup 不改"不成立——requestAutoBackup 经 requestBackup 委托,后者须返回 state〕
- `BackupRecoveryManagerImpl`:`startBackup`(private)重构为 **return 最终 state**(early-return 行改返回 `Failed(...)`,末尾返回算出的 state);`requestAutoBackup` 透传返回。调用链全 suspend 同步执行(非 fire-and-forget,已核实)。
- `FakeBackupRecoveryManager.requestAutoBackup()` 适配返回 state。
- `AutoBackupWorker.doWork()`:`SUCCESS_BACKUP→Result.success()`;`FAILED_BACKUP_WEBDAV→Result.retry()`+`setBackoffCriteria(EXPONENTIAL,...)`;配置类(`FAILED_BLANK_BACKUP_PATH`/`FAILED_BACKUP_PATH_UNAUTHORIZED`/`FAILED_FILE_FORMAT_ERROR`)→`Result.failure()`。**Result 不附带 outputData**(避免备份路径/异常 message 泄露)。
- **已知局限**(plan 记录):`startBackup` catch-all 把瞬时 IO 异常也归 `FAILED_BACKUP_PATH_UNAUTHORIZED`,这类会被判 failure 不重试;本次不细化异常分类(范围控制),仅注释标注。
- `sync/work`:加 `testImplementation(libs.androidx.work.testing)` + 建 `src/test` 依赖块,补 Worker 测试(`TestListenableWorkerBuilder`,据 Fake 返回 state 断言 Result 三态)。

### #10b 抽两个口径金额函数

core/model 新建 2 个语义明确的纯函数(KDoc 须说明两口径对 TRANSFER 的不同处理 + 为何不能合并):

```kotlin
// DAO/月度口径:INCOME 扣手续费;EXPENDITURE/TRANSFER 加手续费减优惠
fun recordAmount(category, amount: Long, charges: Long, concessions: Long): Long =
    if (category == INCOME) amount - charges else amount + charges - concessions

// Analytics 饼图口径:EXPENDITURE 加手续费减优惠;INCOME/TRANSFER 扣手续费
fun analyticsPieAmount(typeCategory, amount: Long, charges: Long, concessions: Long): Long =
    if (typeCategory == EXPENDITURE) amount + charges - concessions else amount - charges
```

- **`recordAmount` 复用 2 处**:`TransactionDao.calculateRecordAmount`(传 `record.charge` 作 charges)、`GetAssetMonthSummaryUseCase.kt:65-69`。
- **`analyticsPieAmount` 复用 2 处**:`TransRecordViewsToAnalyticsPieUseCase.kt:48/74`、`TransRecordViewsToAnalyticsPieSecondUseCase.kt:48/66`。
- 每函数 = 对应原公式,**零行为变更**;消除全部 4 处副本。
- **不碰**:`RecordModelTransToViewsUseCase.sumRelatedAmount`(语义不同:关联金额累加)。
- 测试:`RecordAmountTest`(recordAmount × INCOME/EXPENDITURE/**TRANSFER**)、`AnalyticsPieAmountTest`(analyticsPieAmount × 同)、为两个 Pie UseCase 补 **TRANSFER 金丝雀用例**(带非零 charges/concessions,固化"TRANSFER→amount-charges"防回归)。`TransactionDaoLogicTest:80-102` 三测自动护航 DAO 委托重构。

## 执行策略

controller 串行 TDD,每项独立原子 commit:读 → 红测试 → 实现 → 跑对应 module `testDebugUnitTest` → commit → 自核验(不用后台 Workflow)。

module 顺序(按依赖):
1. **core/model**:#10b 两个口径函数 + RecordAmountTest/AnalyticsPieAmountTest
2. **core/database**:#10b `TransactionDao.calculateRecordAmount` 委托 recordAmount
3. **core/domain**:#7 七个 UseCase 批量化 + #10b 两 Pie 用 analyticsPieAmount + GetAssetMonthSummary 用 recordAmount + Pie TRANSFER 金丝雀
4. **core/data**:#9b 重命名/审计 + #9a parser/matcher 测试(先 parse PoC)+ #10a 接口/Impl 改返回值
5. **feature/record-import**:#9a VM 测试扩充
6. **sync/work**:#10a Worker 改 + Worker 测试

gradle:`-Dorg.gradle.jvmargs="-Xmx4g ..."` 前台 `--no-daemon --console=plain`;worktree 首构建缺依赖带本地代理 7897 暖缓存,之后 `--offline`。

## 影响面

- **公共接口变更** `requestAutoBackup()` 与 `requestBackup()` 均返回 `BackupRecoveryState`(#10a):需适配 `AutoBackupWorker` + `FakeBackupRecoveryManager` + `BackupRecoveryManagerImpl`;**`BackupAndRecoveryViewModel` 不改**(调 requestBackup 忽略返回值,Kotlin 兼容)。
- `TransactionDao.calculateRecordAmount`(#10b):签名不变,内部委托 `recordAmount`,行为等价,迁移测试/`TransactionDaoLogicTest` 不受影响。
- #9b 重命名仅影响测试源集。
- #7 改动为 UseCase 内部,对外签名不变;**无 Composable/ViewModel 签名变化,不牵连截图测试**。

## Out of Scope

- #5 Keystore `setUserAuthenticationRequired(true)`(需真机回归)。
- `GetRelatedRecordViewsUseCase` 的 `queryRelatedRecordCountById` 逐条 N+1。
- `LauncherContentViewModel` Paging 批量化。
- `EditRecordViewModel:146` 关联金额公式(Info 项)。
- #10a `startBackup` 错误码细化(瞬时 IO vs 权限),记为已知局限。
- 评审报告其余 Medium/Low/Info 项。
