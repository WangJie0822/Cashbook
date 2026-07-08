# 首页记录列表加载性能优化 实现计划（R6a：Room `@Relation` 分页）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Room `@Transaction`+`@Relation` 分页替换首页列表的「Paging 逐元素单条转换」，消除 N+1，并继承 Room 生成 PagingSource 的保位/自动 invalidate。

**Architecture:** 新增 `@Relation` POJO（一次分页把 type/asset/tags/images/双向 relatedRecord 批量 IN 查询物化）→ Repository 层 map 成 `RecordViewsModel` → ViewModel 层 `asEntity()` + `insertSeparators`。Room 生成 `LimitOffsetPagingSource`（免手写 `getRefreshKey`/`InvalidationTracker`）。

**Tech Stack:** Kotlin, Room 2.7.1 + room-paging, Paging 3.5.0, Hilt, JUnit4 + Truth, Robolectric（androidTest）。

## Global Constraints

- 金额全链路 `Long`、单位分；**不改金额口径**（finalAmount/recordAmount/analyticsPie 三口径）。
- mapper **必须复用**现有 `asModel()` 转换 + 提取的 `sumRelatedAmount`/`computeRelatedNature`，禁另写金额/性质口径。
- 所有列表/分页 DAO 查询 WHERE **必须含 `books_id=:booksId`**（账本隔离）。
- `insertSeparators` **保留在 ViewModel 层**，禁移进 `PagingSource.load()`（跨页重复 DayHeader → LazyColumn 重复 key 崩溃）。
- DAO 新增抽象方法后 `FakeRecordDao`（core:data test）必须同步实现；接口返回类型变更后 `FakeRecordRepository`（core:testing）必须同步；桩须忠实复刻语义，禁 `emptyList()`/宽松桩。
- 平账合成类型 typeId：`RECORD_TYPE_BALANCE_EXPENDITURE.id = -1101L`、`RECORD_TYPE_BALANCE_INCOME.id = -1102L`；`needRelated = typeId ∈ {FIXED_TYPE_ID_REFUND(-2001L), FIXED_TYPE_ID_REIMBURSE(-2002L)}`。
- 本机构建命令统一加 `--offline --no-daemon --console=plain`；模块测试任务名按模块类型：core:model（jvm）用 `:core:model:test`，core:data/core:domain/feature（android library）用 `:<module>:testDebugUnitTest`，androidTest 用 `:core:database:connectedDebugAndroidTest`。
- 判构建结果只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`。
- **spec 勘误**：spec §4.2 写「纯函数提到 core:model」，因 core:model 是零依赖 jvm library、无法访问 core:common 的 `FIXED_TYPE_ID_*`，本计划修正为**提到 core:data**（Task 1）。

---

## 文件结构总览

| 模块 | 文件 | 责任 |
|---|---|---|
| core:data | `repository/RecordViewsCalc.kt`（新增） | public `sumRelatedAmount`/`computeRelatedNature` 纯函数（从 core:domain 迁出，两处共用） |
| core:domain | `usecase/RecordModelTransToViewsUseCase.kt`（改） | 删 private 副本，改调 core:data public 版本 |
| core:database | `relation/LauncherRecordViewRelation.kt`（新增） | `@Relation` POJO |
| core:database | `dao/RecordDao.kt`（改） | `@Transaction @Query` 分页方法返回 `PagingSource<Int, LauncherRecordViewRelation>` |
| core:data | `repository/impl/RecordViewsRelationMapper.kt`（新增） | `LauncherRecordViewRelation.toRecordViewsModel()` |
| core:data | `repository/RecordRepository.kt` + `impl/RecordRepositoryImpl.kt`（改） | `getRecordPagingData` 返回 `Flow<PagingData<RecordViewsModel>>` |
| core:data test | `testdoubles/FakeRecordDao.kt`（改） | 补 `pagingLauncherRecordViews` 桩 |
| core:testing | `repository/FakeRecordRepository.kt`（改） | `getRecordPagingData` 返回类型同步 |
| feature:records | `viewmodel/LauncherContentViewModel.kt`（改） | `recordPagingData` 改用 Repository RecordViewsModel 流；移除 `recordModelTransToViewsUseCase` 参数 |
| feature:records test | `viewmodel/LauncherContentViewModelTest.kt`（改） | 9 处构造去参 |
| feature:records | `screen/LauncherContentScreen.kt`（改） | LazyColumn 补 `contentType` |
| core:database androidTest | `dao/RecordDaoRelationTest.kt`（新增） | `@Relation` 加载正确性真机测试 |

---

### Task 1: 提取 `sumRelatedAmount`/`computeRelatedNature` 到 core:data 共用

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalc.kt`
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt`（删 `:193-227` 两个 private fun，改调 core:data 版本）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalcTest.kt`

**Interfaces:**
- Produces:
  - `fun sumRelatedAmount(typeCategory: RecordTypeCategoryEnum, relatedRecord: List<RecordModel>): Long`
  - `fun computeRelatedNature(typeCategory: RecordTypeCategoryEnum, relatedRecord: List<RecordModel>): RecordRelatedNatureEnum`
  （包 `cn.wj.android.cashbook.core.data.repository`，public，供 core:data mapper 与 core:domain transBatch 共用）

- [ ] **Step 1: 写失败测试**

`core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalcTest.kt`：
```kotlin
/*
 * Copyright 2021 The Cashbook Open Source Project
 * Licensed under the Apache License, Version 2.0 ...
 */
package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordViewsCalcTest {

    private fun record(typeId: Long, amount: Long, charges: Long = 0L, concessions: Long = 0L) =
        RecordModel(
            id = 1L, booksId = 1L, typeId = typeId, assetId = -1L, relatedAssetId = -1L,
            amount = amount, finalAmount = amount, charges = charges, concessions = concessions,
            remark = "", reimbursable = false, recordTime = 0L, reimbursed = false,
        )

    @Test
    fun sumRelatedAmount_expenditure_sums_income_side() {
        // 主支出 → 关联收入口径 recordAmount(INCOME)=amount-charges
        val related = listOf(record(FIXED_TYPE_ID_REIMBURSE, amount = 1000L, charges = 100L))
        val sum = sumRelatedAmount(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(sum).isEqualTo(900L)
    }

    @Test
    fun sumRelatedAmount_transfer_returns_zero() {
        val sum = sumRelatedAmount(RecordTypeCategoryEnum.TRANSFER, listOf(record(1L, 500L)))
        assertThat(sum).isEqualTo(0L)
    }

    @Test
    fun computeRelatedNature_all_reimburse_is_reimbursed() {
        val related = listOf(record(FIXED_TYPE_ID_REIMBURSE, 1000L))
        val nature = computeRelatedNature(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
    }

    @Test
    fun computeRelatedNature_mixed_is_mixed() {
        val related = listOf(
            record(FIXED_TYPE_ID_REIMBURSE, 1000L),
            record(FIXED_TYPE_ID_REFUND, 1000L),
        )
        val nature = computeRelatedNature(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.MIXED)
    }

    @Test
    fun computeRelatedNature_non_expenditure_is_none() {
        val nature = computeRelatedNature(RecordTypeCategoryEnum.INCOME, listOf(record(1L, 1000L)))
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordViewsCalcTest" --offline --no-daemon --console=plain`
Expected: FAIL（`sumRelatedAmount`/`computeRelatedNature` unresolved reference）

- [ ] **Step 3: 实现 core:data 纯函数**

`core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalc.kt`：
```kotlin
/*
 * Copyright 2021 The Cashbook Open Source Project
 * Licensed under the Apache License, Version 2.0 ...
 */
package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.recordAmount

/**
 * 计算关联金额（单条/批量/分页三条转换路径共用，保证口径一致）。
 * 关联 category 由主 category 取反推断（零查询）。从 RecordModelTransToViewsUseCase 迁出。
 */
fun sumRelatedAmount(
    typeCategory: RecordTypeCategoryEnum,
    relatedRecord: List<RecordModel>,
): Long {
    val relatedCategory = when (typeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> RecordTypeCategoryEnum.INCOME
        RecordTypeCategoryEnum.INCOME -> RecordTypeCategoryEnum.EXPENDITURE
        else -> return 0L
    }
    return relatedRecord.sumOf { record ->
        recordAmount(relatedCategory, record.amount, record.charges, record.concessions)
    }
}

/**
 * 计算被吸收支出的关联性质（在已物化 relatedRecord 上判定，零查询）。从 RecordModelTransToViewsUseCase 迁出。
 */
fun computeRelatedNature(
    typeCategory: RecordTypeCategoryEnum,
    relatedRecord: List<RecordModel>,
): RecordRelatedNatureEnum {
    if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE || relatedRecord.isEmpty()) {
        return RecordRelatedNatureEnum.NONE
    }
    val allReimburse = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REIMBURSE }
    val allRefund = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REFUND }
    return when {
        allReimburse -> RecordRelatedNatureEnum.REIMBURSED
        allRefund -> RecordRelatedNatureEnum.REFUNDED
        else -> RecordRelatedNatureEnum.MIXED
    }
}
```

- [ ] **Step 4: 改 core:domain 调用新函数（删 private 副本）**

在 `RecordModelTransToViewsUseCase.kt`：
1. 删除 `:193-205` 的 `private fun sumRelatedAmount(...)` 与 `:213-227` 的 `private fun computeRelatedNature(...)`。
2. 顶部 import 加：
```kotlin
import cn.wj.android.cashbook.core.data.repository.sumRelatedAmount
import cn.wj.android.cashbook.core.data.repository.computeRelatedNature
```
（原文件内对 `sumRelatedAmount(...)`/`computeRelatedNature(...)` 的调用点 `:68/85/162/179` 不变，现在解析到 import 的 core:data 版本。）

- [ ] **Step 5: 运行 core:data + core:domain 测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordViewsCalcTest" :core:domain:testDebugUnitTest --tests "*RecordModelTransToViewsUseCaseTest" --offline --no-daemon --console=plain`
Expected: 两模块 PASS（core:domain 现有等价性测试 `batch_produces_field_equivalent_result_to_single` 回归通过，证明迁出无行为变化）

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalc.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/RecordViewsCalcTest.kt \
        core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt
git commit -m "[refactor|core:data·core:domain|金额计算][公共]提取 sumRelatedAmount/computeRelatedNature 到 core:data 共用（首页 R6a 分页 mapper 复用）"
```

---

### Task 2: `@Relation` POJO + DAO `@Transaction @Query` 分页方法

**Files:**
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/LauncherRecordViewRelation.kt`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（加抽象方法）

**Interfaces:**
- Produces:
  - `data class LauncherRecordViewRelation(record, types, assets, intoAssets, images, tags, relatedAsRecordId, relatedAsRelatedId)`（各 `@Relation` 均为 `List`）
  - `fun RecordDao.pagingLauncherRecordViews(booksId: Long, startDate: Long, endDate: Long): PagingSource<Int, LauncherRecordViewRelation>`

- [ ] **Step 1: 创建 `@Relation` POJO**

`core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/LauncherRecordViewRelation.kt`：
```kotlin
/*
 * Copyright 2021 The Cashbook Open Source Project
 * Licensed under the Apache License, Version 2.0 ...
 */
package cn.wj.android.cashbook.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable

/**
 * 首页记录列表分页的 @Relation 视图：一次分页把 type/asset/tags/images/双向 relatedRecord 批量 IN 物化，消 N+1。
 * Room 为 @Transaction 方法生成 LimitOffsetPagingSource（保位）+ 自动观察全部 @Relation 表（自动 invalidate）。
 * type/asset 一对一（Room @Relation 恒为集合，映射时取 firstOrNull）；平账 typeId 负、db_type 无匹配 → types 空 List（不丢主记录）。
 */
data class LauncherRecordViewRelation(
    @Embedded val record: RecordTable,

    @Relation(parentColumn = "type_id", entityColumn = "id", entity = TypeTable::class)
    val types: List<TypeTable>,

    @Relation(parentColumn = "asset_id", entityColumn = "id", entity = AssetTable::class)
    val assets: List<AssetTable>,

    @Relation(parentColumn = "into_asset_id", entityColumn = "id", entity = AssetTable::class)
    val intoAssets: List<AssetTable>,

    @Relation(parentColumn = "id", entityColumn = "record_id", entity = ImageWithRelatedTable::class)
    val images: List<ImageWithRelatedTable>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TagWithRecordTable::class,
            parentColumn = "record_id",
            entityColumn = "tag_id",
        ),
        entity = TagTable::class,
    )
    val tags: List<TagTable>,

    // 收入侧：我作为 record_id 关联的 related_record（被吸收支出）
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordWithRelatedTable::class,
            parentColumn = "record_id",
            entityColumn = "related_record_id",
        ),
        entity = RecordTable::class,
    )
    val relatedAsRecordId: List<RecordTable>,

    // 支出侧：我作为 related_record_id 时吸收我的 record（收入）
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordWithRelatedTable::class,
            parentColumn = "related_record_id",
            entityColumn = "record_id",
        ),
        entity = RecordTable::class,
    )
    val relatedAsRelatedId: List<RecordTable>,
)
```

- [ ] **Step 2: 在 `RecordDao.kt` 加 `@Transaction @Query` 方法**

顶部 import 加：
```kotlin
import androidx.room.Transaction
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
```
在 `pagingQueryByBooksIdBetweenDate`（`:85-89`）之后加：
```kotlin
    /** 首页分页：一次 @Transaction+@Relation 物化关联，消 N+1；Room 生成 LimitOffsetPagingSource（保位+自动 invalidate） */
    @Transaction
    @Query(
        value = """
            SELECT * FROM db_record
            WHERE record_time>=:startDate
            AND record_time<:endDate
            AND books_id=:booksId
            ORDER BY record_time DESC
        """,
    )
    fun pagingLauncherRecordViews(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): PagingSource<Int, LauncherRecordViewRelation>
```

- [ ] **Step 3: 编译 core:database 触发 KSP 生成**

Run: `./gradlew :core:database:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`；`:core:database:kspDebugKotlin` 执行、无 `e:`/Room 错误。

- [ ] **Step 4: 核验 Room 生成的实现（两个 gate + 双向）**

Run:
```bash
GEN=$(find core/database/build/generated -name 'RecordDao_Impl.kt' | head -1)
grep -nE 'pagingLauncherRecordViews|LimitOffsetPagingSource' "$GEN"
grep -oE '"db_[a-z_]+"' "$GEN" | sort -u
grep -nE 'related_record_id = `db_record`.`id`|record_id = `db_record`.`id`' "$GEN"
```
Expected：方法生成；PagingSource 为 `LimitOffsetPagingSource`；表集含 `db_record/db_type/db_asset/db_tag/db_tag_with_record/db_image_with_related/db_record_with_related`；双向 JOIN 两条不同方向 SQL。

- [ ] **Step 5: Commit**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/LauncherRecordViewRelation.kt \
        core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt
git commit -m "[feat|core:database|首页分页][公共]新增 @Relation POJO + @Transaction 分页 DAO（Room 生成 LimitOffsetPagingSource 消 N+1）"
```

---

### Task 3: POJO → `RecordViewsModel` mapper

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordViewsRelationMapper.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordViewsRelationMapperTest.kt`

**Interfaces:**
- Consumes: Task 1 `sumRelatedAmount`/`computeRelatedNature`；Task 2 `LauncherRecordViewRelation`；现有 `internal fun TypeTable/AssetTable/TagTable/RecordTable/ImageWithRelatedTable.asModel()`
- Produces: `internal fun LauncherRecordViewRelation.toRecordViewsModel(): RecordViewsModel`

- [ ] **Step 1: 写失败测试**

`RecordViewsRelationMapperTest.kt`（直接构造 POJO，断言逐字段）：
```kotlin
/* Copyright ... Apache 2.0 ... */
package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable // 若未直接用可删
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordViewsRelationMapperTest {

    private fun recordTable(id: Long, typeId: Long, amount: Long = 1000L) = RecordTable(
        id = id, typeId = typeId, assetId = 10L, intoAssetId = -1L, booksId = 1L,
        amount = amount, finalAmount = amount, concessions = 0L, charge = 0L,
        remark = "r", reimbursable = 0, recordTime = 100L, reimbursed = 0,
    )

    private fun typeTable(id: Long, category: Int) = TypeTable(
        id = id, parentId = -1L, name = "餐饮", iconName = "ic", typeLevel = 0,
        typeCategory = category, protected = 0, sort = 0,
    )

    private fun assetTable(id: Long) = AssetTable(
        id = id, booksId = 1L, name = "现金", balance = 0L, totalAmount = 0L,
        billingDate = "", repaymentDate = "", type = 0, classification = 0,
        invisible = 0, openBank = "", cardNo = "", remark = "", sort = 0, modifyTime = 0L,
    )

    @Test
    fun normal_expenditure_maps_all_fields() {
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 1L, typeId = 5L),
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = listOf(assetTable(10L)),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = listOf(TagTable(id = 3L, name = "旅行", booksId = 1L, invisible = 0)),
            relatedAsRecordId = emptyList(),
            relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.id).isEqualTo(1L)
        assertThat(m.type.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
        assertThat(m.asset?.id).isEqualTo(10L)
        assertThat(m.relatedAsset).isNull()
        assertThat(m.relatedTags.map { it.id }).containsExactly(3L)
        assertThat(m.relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
        assertThat(m.relatedAmount).isEqualTo(0L)
    }

    @Test
    fun balance_type_empty_types_resolves_synthetic() {
        // 平账支出 typeId=-1101 → db_type 无匹配 → types 空 → 合成类型
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 2L, typeId = RECORD_TYPE_BALANCE_EXPENDITURE.id),
            types = emptyList(),
            assets = emptyList(), intoAssets = emptyList(), images = emptyList(), tags = emptyList(),
            relatedAsRecordId = emptyList(), relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.type.id).isEqualTo(RECORD_TYPE_BALANCE_EXPENDITURE.id)
        assertThat(m.isBalanceRecord).isTrue()
    }

    @Test
    fun expenditure_picks_relatedAsRelatedId_side() {
        // 主支出 → relatedRecord 取 relatedAsRelatedId（吸收它的收入）
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 4L, typeId = 5L),
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = emptyList(), intoAssets = emptyList(), images = emptyList(), tags = emptyList(),
            relatedAsRecordId = listOf(recordTable(id = 99L, typeId = 5L)), // 不应被选
            relatedAsRelatedId = listOf(recordTable(id = 7L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 1000L)),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.relatedRecord.map { it.id }).containsExactly(7L)
        assertThat(m.relatedNature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
        assertThat(m.relatedAmount).isEqualTo(1000L) // recordAmount(INCOME)=amount-charges=1000
    }
}
```

> 注：`TypeTable`/`AssetTable`/`TagTable` 构造参数以各自 `@Entity` 定义为准（实现时按实际字段顺序/名对齐；若构造签名与上不符，以真实 Table 定义为准修正测试数据）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordViewsRelationMapperTest" --offline --no-daemon --console=plain`
Expected: FAIL（`toRecordViewsModel` unresolved）

- [ ] **Step 3: 实现 mapper**

`RecordViewsRelationMapper.kt`：
```kotlin
/* Copyright ... Apache 2.0 ... */
package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.data.repository.asModel      // RecordTable.asModel / ImageWithRelatedTable.asModel (RecordRepository.kt)
import cn.wj.android.cashbook.core.data.repository.computeRelatedNature
import cn.wj.android.cashbook.core.data.repository.sumRelatedAmount
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordViewsModel

/**
 * 把 @Relation 分页视图组装为 RecordViewsModel，与单条 RecordModelTransToViewsUseCase.invoke 逐字段等价。
 * 所有关联已由 Room @Relation 批量物化，此处仅内存组装（零查询）。
 */
internal fun LauncherRecordViewRelation.toRecordViewsModel(): RecordViewsModel {
    val typeTable = types.firstOrNull()
    val type = if (typeTable != null) {
        val id = typeTable.id ?: -1L
        typeTable.asModel(needRelated = id == FIXED_TYPE_ID_REFUND || id == FIXED_TYPE_ID_REIMBURSE)
    } else {
        // 平账合成类型（typeId 负、db_type 无匹配 → @Relation 空 List）
        when (record.typeId) {
            RECORD_TYPE_BALANCE_INCOME.id -> RECORD_TYPE_BALANCE_INCOME
            else -> RECORD_TYPE_BALANCE_EXPENDITURE
        }
    }
    val relatedTables = if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
        relatedAsRecordId
    } else {
        relatedAsRelatedId
    }
    val relatedRecords = relatedTables.map { it.asModel() }
    return RecordViewsModel(
        id = record.id ?: -1L,
        booksId = record.booksId,
        type = type,
        asset = assets.firstOrNull()?.asModel(),
        relatedAsset = intoAssets.firstOrNull()?.asModel(),
        amount = record.amount,
        finalAmount = record.finalAmount,
        charges = record.charge,
        concessions = record.concessions,
        remark = record.remark,
        reimbursable = record.reimbursable == SWITCH_INT_ON,
        relatedTags = tags.map { it.asModel() },
        relatedImage = images.map { it.asModel() },
        relatedRecord = relatedRecords,
        relatedAmount = sumRelatedAmount(type.typeCategory, relatedRecords),
        relatedNature = computeRelatedNature(type.typeCategory, relatedRecords),
        recordTime = record.recordTime,
        reimbursed = record.reimbursed == SWITCH_INT_ON,
    )
}
```

> `TypeTable.asModel`/`AssetTable.asModel`/`TagTable.asModel` 定义在 core:data 的 `repository/TypeRepository.kt`/`AssetRepository.kt`/`TagRepository.kt`（internal，同模块可见）；`RecordTable.asModel`/`ImageWithRelatedTable.asModel` 在 `repository/RecordRepository.kt`。同模块 internal 无需额外 import 声明（同包）或按实际包 import。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordViewsRelationMapperTest" --offline --no-daemon --console=plain`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordViewsRelationMapper.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordViewsRelationMapperTest.kt
git commit -m "[feat|core:data|首页分页][公共]新增 @Relation POJO→RecordViewsModel mapper（复用 asModel + 提取的金额/性质口径）"
```

---

### Task 4: Repository `getRecordPagingData` 改造 + 接口 + Fakes

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt:130-131`（接口返回类型）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt:354-375`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（补新方法）
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt:286-291`

**Interfaces:**
- Produces: `fun RecordRepository.getRecordPagingData(startDate: Long, endDate: Long): Flow<PagingData<RecordViewsModel>>`（返回类型由 `RecordModel` 改为 `RecordViewsModel`）

- [ ] **Step 1: 改接口返回类型**

`RecordRepository.kt`：把 `:131`
```kotlin
    fun getRecordPagingData(startDate: Long, endDate: Long): Flow<PagingData<RecordModel>>
```
改为
```kotlin
    fun getRecordPagingData(startDate: Long, endDate: Long): Flow<PagingData<RecordViewsModel>>
```
并确保 import `cn.wj.android.cashbook.core.model.model.RecordViewsModel`。

- [ ] **Step 2: 改 `RecordRepositoryImpl.getRecordPagingData`**

替换 `:354-375` 实现体：
```kotlin
    override fun getRecordPagingData(
        startDate: Long,
        endDate: Long,
    ): Flow<PagingData<RecordViewsModel>> {
        // 用 @Transaction+@Relation 分页：Room 生成 LimitOffsetPagingSource 对全部关联表自动 invalidate（保位、不回顶）。
        // 勿改回手动 combine recordDataVersion，否则每次增删改重建 Pager 回顶。
        return combineProtoDataSource.recordSettingsData
            .flatMapLatest { settings ->
                Pager(
                    config = PagingConfig(pageSize = 20),
                    pagingSourceFactory = {
                        recordDao.pagingLauncherRecordViews(
                            booksId = settings.currentBookId,
                            startDate = startDate,
                            endDate = endDate,
                        )
                    },
                ).flow.map { pagingData ->
                    pagingData.map { it.toRecordViewsModel() }
                }
            }
    }
```
import 加：`cn.wj.android.cashbook.core.data.repository.impl.toRecordViewsModel`（同包则免）、`cn.wj.android.cashbook.core.model.model.RecordViewsModel`。

- [ ] **Step 3: 补 `FakeRecordDao.pagingLauncherRecordViews`（忠实复刻）**

在 FakeRecordDao 加（组装 POJO from 内存 `records`/`types`/`assets`/`tagWithRecords`/`images`/`relatedRecords`，忠实复刻 @Relation 语义）：
```kotlin
    override fun pagingLauncherRecordViews(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): PagingSource<Int, LauncherRecordViewRelation> {
        return object : PagingSource<Int, LauncherRecordViewRelation>() {
            override suspend fun load(
                params: LoadParams<Int>,
            ): LoadResult<Int, LauncherRecordViewRelation> {
                val page = records.filter {
                    it.booksId == booksId && it.recordTime >= startDate && it.recordTime < endDate
                }.sortedByDescending { it.recordTime }.map { rec ->
                    val recId = rec.id ?: -1L
                    LauncherRecordViewRelation(
                        record = rec,
                        types = types.mapNotNull { it.table }.filter { it.id == rec.typeId },
                        assets = assets.filter { it.id == rec.assetId },
                        intoAssets = assets.filter { it.id == rec.intoAssetId },
                        images = images.filter { it.recordId == recId },
                        tags = tagWithRecords.filter { it.recordId == recId }.mapNotNull { it.tag },
                        relatedAsRecordId = relatedRecords.filter { it.recordId == recId }
                            .mapNotNull { link -> records.firstOrNull { it.id == link.relatedRecordId } },
                        relatedAsRelatedId = relatedRecords.filter { it.relatedRecordId == recId }
                            .mapNotNull { link -> records.firstOrNull { it.id == link.recordId } },
                    )
                }
                return LoadResult.Page(data = page, prevKey = null, nextKey = null)
            }

            override fun getRefreshKey(state: PagingState<Int, LauncherRecordViewRelation>): Int? = null
        }
    }
```
> 注：`FakeTypeEntry`/`FakeTagWithRecordEntry` 若未持有底层 `TypeTable`/`TagTable`，本步骤需先为其补一个 `table`/`tag` 字段（忠实复刻所需）。若成本过高，可退化为返回 `records` 层正确、关联空的 POJO 并**在方法上加注释「@Relation 关联语义由 core:database androidTest（Task 7）真库验证」**——符合 CLAUDE.md「测试替身：级联/关系真实覆盖在 instrumented 层」。实现者按 FakeRecordDao 现有内存结构择一，不得用 `emptyList()` 冒充记录层。

import 加：`LauncherRecordViewRelation`、`PagingState`（若未 import）。

- [ ] **Step 4: 改 `FakeRecordRepository.getRecordPagingData` 返回类型**

`FakeRecordRepository.kt:286-291`：FakeRecordRepository 内存持 `RecordModel`，但接口现返回 `RecordViewsModel`。改为持有/返回 `RecordViewsModel` 的桩——最小改动：新增一个内存列表 `val recordViews = mutableListOf<RecordViewsModel>()`，方法返回 `flowOf(PagingData.from(recordViews.toList()))`：
```kotlin
    /** 首页分页视图桩（测试按需 recordViews.add(...) 注入） */
    val recordViews = mutableListOf<RecordViewsModel>()

    override fun getRecordPagingData(
        startDate: Long,
        endDate: Long,
    ): Flow<PagingData<RecordViewsModel>> {
        return flowOf(PagingData.from(recordViews.toList()))
    }
```
import 加：`cn.wj.android.cashbook.core.model.model.RecordViewsModel`。

- [ ] **Step 5: 编译 core:data 主源集 + 两个 test 源集**

Run: `./gradlew :core:data:compileDebugKotlin :core:data:compileDebugUnitTestKotlin :core:testing:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（验证接口/Fake 全部对齐、无 "not abstract" 报错）

- [ ] **Step 6: 运行 core:data 全测试回归**

Run: `./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt \
        core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt \
        core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt
git commit -m "[feat|core:data·core:testing|首页分页][公共]getRecordPagingData 改用 @Relation 分页返回 RecordViewsModel（消 N+1），同步 Fake 替身"
```

---

### Task 5: `LauncherContentViewModel` 改用新分页流

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt`（`:186-202` recordPagingData；`:62-67` 构造去 `recordModelTransToViewsUseCase`）
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt`（9 处构造：75,102,132,162,192,222,259,294,618）

**Interfaces:**
- Consumes: Task 4 `recordRepository.getRecordPagingData(...): Flow<PagingData<RecordViewsModel>>`

- [ ] **Step 1: 改 `recordPagingData`（去逐条转换）**

`LauncherContentViewModel.kt` 替换 `:186-202`：
```kotlin
    /** 分页记录数据 */
    val recordPagingData: Flow<PagingData<LauncherListItem>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection.toDateRange(d) }
            .flatMapLatest { (startDate, endDate) ->
                recordRepository.getRecordPagingData(startDate, endDate)
                    .map { pagingData ->
                        pagingData.map { viewsModel ->
                            LauncherListItem.Record(viewsModel.asEntity()) as LauncherListItem
                        }
                    }
                    .map { pagingData ->
                        pagingData.insertSeparators { before, after ->
                            recordDaySeparator(before, after)
                        }
                    }
            }.cachedIn(viewModelScope)
```
import：加 `cn.wj.android.cashbook.core.model.transfer.asEntity`（若未 import）；删不再使用的 `RecordModelTransToViewsUseCase` import 与 `recordModelTransToViewsUseCase` 相关 import（`asEntity` 现作用于 RecordViewsModel）。

- [ ] **Step 2: 从构造移除 `recordModelTransToViewsUseCase`**

`:62-67` 构造改为：
```kotlin
class LauncherContentViewModel @Inject constructor(
    booksRepository: BooksRepository,
    settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
) : ViewModel() {
```
删除 `private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,` 参数及其 import。

- [ ] **Step 3: 改 9 处测试构造**

`LauncherContentViewModelTest.kt`：把每处
```kotlin
        val recordModelTransToViewsUseCase = RecordModelTransToViewsUseCase(...)
        viewModel = LauncherContentViewModel(
            booksRepository = ...,
            settingRepository = ...,
            recordRepository = ...,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
        )
```
改为（删 useCase 构造与该命名参数；9 处 75/102/132/162/192/222/259/294/618 同款）：
```kotlin
        viewModel = LauncherContentViewModel(
            booksRepository = ...,
            settingRepository = ...,
            recordRepository = ...,
        )
```
凡测试原先靠 `recordModelTransToViewsUseCase` + repo 的 RecordModel 断言列表内容者，改为向 `FakeRecordRepository.recordViews` 注入 `RecordViewsModel`（见 Task 4 Step 4）。

- [ ] **Step 4: 运行 feature:records 测试**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest" --offline --no-daemon --console=plain`
Expected: PASS

- [ ] **Step 5: 验证 app 跨模块 Hilt 图（构造签名变更）**

Run: `./gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（Hilt 对 LauncherContentViewModel 新构造重新生成注入，无缺失绑定）

- [ ] **Step 6: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt
git commit -m "[feat|feature:records|首页分页][公共]LauncherContentViewModel 改用 RecordViewsModel 分页流（去逐条 N+1 转换、移除 transToViews 依赖）"
```

---

### Task 6: LazyColumn 补 `contentType`（根因 C）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt:521-530`

- [ ] **Step 1: 加 `contentType`**

在 `items(count = ..., key = { ... })` 加 `contentType`：
```kotlin
                    items(
                        count = items.itemCount,
                        key = { index ->
                            when (val item = items.peek(index)) {
                                is LauncherListItem.DayHeader -> "header_${item.dateStr}"
                                is LauncherListItem.Record -> "record_${item.entity.id}"
                                null -> "placeholder_$index"
                            }
                        },
                        contentType = { index ->
                            when (items.peek(index)) {
                                is LauncherListItem.DayHeader -> "day_header"
                                is LauncherListItem.Record -> "record"
                                null -> "placeholder"
                            }
                        },
                    ) { index ->
```

- [ ] **Step 2: 编译 feature:records**

Run: `./gradlew :feature:records:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt
git commit -m "[perf|feature:records|首页列表][公共]LazyColumn 补 contentType 提升异质项复用（根因 C）"
```

---

### Task 7: `@Relation` 加载正确性 androidTest（真库）

**Files:**
- Create: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoRelationTest.kt`

**Interfaces:**
- Consumes: Task 2 `pagingLauncherRecordViews`

- [ ] **Step 1: 写 androidTest（真库插入 + 断言各 @Relation）**

`RecordDaoRelationTest.kt`（参照 core:database 现有 `DatabaseTest`/androidTest 建库方式：`Room.inMemoryDatabaseBuilder`）：
```kotlin
/* Copyright ... Apache 2.0 ... */
package cn.wj.android.cashbook.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordDaoRelationTest {

    private lateinit var db: CashbookDatabase
    private lateinit var recordDao: RecordDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).build()
        recordDao = db.recordDao() // 若 accessor 名不同，按 CashbookDatabase 实际 abstract fun 修正
    }

    @After fun teardown() = db.close()

    @Test
    fun paging_relation_loads_type_and_bidirectional_related() = runTest {
        // 插入类型、支出记录、吸收它的收入记录 + 关联行（related_record_id=支出）
        // 具体插入 API 用现有 typeDao/recordDao 的 insert 方法；关联行插 db_record_with_related
        // …（实现时按现有 DAO insert 方法填充：普通支出 recId=1 typeId=5(EXPENDITURE)，
        //   报销收入 recId=2 typeId=FIXED_TYPE_ID_REIMBURSE，related(record_id=2, related_record_id=1)）
        val source = recordDao.pagingLauncherRecordViews(booksId = 1L, startDate = 0L, endDate = Long.MAX_VALUE)
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page

        val expenditure = result.data.first { it.record.id == 1L }
        assertThat(expenditure.types.single().id).isEqualTo(5L)          // type 正确物化
        assertThat(expenditure.relatedAsRelatedId.map { it.id }).containsExactly(2L) // 支出侧：被收入 2 吸收
        val income = result.data.first { it.record.id == 2L }
        assertThat(income.relatedAsRecordId.map { it.id }).containsExactly(1L)        // 收入侧：吸收支出 1
        assertThat(income.relatedAsRelatedId).isEmpty()                  // 双向不混淆
    }

    @Test
    fun paging_relation_balance_record_has_empty_type() = runTest {
        // 插入平账支出 typeId=-1101（db_type 无此行）
        val source = recordDao.pagingLauncherRecordViews(booksId = 1L, startDate = 0L, endDate = Long.MAX_VALUE)
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        val balance = result.data.first { it.record.typeId == -1101L }
        assertThat(balance.types).isEmpty() // @Relation 天然 LEFT：主记录不丢，type 空
    }
}
```
> 插入细节按 core:database 现有 DAO 的 insert/upsert 方法填充；参照同目录既有 androidTest（如 `TransactionDaoTest`/`DatabaseTest`）的建库与插入范式。

- [ ] **Step 2: 运行 androidTest（需模拟器/真机在线）**

Run: `./gradlew :core:database:connectedDebugAndroidTest --tests "*RecordDaoRelationTest" --no-daemon --console=plain`
（首次需联网拉 UTP；本机经代理时注意 TLS 稳定性——见 CLAUDE.md）
Expected: PASS（双向不混淆 + 平账 type 空）

- [ ] **Step 3: Commit**

```bash
git add core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoRelationTest.kt
git commit -m "[test|core:database|首页分页][公共]@Relation 分页加载 androidTest（双向 relatedRecord 不混淆 + 平账 type 空）"
```

---

### Task 8: 真机 journey 验收（不 commit 代码，产出验收记录）

**Files:** 无代码改动；产出 `docs/testing/reports/2026-07-06-launcher-paging-journey.md`（可选）。

- [ ] **Step 1: 全量编译 + 全模块单测**

Run: `./gradlew :app:assembleOnlineDebug :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: 全 `BUILD SUCCESSFUL` / 测试 PASS

- [ ] **Step 2: 模拟器 journey（android-cli 黑盒）**

装 APK → 首页：
1. 首屏列表正确加载（金额、报销/退款对冲显示、日期头分组正确）；
2. **滚到列表中部** → 新增一条记录 → 返回首页：列表刷新出现新记录，**滚动位置不回顶**；
3. 编辑/删除中部一条 → 列表刷新、**不回顶**；
4. 切月 / 切「全部」→ 数据正确。

- [ ] **Step 3: 记录验收结论**（现象 + 命令/截图证据），交付前作为「完整链路验证」凭据。

---

## Self-Review（对照 spec）

- **spec §2 根因 A（N+1）** → Task 2/3/4/5（@Relation 分页 + mapper + Repository + ViewModel）✅
- **spec §2 根因 C（contentType）** → Task 6 ✅
- **spec §4.1 架构分层（mapper 放 core:data）** → Task 3/4；纯函数因 core:model 依赖约束改放 core:data（Task 1，已在 Global Constraints 注明 spec 勘误）✅
- **spec §4.3 关键决策（双向选向/平账/relatedAmount 内存计算/firstOrNull）** → Task 3 mapper + 测试 ✅
- **spec §5 回顶/invalidation（Room 免费）** → Task 2 Step 4 核验 + Task 8 真机验不回顶 ✅
- **spec §5 separators 留 VM 层** → Task 5 Step 1 保留 insertSeparators ✅
- **spec §5 books_id 隔离** → Task 2 DAO WHERE 含 books_id ✅
- **spec §5 测试替身同步** → Task 4 FakeRecordDao/FakeRecordRepository ✅
- **spec §6 测试策略（mapper 等价性 + @Relation androidTest + 真机 journey）** → Task 3/7/8 ✅
- **spec §7 根因 B（flowOn）** → 列为可选，未纳入本计划 Task（交付后单独评估）✅ 一致
- **spec §8 唯一消费方首页、transBatch 非 dead code** → Task 4 只改 getRecordPagingData；Task 1 保留 transBatch 调用 ✅
- **占位符扫描**：Task 3/7 有两处「按实际 Table/DAO 定义对齐」的说明（Table 构造签名、androidTest 插入 API）——因这些依赖具体 @Entity/DAO 字段，实现时以真实定义为准，非可省略的 TODO；其余代码步骤均完整。
- **类型一致性**：`getRecordPagingData: Flow<PagingData<RecordViewsModel>>`、`toRecordViewsModel()`、`pagingLauncherRecordViews(...)` 在各 Task 引用一致 ✅
