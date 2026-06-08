# 首屏 gate 优化（M-1 去 netRecalc）+ 备份恢复刷新一致性（F2）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `LauncherContentViewModel` 的净自付全量重算从首屏 gate 中移出后台静默执行（M-1），并让备份恢复后的重算走 Repository 统一刷新副作用（F2）。

**Architecture:** M-1 在 `init` 协程内把 `_migrationCompleted=true` 提到 netRecalc 之前（同协程后台续跑 suspend 重算），db9To10 迁移分支保留 gate；F2 给 `BackupRecoveryManagerImpl` 注入 `RecordRepository`、把直调 DAO 改为 `recordRepository.recalculateAllFinalAmount()`。刷新链（bump `recordDataVersion` + Room PagingSource 自动 invalidate）复用既有机制，不改算法。

**Tech Stack:** Kotlin Coroutines/Flow、Hilt、Room、JUnit + Truth、`UnconfinedTestDispatcher`。

**Spec:** `docs/superpowers/specs/2026-06-08-launcher-firstscreen-gate-optimization-design.md`（已吸收 team-review 四维 finding：H-1 措辞 / M-A 幂等 / M-B 并发 / M-C 事务边界 / M-D 测试区分力 / M-E 列类型 / M-F Fake 默认不挂起）。

---

## File Structure

| 文件 | 责任 | Task |
|---|---|---|
| `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt` | 测试替身：给 `recalculateAllFinalAmount`/`migrateAfter9To10` 加 invocationCount + 可控挂起开关 + 进入信号（默认不挂起） | 1 |
| `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt` | M-1：`init` gate 分离 | 2 |
| `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt` | M-1 测试：去 gate 区分力 + db9To10 gate 保留 | 2 |
| `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt` | F2：注入 `RecordRepository`，恢复后走 repository | 3 |

`DataModule.kt`（`:46-50` `@Binds bindBackupRecoveryManager`）**无需改**——Hilt 自动解析新增构造参数。

---

## Task 1: FakeRecordRepository 测试替身改造（可控挂起 + 计数 + 进入信号）

**Files:**
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt:297-307`

> 目的：让 `LauncherContentViewModelTest` 能区分"首屏放行"与"重算/迁移进行中"。**开关默认不挂起**（M-F 硬约束）——该 Fake 被 32 个测试文件复用，默认 `gate=null` 立即返回才不破坏现有测试。`migrateAfter9To10` 当前无计数（`:297-299` 纯 no-op），需与 `recalculateAllFinalAmount` 对称补齐（F-feas-3）。

- [ ] **Step 1: 确认现有相关测试基线绿（改前）**

Run（PowerShell，先按本机内存检查 + offline 暖缓存约定）：
```
.\gradlew :feature:records:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`（只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`，不信 exit code）。记录现有 `LauncherContentViewModelTest` 全绿作为基线。

- [ ] **Step 2: 改造 Fake 的两个方法 + 加挂起开关字段**

把 `FakeRecordRepository.kt:297-307` 整段替换为：

```kotlin
    /** db9To10 迁移调用次数（供 gate 测试断言） */
    var migrateAfter9To10Count: Int = 0

    /** 非 null 时 [migrateAfter9To10] 挂起在此 deferred；默认 null=不挂起立即返回（保持 32 个复用测试不破） */
    var migrateSuspendGate: CompletableDeferred<Unit>? = null

    /** [migrateAfter9To10] 进入信号：入口 complete，供测试确认协程已停在挂起点 */
    var migrateStartedSignal: CompletableDeferred<Unit>? = null

    override suspend fun migrateAfter9To10() {
        migrateAfter9To10Count++
        migrateStartedSignal?.complete(Unit)
        migrateSuspendGate?.await()
    }

    /** 净自付重算调用次数（供 gate 触发测试断言） */
    var recalculateAllFinalAmountCount: Int = 0

    /** 非 null 时 [recalculateAllFinalAmount] 挂起在此 deferred；默认 null=不挂起立即返回 */
    var recalcSuspendGate: CompletableDeferred<Unit>? = null

    /** [recalculateAllFinalAmount] 进入信号：入口 complete，供测试确认协程已停在挂起点 */
    var recalcStartedSignal: CompletableDeferred<Unit>? = null

    override suspend fun recalculateAllFinalAmount() {
        recalculateAllFinalAmountCount++
        recalcStartedSignal?.complete(Unit)
        recalcSuspendGate?.await()
    }
```

- [ ] **Step 3: 加 import**

在 `FakeRecordRepository.kt` import 区加（若未存在）：
```kotlin
import kotlinx.coroutines.CompletableDeferred
```

- [ ] **Step 4: 跑全部复用 Fake 的模块测试，确认默认不挂起未破坏现有测试（M-F）**

Run（逐个，遵守本机内存检查；feature 模块用 `testDebugUnitTest`，core:domain 同）：
```
.\gradlew :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:assets:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:types:testDebugUnitTest --offline --no-daemon --console=plain
```
（`record-import` 模块若存在独立 gradle 路径一并跑；以 `settings.gradle.kts` 实际模块名为准。）
Expected: 全部 `BUILD SUCCESSFUL`，现有用例零回归（默认 `gate=null` → 方法立即返回，行为与改前一致）。

- [ ] **Step 5: Commit**

```
git add core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt
git commit -m "[test|core|首屏gate优化][公共]FakeRecordRepository 加 recalc/migrate 可控挂起开关+计数+进入信号(默认不挂起,M-F对称F-feas-3)"
```

---

## Task 2: M-1 LauncherContentViewModel.init gate 分离（TDD）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt:67-79`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt`

> 核心是 M-D 的"区分力"：测试 A 必须对**旧顺序**（recalc 在前、置位在末尾）FAIL（挂起中 Loading），对**新顺序** PASS（Success）。先写测试 + 证旧顺序红，再改实现证绿。

- [ ] **Step 1: 写失败测试 A（去 gate 区分力）+ 测试 B（db9To10 gate 保留）**

在 `LauncherContentViewModelTest.kt` 加两个用例（import 见 Step 2）：

```kotlin
    @Test
    fun when_net_recalc_running_then_uiState_already_success() = runTest {
        // M-D 区分力：净自付重算"进行中"时首屏应已放行为 Success（旧顺序此时为 Loading → 对旧实现 FAIL）
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true,
                preferenceSplit = true,
                finalAmountNetRecalcDone = false,
            ),
        )
        val repo = FakeRecordRepository()
        repo.recalcStartedSignal = CompletableDeferred()
        repo.recalcSuspendGate = CompletableDeferred() // 挂起重算，不放行
        val useCase = RecordModelTransToViewsUseCase(
            recordRepository = repo,
            typeRepository = FakeTypeRepository(),
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val vm = LauncherContentViewModel(
            booksRepository = booksRepository,
            settingRepository = settingRepository,
            recordRepository = repo,
            recordModelTransToViewsUseCase = useCase,
        )

        val collectJob = launch(UnconfinedTestDispatcher()) { vm.uiState.collect() }
        repo.recalcStartedSignal!!.await() // 确认重算已进入挂起点
        assertThat(vm.uiState.value).isInstanceOf(LauncherContentUiState.Success::class.java)

        repo.recalcSuspendGate!!.complete(Unit) // 放行重算
        assertThat(repo.recalculateAllFinalAmountCount).isEqualTo(1)
        collectJob.cancel()
    }

    @Test
    fun when_db9To10_migrating_then_uiState_loading_until_done() = runTest {
        // db9To10 分支保留 gate：migrate 进行中 uiState=Loading，完成后转 Success
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = false,
                preferenceSplit = true,
                finalAmountNetRecalcDone = false,
            ),
        )
        val repo = FakeRecordRepository()
        repo.migrateStartedSignal = CompletableDeferred()
        repo.migrateSuspendGate = CompletableDeferred() // 挂起 migrate
        val useCase = RecordModelTransToViewsUseCase(
            recordRepository = repo,
            typeRepository = FakeTypeRepository(),
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val vm = LauncherContentViewModel(
            booksRepository = booksRepository,
            settingRepository = settingRepository,
            recordRepository = repo,
            recordModelTransToViewsUseCase = useCase,
        )

        val collectJob = launch(UnconfinedTestDispatcher()) { vm.uiState.collect() }
        repo.migrateStartedSignal!!.await() // migrate 进入挂起点
        assertThat(vm.uiState.value).isInstanceOf(LauncherContentUiState.Loading::class.java)

        repo.migrateSuspendGate!!.complete(Unit) // 放行 migrate
        assertThat(vm.uiState.value).isInstanceOf(LauncherContentUiState.Success::class.java)
        assertThat(repo.migrateAfter9To10Count).isEqualTo(1)
        collectJob.cancel()
    }
```

- [ ] **Step 2: 加 import（若缺）**

`LauncherContentViewModelTest.kt` 顶部确保有：
```kotlin
import kotlinx.coroutines.CompletableDeferred
```
（`launch`、`UnconfinedTestDispatcher`、`runTest`、`CashbookTestRunner` 相关、`TempKeysModel`、各 Fake、`RecordModelTransToViewsUseCase` 现有文件已 import。）

- [ ] **Step 3: 运行测试 A，确认对当前（旧顺序）实现 FAIL（证明区分力）**

Run:
```
.\gradlew :feature:records:testDebugUnitTest --offline --no-daemon --console=plain --tests "*LauncherContentViewModelTest.when_net_recalc_running_then_uiState_already_success"
```
Expected: **FAIL**。当前代码（`:67-79` recalc 在前、`_migrationCompleted=true` 在 `:77` 末尾）下，重算挂起时 `_migrationCompleted` 仍 false → `uiState=Loading`，断言 Success 失败。这一步证明测试真能抓住"未去 gate"退化。

- [ ] **Step 4: 改实现——init gate 分离**

把 `LauncherContentViewModel.kt:67-79` 的 `init { ... }` 整块替换为：

```kotlin
    init {
        viewModelScope.launch {
            val tempKeys = settingRepository.tempKeysModel.first()
            if (!tempKeys.db9To10DataMigrated) {
                // final_amount 全为 Migration9To10 DEFAULT 0，首屏会显全 0 → 必须 gate（先迁移后置位，不可调换，impact L-1）
                recordRepository.migrateAfter9To10()
                _migrationCompleted.value = true
            } else {
                // 已迁移：立即放行首屏（finalAmount 为旧吸收模型值——被吸收支出=0/吸收者可负，首屏短暂显旧值）
                _migrationCompleted.value = true
                if (!tempKeys.finalAmountNetRecalcDone) {
                    // 净自付重算后台静默跑；完成后 recalculateAllFinalAmount 内部 bump recordDataVersion，
                    // 汇总流（订阅 version）+ 列表（Room PagingSource 对 db_record UPDATE 自动 invalidate）刷新到净自付值
                    recordRepository.recalculateAllFinalAmount()
                }
            }
        }
    }
```

- [ ] **Step 5: 运行测试 A + B + 现有用例，确认全 PASS**

Run:
```
.\gradlew :feature:records:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`。测试 A（重算挂起中 Success）PASS、测试 B（migrate 挂起中 Loading→放行 Success）PASS、现有 `when_db9To10_done_but_net_recalc_not_done...`（计数=1）等全绿。

- [ ] **Step 6: Commit**

```
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt
git commit -m "[perf|feature|首屏gate优化][公共]M-1 LauncherContentViewModel netRecalc 去 gate 后台静默重算(db9To10保留gate)+区分力测试(旧顺序FAIL/新顺序PASS)"
```

---

## Task 3: F2 BackupRecoveryManagerImpl 恢复后走 Repository

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（构造 `:83-91` + 调用点 `:699`）

> F2 测试覆盖现实：`BackupRecoveryManagerSchemeTest` 只测 Companion 纯函数、不实例化 Manager；Manager 构造依赖 Context/WebDAV/database 实例化成本高。本 Task **降级为编译期签名保证 + 人工核验调用点**（F-feas-4）——交付说明须标注"F2 调用点替换无自动化测试覆盖，靠编译期签名 + 人工 diff 核验"。事务/线程边界已 controller hands-on 核验安全（spec §3 事实表）。

- [ ] **Step 1: 构造注入 RecordRepository**

`BackupRecoveryManagerImpl.kt` 构造（`:83-91`）加参数。把：
```kotlin
class BackupRecoveryManagerImpl @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val settingRepository: SettingRepository,
    private val webDAVHandler: WebDAVHandler,
    private val database: CashbookDatabase,
    private val combineProtoDataSource: CombineProtoDataSource,
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) : BackupRecoveryManager {
```
改为（新增 `recordRepository`）：
```kotlin
class BackupRecoveryManagerImpl @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
    private val webDAVHandler: WebDAVHandler,
    private val database: CashbookDatabase,
    private val combineProtoDataSource: CombineProtoDataSource,
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) : BackupRecoveryManager {
```

- [ ] **Step 2: 加 import**

`BackupRecoveryManagerImpl.kt` import 区加：
```kotlin
import cn.wj.android.cashbook.core.data.repository.RecordRepository
```

- [ ] **Step 3: 调用点改走 repository**

`BackupRecoveryManagerImpl.kt:699`（恢复成功合并后那行，语义定位："重置 refund/reimburse/creditCard typeId 标志之后、`return SUCCESS_RECOVERY` 之前的全表重算"）。把：
```kotlin
                database.transactionDao().recalculateAllFinalAmount()
```
改为：
```kotlin
                // F2：走 Repository 统一副作用（重算 + 置 finalAmountNetRecalcDone + bump recordDataVersion）
                recordRepository.recalculateAllFinalAmount()
```

- [ ] **Step 4: 编译 + 跑 core:data 测试（验证 DI 图与既有测试不破）**

Run:
```
.\gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`。编译通过证明构造签名变更被 Hilt `@Binds`（DataModule `:46-50`）自动解析、无循环依赖；`BackupRecoveryManagerSchemeTest` 等既有测试绿。

- [ ] **Step 5: 人工核验调用点（F-feas-4 诚实标注）**

`git diff core/data/.../BackupRecoveryManagerImpl.kt` 确认：① `:699` 唯一改动点确实由 `database.transactionDao()...` 改为 `recordRepository...`；② 无其它残留直调 DAO 的重算点（grep `transactionDao().recalculateAllFinalAmount` 在该文件应为 0）。

Run:
```
git -C . diff --stat
```
Expected: 仅 `BackupRecoveryManagerImpl.kt` 改动。

- [ ] **Step 6: Commit**

```
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt
git commit -m "[refactor|core|首屏gate优化][公共]F2 备份恢复走 recordRepository.recalculateAllFinalAmount 统一置标记+bump version(切IO dispatcher已核验安全)"
```

---

## Task 4: 全量回归验证

**Files:** 无（仅验证）

- [ ] **Step 1: 跑全部受影响模块单测**

Run（遵守本机内存检查；以 `settings.gradle.kts` 实际模块名为准补齐 record-import）:
```
.\gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:records:testDebugUnitTest :feature:assets:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:types:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: 全部 `BUILD SUCCESSFUL`（逐模块确认 `^BUILD SUCCESSFUL`）。

- [ ] **Step 2: 编译 app 主 flavor 确认全链路无断**

Run:
```
.\gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`（验证 DI 图在 app 层装配通过）。

- [ ] **Step 3: 记录交付说明**

汇总：M-1 去 gate（含区分力测试旧 FAIL/新 PASS 证据）；F2 走 repository（**显式标注**"调用点替换无自动化测试覆盖，靠编译期签名 + 人工 diff 核验"）；可见性刷新链复用净自付重构既有机制，未新增测试；androidTest（`TransactionDaoTest`）本机无设备未跑，本次改动不涉 DAO 算法、无新增 androidTest。

---

## Self-Review（plan 对照 spec）

- **spec §4 改动 1（M-1 gate 分离）** → Task 2 Step 4 完整代码 ✅
- **spec §4 改动 2（F2 走 repository）** → Task 3 Step 1/3 完整代码 ✅
- **spec §5 测试（区分力旧FAIL/新PASS、db9To10 gate、Fake 默认不挂起、跑全模块、F2 降级标注）** → Task 1 + Task 2 Step 1/3/5 + Task 4 ✅
- **spec §5 可见性回归** → 诚实降级：ViewModel+Fake 无法覆盖端到端刷新链，复用既有机制，已在 Task 4 Step 3 交付说明标注（非 placeholder，是有依据的范围决策）✅
- **spec §7 impact L-1（db9To10 先 migrate 后置位）** → Task 2 Step 4 代码注释 + 顺序固定 ✅
- **No placeholder**：所有代码步骤含完整代码；测试含完整断言 ✅
- **类型一致**：`recalcSuspendGate`/`recalcStartedSignal`/`recalculateAllFinalAmountCount`/`migrateSuspendGate`/`migrateStartedSignal`/`migrateAfter9To10Count` 在 Task 1 定义、Task 2 测试一致引用 ✅
