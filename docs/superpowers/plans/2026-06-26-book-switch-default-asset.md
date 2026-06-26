# 切换账本后默认资产按账本隔离 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复切换账本后新建记录默认带出其他账本资产的 bug——默认资产改由当前账本最近一条「有效、可见、在册」记录派生，不再读全局 `lastAssetId`。

**Architecture:** 在 `RecordDao` 新增一条 `@Query`（带账本+可见资产子查询，`ORDER BY id DESC LIMIT 1`），`RecordRepositoryImpl.getDefaultRecord` 改读它；移除已无人读的 `lastAssetId` 死写。无 DB schema 变更（纯查询）。JVM 侧覆盖落在「FakeRecordDao 忠实复刻该查询」的单测；真 SQL 语义由 `RecordDaoTest` androidTest 守护（`RecordRepositoryImpl` 因 `CombineProtoDataSource` 为 final 无法 JVM 实例化，其一行委托不单独 JVM 测）。

**Tech Stack:** Kotlin, Room (KSP), Hilt, Coroutines/Flow, JUnit4 + Truth, Robolectric, AndroidJUnit4 instrumented test。

## Global Constraints

- **金额单位为分（Long）**：本任务不涉金额计算，但 `RecordTable` 字段沿用既有类型，勿改。
- **无 DB schema 变更**：仅新增 `@Query` SELECT，不触任何 `@Entity`；**禁止**新增 migration / schema JSON / version bump。
- **DAO 新增抽象方法须同步 `FakeRecordDao`**：否则 `:core:data:compileDebugUnitTestKotlin` 报 not abstract、整模块测试编译失败。
- **测试替身忠实复刻**：`FakeRecordDao.queryLastUsedAssetId` 必须真实复刻 SQL 语义（账本过滤 + 可见在册资产子查询 + 最大 id），禁止 `emptyList`/`-1L`/忽略资产归属的宽松桩。
- **模块测试任务名**：`core:database`/`core:data`/`feature:records` 均为 Android 库 → JVM 单测 `:模块:testDebugUnitTest`；`core:database` instrumented → `:core:database:connectedDebugAndroidTest`（需模拟器/真机）。
- **本机 Gradle**：直连 Maven Central 不通，需要联网拉依赖时清继承代理后加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`；缓存已暖后增量可 `--offline --no-daemon --console=plain`。`connectedDebugAndroidTest` 首次需联网拉 UTP，受代理 TLS 稳定性影响（属环境问题，非代码）。
- **提交信息格式**：`[类型|模块|功能][影响范围]中文说明`，原子化、最小范围 stage。
- **License Header**：新增/修改文件保持既有 Apache 2.0 头（Spotless 检查）；本任务均为改既有文件，不新建文件。

---

### Task 1: 新增 `queryLastUsedAssetId` DAO 查询 + FakeRecordDao 忠实复刻 + JVM 单测

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（新增抽象方法，建议加在 `queryEarliestRecordTime`（约 :557-558）之后）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（加 `assets` 集合 + `addAsset` + 实现）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`（加 `createAsset` helper + 6 个用例）

**Interfaces:**
- Produces:
  - `RecordDao.queryLastUsedAssetId(booksId: Long): Long?` —— 当前账本最近一条「资产可见且在册」记录的 `asset_id`，无则 `null`
  - `FakeRecordDao.assets: MutableList<AssetTable>`、`FakeRecordDao.addAsset(asset: AssetTable)`

- [ ] **Step 1: 在 RecordDao 新增抽象查询方法**

在 `RecordDao.kt` 接口内（`queryEarliestRecordTime` 方法之后）加入。`$SWITCH_INT_OFF` 已在本文件被其它 `@Query` 使用（如 `reimbursable=$SWITCH_INT_ON`），import 已存在，无需新增：

```kotlin
    /**
     * 查询当前账本 [booksId] 最近一条「资产仍存在、可见、属于本账本」记录的资产 id。
     *
     * 用于新建记录默认资产（按账本隔离，替代旧的全局 lastAssetId）。子查询保证：
     * - 不串号：asset 的 books_id 与记录同账本二次过滤
     * - 不悬空：已删资产不在 db_asset，自动排除
     * - 不带出隐藏资产：invisible=$SWITCH_INT_OFF，与 queryVisibleAssetByBookId 一致
     * - asset_id=-1（无资产记录）天然不命中子查询、被跳过
     * - ORDER BY id DESC = 最近创建（id 为自增主键）；无匹配返回 null
     */
    @Query(
        """
        SELECT asset_id FROM db_record
        WHERE books_id = :booksId
          AND asset_id IN (
            SELECT id FROM db_asset
            WHERE books_id = :booksId AND invisible = $SWITCH_INT_OFF
          )
        ORDER BY id DESC LIMIT 1
        """,
    )
    suspend fun queryLastUsedAssetId(booksId: Long): Long?
```

- [ ] **Step 2: 编译 core:database，验证 Room 接受该 SQL**

Room 在 KSP 阶段校验 `@Query` SQL（列名/语法/返回类型）。

Run: `./gradlew :core:database:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（若缓存缺依赖去掉 `--offline` 并加代理 `-D` 参数，见 Global Constraints）。SQL 有误则 KSP 在此报错。

- [ ] **Step 3: 给 FakeRecordDao 加 `assets` 集合、`addAsset`、桩实现**

在 `FakeRecordDao.kt` 顶部 import 区加：

```kotlin
import cn.wj.android.cashbook.core.database.table.AssetTable
```

在 `tagWithRecords` 集合声明（约 :49）之后加：

```kotlin
    /** 资产数据列表，用于 queryLastUsedAssetId 的资产归属/可见性子查询复刻 */
    val assets = mutableListOf<AssetTable>()
```

在 `addRecord(...)` 方法（约 :494-498）之后加 `addAsset` 与**桩**实现（桩先返回 null，便于下一步看到测试失败）：

```kotlin
    fun addAsset(asset: AssetTable) {
        assets.add(asset)
    }

    override suspend fun queryLastUsedAssetId(booksId: Long): Long? {
        // 桩：Step 6 替换为忠实实现
        return null
    }
```

- [ ] **Step 4: 在 RecordRepositoryImplTest 写 `createAsset` helper + 6 个用例**

先在 `RecordRepositoryImplTest.kt` import 区加：

```kotlin
import cn.wj.android.cashbook.core.database.table.AssetTable
```

在文件末尾 `createRecord(...)` helper（约 :487-511）之后、类闭合 `}` 之前加 helper：

```kotlin
    private fun createAsset(
        id: Long,
        booksId: Long = 1L,
        invisible: Int = SWITCH_INT_OFF,
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = "资产$id",
        balance = 0L,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = 0,
        classification = 0,
        invisible = invisible,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = 0L,
    )
```

在「查询委托测试」区之后加 6 个用例（`SWITCH_INT_ON` 已在本文件 import，:20）：

```kotlin
    // ========== queryLastUsedAssetId（任务1：默认资产按账本隔离）==========

    @Test
    fun given_records_in_two_books_when_queryLastUsedAssetId_then_returns_current_book_asset() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L))
        recordDao.addAsset(createAsset(id = 20L, booksId = 2L))
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 10L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 2L, assetId = 20L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isEqualTo(10L)
        assertThat(recordDao.queryLastUsedAssetId(2L)).isEqualTo(20L)
    }

    @Test
    fun given_latest_record_asset_deleted_when_queryLastUsedAssetId_then_falls_back_to_earlier_valid() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L))
        // 资产 99 不在 assets（已删）
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 10L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L, assetId = 99L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isEqualTo(10L)
    }

    @Test
    fun given_latest_record_asset_invisible_when_queryLastUsedAssetId_then_skips_to_visible() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L, invisible = SWITCH_INT_OFF))
        recordDao.addAsset(createAsset(id = 11L, booksId = 1L, invisible = SWITCH_INT_ON))
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 10L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L, assetId = 11L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isEqualTo(10L)
    }

    @Test
    fun given_latest_record_has_no_asset_when_queryLastUsedAssetId_then_skips_to_earlier_with_asset() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L))
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 10L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L, assetId = -1L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isEqualTo(10L)
    }

    @Test
    fun given_no_records_when_queryLastUsedAssetId_then_returns_null() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isNull()
    }

    @Test
    fun given_multiple_valid_records_when_queryLastUsedAssetId_then_returns_max_id_asset() = runTest {
        recordDao.addAsset(createAsset(id = 10L, booksId = 1L))
        recordDao.addAsset(createAsset(id = 20L, booksId = 1L))
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 10L))
        recordDao.addRecord(createRecord(id = 5L, booksId = 1L, assetId = 20L))
        recordDao.addRecord(createRecord(id = 3L, booksId = 1L, assetId = 10L))

        assertThat(recordDao.queryLastUsedAssetId(1L)).isEqualTo(20L)
    }
```

- [ ] **Step 5: 跑测试，确认因桩失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: FAIL——5 个期望非空 assetId 的用例失败（桩返回 null）；`given_no_records...` 用例通过。确认测试有判别性。

- [ ] **Step 6: 用忠实实现替换桩**

把 Step 3 的桩 `queryLastUsedAssetId` 替换为：

```kotlin
    override suspend fun queryLastUsedAssetId(booksId: Long): Long? {
        // 忠实复刻真实 SQL：asset_id IN（本账本可见在册资产）+ ORDER BY id DESC LIMIT 1
        val visibleAssetIds = assets
            .filter { it.booksId == booksId && it.invisible == SWITCH_INT_OFF }
            .mapNotNull { it.id }
            .toSet()
        return records
            .filter { it.booksId == booksId && it.assetId in visibleAssetIds }
            .maxByOrNull { it.id ?: Long.MIN_VALUE }
            ?.assetId
    }
```

- [ ] **Step 7: 跑测试，确认全部通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，6 个新用例全 PASS。

- [ ] **Step 8: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt
git commit -m "[feat|core:database|默认资产][公共]新增 queryLastUsedAssetId 按账本派生默认资产 + FakeRecordDao 忠实复刻与单测"
```

---

### Task 2: getDefaultRecord 改用 queryLastUsedAssetId + 移除 lastAssetId 死写 + proto legacy 注释

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（:379 改读、:100 删死写）
- Modify: `core/datastore-proto/src/main/proto/record_settings.proto`（:10 加 legacy 注释）

**Interfaces:**
- Consumes: `RecordDao.queryLastUsedAssetId(booksId: Long): Long?`（Task 1）

> 说明：`RecordRepositoryImpl` 因 `CombineProtoDataSource` 是 final 无法在 JVM 测试中实例化（见 `RecordRepositoryImplTest` 头部注释），本任务的行为正确性由 **Task 3 androidTest**（真 SQL）+ Task 1 忠实 Fake 单测共同保证；`getDefaultRecord` 的一行委托过于平凡、不单独建测。本任务自身以「编译 + 既有测试无回归」守护。

- [ ] **Step 1: 改 getDefaultRecord 默认资产取值**

`RecordRepositoryImpl.kt` `getDefaultRecord`（约 :372-389）内，把：

```kotlin
                assetId = appDataModel.lastAssetId,
```

改为：

```kotlin
                assetId = recordDao.queryLastUsedAssetId(appDataModel.currentBookId) ?: -1L,
```

- [ ] **Step 2: 移除 updateRecord 中的 lastAssetId 死写**

`RecordRepositoryImpl.kt` `updateRecord`（约 :82-101）末尾，删除这两行（含注释）：

```kotlin
        // 更新上次使用的默认数据
        combineProtoDataSource.updateLastAssetId(record.assetId)
```

（删除后 `updateRecord` 以 `assetDataVersion.updateVersion()` 结尾。`CombineProtoDataSource.updateLastAssetId` 方法、proto→model 映射、`splitAppPreferences` 拷贝均保留不动——仅搬运/持久化，不再被业务读取。）

- [ ] **Step 3: 给 proto 字段加 legacy 注释**

`record_settings.proto` 第 10 行：

```proto
  sint64 lastAssetId = 3; // 上一次使用资产 id
```

改为（保留字段与字段号，**不** reserve，因仍被 model 映射/迁移搬运）：

```proto
  sint64 lastAssetId = 3; // [legacy] 上一次使用资产 id；自 2026-06-26 起不再被 getDefaultRecord 读取，默认资产改由 RecordDao.queryLastUsedAssetId 按账本派生。保留字段以兼容存量数据与 splitAppPreferences 迁移，勿复用字段号
```

- [ ] **Step 4: 编译 core:data，确认无悬空引用**

Run: `./gradlew :core:data:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（移除死写后无编译错误；`updateLastAssetId` 仍有定义，仅生产侧不再调用）。

- [ ] **Step 5: 跑 core:data 全量单测，确认无回归**

Run: `./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，全绿（无既有测试断言被移除的写；`SettingRepositoryImplTest` 等仅断言 proto 默认值，不受影响）。

- [ ] **Step 6: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt \
        core/datastore-proto/src/main/proto/record_settings.proto
git commit -m "[fix|core:data|默认资产][公共]getDefaultRecord 改按账本派生默认资产，移除全局 lastAssetId 死写"
```

---

### Task 3: RecordDaoTest androidTest —— 真 SQL 语义守护

**Files:**
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`（加 1 个 region / 6 个用例，复用既有 `createAsset`/`createRecord`/`insertRecord`/`createTestBook` helper）

**Interfaces:**
- Consumes: `RecordDao.queryLastUsedAssetId(booksId: Long): Long?`（Task 1）

> 说明：这是新查询**唯一可信的真 SQL 验证层**（device-gated）。本机受代理 TLS 影响 `connectedDebugAndroidTest` 可能跑不动（环境问题），CI 必跑；若本机无法跑，提交后在 PR/CI 验证。

- [ ] **Step 1: 写 androidTest 用例**

在 `RecordDaoTest.kt` 末尾（类闭合 `}` 之前、最后一个 region 之后）加。`assetDao.insert` 返回 Unit，故资产用**显式 id**插入（Room @Insert 对 autoGenerate 主键传非空 id 时按该 id 落库）；记录用 `insertRecord` 按插入顺序自增 id（后插入者 id 更大 = 更近）：

```kotlin
    // region queryLastUsedAssetId（任务1：默认资产按账本隔离）

    @Test
    fun queryLastUsedAssetId_isolatesByBook() = runTest {
        val book1 = createTestBook()
        val book2 = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book1, name = "卡A"))
        assetDao.insert(createAsset(id = 20L, booksId = book2, name = "卡B"))
        insertRecord(createRecord(booksId = book1, assetId = 10L))
        insertRecord(createRecord(booksId = book2, assetId = 20L))

        assertThat(recordDao.queryLastUsedAssetId(book1)).isEqualTo(10L)
        assertThat(recordDao.queryLastUsedAssetId(book2)).isEqualTo(20L)
    }

    @Test
    fun queryLastUsedAssetId_skipsDeletedAsset() = runTest {
        val book = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book, name = "卡A"))
        // 资产 99 不入库（已删）
        insertRecord(createRecord(booksId = book, assetId = 10L))
        insertRecord(createRecord(booksId = book, assetId = 99L)) // 后插入 = 更近，但资产已删

        assertThat(recordDao.queryLastUsedAssetId(book)).isEqualTo(10L)
    }

    @Test
    fun queryLastUsedAssetId_skipsInvisibleAsset() = runTest {
        val book = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book, name = "可见", invisible = SWITCH_INT_OFF))
        assetDao.insert(createAsset(id = 11L, booksId = book, name = "隐藏", invisible = SWITCH_INT_ON))
        insertRecord(createRecord(booksId = book, assetId = 10L))
        insertRecord(createRecord(booksId = book, assetId = 11L)) // 后插入 = 更近，但隐藏

        assertThat(recordDao.queryLastUsedAssetId(book)).isEqualTo(10L)
    }

    @Test
    fun queryLastUsedAssetId_skipsNoAssetRecord() = runTest {
        val book = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book, name = "卡A"))
        insertRecord(createRecord(booksId = book, assetId = 10L))
        insertRecord(createRecord(booksId = book, assetId = -1L)) // 后插入 = 更近，但无资产

        assertThat(recordDao.queryLastUsedAssetId(book)).isEqualTo(10L)
    }

    @Test
    fun queryLastUsedAssetId_returnsNullWhenNoRecord() = runTest {
        val book = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book, name = "卡A"))

        assertThat(recordDao.queryLastUsedAssetId(book)).isNull()
    }

    @Test
    fun queryLastUsedAssetId_returnsMostRecentlyInserted() = runTest {
        val book = createTestBook()
        assetDao.insert(createAsset(id = 10L, booksId = book, name = "卡A"))
        assetDao.insert(createAsset(id = 20L, booksId = book, name = "卡B"))
        insertRecord(createRecord(booksId = book, assetId = 10L))
        insertRecord(createRecord(booksId = book, assetId = 20L)) // 最后插入 = id 最大
        insertRecord(createRecord(booksId = book, assetId = -1L)) // 更晚但无资产，应被跳过

        assertThat(recordDao.queryLastUsedAssetId(book)).isEqualTo(20L)
    }

    // endregion
```

- [ ] **Step 2: 跑 androidTest（需模拟器/真机）**

Run: `./gradlew :core:database:connectedDebugAndroidTest --tests "*RecordDaoTest"`（首次需联网拉 UTP，按 Global Constraints 处理代理；勿加 `--offline`）
Expected: 6 个新用例 PASS。若本机环境（代理 TLS）无法完成 instrumented 运行，记录为环境受阻、转 CI 验证，**不**因此判代码失败。

- [ ] **Step 3: 提交**

```bash
git add core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt
git commit -m "[test|core:database|默认资产][公共]RecordDaoTest 补 queryLastUsedAssetId 真 SQL 语义用例"
```

---

### Task 4（可选，低风险）: 共享 `_defaultRecordData` 冷流，消除每次开记账页 3× 查询

> **可选**：spec 标注「默认采纳、用户可否决」。`getDefaultRecord` 由内存读变 DB 查询后，`EditRecordViewModel._defaultRecordData` 为未共享冷流、被 3 处下游独立收集 → 每开记账页跑 3×。本任务用 `shareIn(replay=1)` 让三处共享一次计算（去重 + 去抖）。属行为保持型优化，验证 = 既有 `EditRecordViewModelTest` 全绿（无新测，收益为结构性单次收集）。若用户否决则跳过本 Task。

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/EditRecordViewModel.kt`（`_defaultRecordData` 约 :105-107）

- [ ] **Step 1: 加 shareIn import**

`EditRecordViewModel.kt` import 区加（`SharingStarted` 已 import，:58）：

```kotlin
import kotlinx.coroutines.flow.shareIn
```

- [ ] **Step 2: 给 _defaultRecordData 加 shareIn**

把（约 :105-107）：

```kotlin
    private val _defaultRecordData = _recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }
```

改为：

```kotlin
    private val _defaultRecordData = _recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )
```

- [ ] **Step 3: 编译 feature:records**

Run: `./gradlew :feature:records:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 跑 feature:records 单测，确认无回归**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*EditRecordViewModelTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，全绿（uiState/defaultTypeId/selectedTypeCategory 行为不变；`.first()` 经 replay=1 缓存正常取值）。

- [ ] **Step 5: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/EditRecordViewModel.kt
git commit -m "[perf|feature:records|默认资产][公共]共享 _defaultRecordData 冷流，消除开记账页 3× 默认记录查询"
```

---

## 收尾（实现完成后，进入节点 2）

四个 Task 完成、本地可跑的测试通过后，按 CLAUDE.md「Agent Team 评审·节点 2」对本次 `git diff` 调 `comprehensive-review:full-review`（改动 < ~50 行且不涉安全/接口/破坏性时可降级两维快审；本任务跨 core:database/core:data/feature:records 含查询+ViewModel 改动，建议跑满或至少 code-reviewer + architect-review 两维）。blocking（Critical/High）修复后再交付。

## Self-Review（plan vs spec 覆盖核对）

- ✅ spec「新增 DAO 查询（含 invisible 过滤）」→ Task 1 Step 1。
- ✅ spec「getDefaultRecord 改读」→ Task 2 Step 1。
- ✅ spec「移除死写 + proto legacy」→ Task 2 Step 2/3。
- ✅ spec「FakeRecordDao 加 assets + 忠实实现」→ Task 1 Step 3/6。
- ✅ spec「语义：最近创建 / 跳过无资产 / 跳过隐藏 / 跨账本隔离 / 空→null」→ Task 1 6 用例 + Task 3 6 用例。
- ✅ spec「androidTest 真 SQL」→ Task 3。
- ✅ spec「可选 shareIn」→ Task 4（可选）。
- ✅ spec「无 schema 变更」→ Global Constraints + 无 migration 步骤。
- ✅ spec「FakeRecordRepository 保持 -1L 不动」→ 未触及，符合。
- 类型一致性：`queryLastUsedAssetId(booksId: Long): Long?` 在 Task 1/2/3 全程一致；`addAsset`/`assets` 在 Task 1 内自洽。
- 占位符扫描：无 TBD/TODO；每个 code step 均有完整代码与期望输出。
