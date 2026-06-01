# Cashbook 评审 Top10 剩余 5 项修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 2026-05-30 全维度评审 Top10 中剩余 5 项(#7 N+1 批量化、#9a 导入测试、#9b RepositoryImplTest 测试债、#10a AutoBackup 失败重试、#10b 抽两口径金额函数)。

**Architecture:** 按 module 依赖自底向上串行(core/model→core/database→core/domain→core/data→feature/record-import→sync/work);每项独立原子 commit,严格 TDD;controller 亲自串行执行(不用后台 Workflow,本机实证不稳)。

**Tech Stack:** Kotlin、Jetpack Compose、Room、Hilt、Coroutines、WorkManager、Truth、Roborazzi。

**Spec:** `docs/superpowers/specs/2026-06-01-cashbook-review-top10-leftover-design.md`

**关键约束(已 hands-on 核实):**
- 金额公式**两种口径**:`recordAmount`(DAO/月度,INCOME→`amount-charges`,其余→`amount+charges-concessions`)与 `analyticsPieAmount`(Pie,EXPENDITURE→`amount+charges-concessions`,其余→`amount-charges`)对 **TRANSFER 处理相反**,不可统一(reverse Critical)。
- `MappingTest.kt` 已全覆盖 asModel/asTable,#9b **不新建映射测试**。
- `CombineProtoDataSource` 是 final class,#9b **不真实例化 Impl**。
- `requestAutoBackup`/`requestBackup`/`startBackup` 调用链全 suspend 同步执行,可同步返回 state。

**gradle 命令模板(本机)**:
```
./gradlew :<module>:<task> -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8 -Duser.country=CN -Duser.language=zh" --no-daemon --console=plain --offline
```
- core/model 是 JVM 库,测试任务 `:core:model:test`;android library/feature/sync 用 `:<module>:testDebugUnitTest`。
- worktree 首构建缺依赖时去掉 `--offline` 并带本地代理 `-Dhttp(s).proxyHost=127.0.0.1 -Dhttp(s).proxyPort=7897` 暖一次缓存。
- 判定只信 `BUILD SUCCESSFUL`/`BUILD FAILED`,不信 exit code。

---

## Phase A — core/model(#10b 两口径函数)

### Task 1: recordAmount(DAO/月度口径)

**Files:**
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmount.kt`
- Create: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmountTest.kt`
- Modify(若无测试依赖): `core/model/build.gradle.kts`

- [ ] **Step 1: 确认 core/model 测试依赖**

Read `core/model/build.gradle.kts`。若无 `testImplementation(libs.junit)` + `testImplementation(libs.google.truth)`,在 `dependencies {}` 添加(参照 `core/common/build.gradle.kts:38-39`)。无 dependencies 块则新建。

- [ ] **Step 2: 写失败测试**

```kotlin
/* Apache License Header（复制同模块其他文件的头） */
package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordAmountTest {
    @Test
    fun income_subtracts_charges() {
        assertThat(recordAmount(RecordTypeCategoryEnum.INCOME, 10000, 200, 50)).isEqualTo(9800)
    }

    @Test
    fun expenditure_adds_charges_minus_concessions() {
        assertThat(recordAmount(RecordTypeCategoryEnum.EXPENDITURE, 10000, 200, 50)).isEqualTo(10150)
    }

    @Test
    fun transfer_follows_non_income_branch() {
        // 关键金丝雀：TRANSFER 走"非收入"分支 = amount + charges - concessions（当支出）
        assertThat(recordAmount(RecordTypeCategoryEnum.TRANSFER, 10000, 200, 50)).isEqualTo(10150)
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `:core:model:test --tests "*RecordAmountTest*"`
Expected: FAIL（unresolved reference: recordAmount）

- [ ] **Step 4: 实现**

```kotlin
/* Apache License Header */
package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 记录实际金额（DAO/月度结余口径，单位：分）。
 * - 收入：金额 - 手续费
 * - 支出 / 转账：金额 + 手续费 - 优惠
 *
 * 与 [analyticsPieAmount] 是两种不同口径：本函数 TRANSFER 归"非收入"(当支出)，
 * Pie 口径 TRANSFER 归"非支出"(当收入)，二者对 TRANSFER 处理相反，不可互换。
 */
fun recordAmount(
    category: RecordTypeCategoryEnum,
    amount: Long,
    charges: Long,
    concessions: Long,
): Long = if (category == RecordTypeCategoryEnum.INCOME) {
    amount - charges
} else {
    amount + charges - concessions
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `:core:model:test --tests "*RecordAmountTest*"`
Expected: PASS（BUILD SUCCESSFUL）

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmount.kt core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmountTest.kt core/model/build.gradle.kts
git commit -m "feat(core/model): 抽 recordAmount DAO/月度口径金额纯函数 + 测试"
```

### Task 2: analyticsPieAmount(Pie 口径)

**Files:**
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmount.kt`(同文件追加)
- Modify: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmountTest.kt`(追加测试类或同类追加）

- [ ] **Step 1: 写失败测试**(追加到 RecordAmountTest 或新建 AnalyticsPieAmountTest)

```kotlin
class AnalyticsPieAmountTest {
    @Test
    fun expenditure_adds_charges_minus_concessions() {
        assertThat(analyticsPieAmount(RecordTypeCategoryEnum.EXPENDITURE, 10000, 200, 50)).isEqualTo(10150)
    }

    @Test
    fun income_subtracts_charges() {
        assertThat(analyticsPieAmount(RecordTypeCategoryEnum.INCOME, 10000, 200, 50)).isEqualTo(9800)
    }

    @Test
    fun transfer_follows_non_expenditure_branch() {
        // 关键金丝雀：Pie 口径 TRANSFER 走"非支出"分支 = amount - charges（当收入），与 recordAmount 相反
        assertThat(analyticsPieAmount(RecordTypeCategoryEnum.TRANSFER, 10000, 200, 50)).isEqualTo(9800)
    }
}
```

- [ ] **Step 2: 运行确认失败** — Run: `:core:model:test --tests "*AnalyticsPieAmountTest*"` Expected: FAIL

- [ ] **Step 3: 实现**(追加到 RecordAmount.kt)

```kotlin
/**
 * Analytics 饼图金额口径（单位：分）。
 * - 支出：金额 + 手续费 - 优惠
 * - 收入 / 转账：金额 - 手续费
 *
 * 与 [recordAmount] 口径相反（见该函数注释）。仅用于收支分类饼图统计。
 */
fun analyticsPieAmount(
    typeCategory: RecordTypeCategoryEnum,
    amount: Long,
    charges: Long,
    concessions: Long,
): Long = if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
    amount + charges - concessions
} else {
    amount - charges
}
```

- [ ] **Step 4: 运行确认通过** — Run: `:core:model:test` Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmount.kt core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordAmountTest.kt
git commit -m "feat(core/model): 抽 analyticsPieAmount Pie 口径金额纯函数 + 测试(含 TRANSFER 反向金丝雀)"
```

---

## Phase B — core/database(#10b DAO 复用)

### Task 3: TransactionDao.calculateRecordAmount 委托 recordAmount

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt:151-163`
- Test(护航,既有): `core/data/src/test/.../dao/TransactionDaoLogicTest.kt:80-102`

- [ ] **Step 1: 确认既有护航测试覆盖三分支**

Read `TransactionDaoLogicTest.kt:80-102`,确认 income/expenditure/transfer 三测存在且断言金额公式。这三测将护航本次委托重构(行为须保持)。

- [ ] **Step 2: 改实现为委托**

`TransactionDao.kt:151-163` 现为：
```kotlin
fun calculateRecordAmount(record: RecordTable, category: RecordTypeCategoryEnum): Long {
    return if (category == RecordTypeCategoryEnum.INCOME) {
        record.amount - record.charge
    } else {
        record.amount + record.charge - record.concessions
    }
}
```
改为(import `cn.wj.android.cashbook.core.model.model.recordAmount`):
```kotlin
fun calculateRecordAmount(record: RecordTable, category: RecordTypeCategoryEnum): Long =
    recordAmount(category, record.amount, record.charge, record.concessions)
```

- [ ] **Step 3: 运行护航测试确认仍通过**

Run: `:core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest*"`
Expected: PASS（行为等价,三测不变）

- [ ] **Step 4: Commit**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt
git commit -m "refactor(core/database): calculateRecordAmount 委托 core/model recordAmount(行为等价)"
```

---

## Phase C — core/domain(#10b UseCase + #7 N+1)

### Task 4: GetAssetMonthSummaryUseCase 用 recordAmount

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetMonthSummaryUseCase.kt:65-69`
- Test(既有): `core/domain/src/test/.../GetAssetMonthSummaryUseCaseTest.kt`(若存在)

- [ ] **Step 1: 查既有测试**

Glob `core/domain/src/test/**/GetAssetMonthSummaryUseCaseTest.kt`。若存在,记录其断言(将护航重构);若不存在,跳过(无回归护栏,靠 recordAmount 自身测试 + 等价性)。

- [ ] **Step 2: 改实现**

`GetAssetMonthSummaryUseCase.kt:65-69` 现为：
```kotlin
val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
    record.amount - record.charges
} else {
    record.amount + record.charges - record.concessions
}
```
改为(import `cn.wj.android.cashbook.core.model.model.recordAmount` as 函数;注意局部变量名 `recordAmount` 与函数同名冲突 → 局部变量改名 `amount`)：
```kotlin
val amount = recordAmount(category, record.amount, record.charges, record.concessions)
```
并将下方 `:70-77` 引用 `recordAmount` 的两处局部变量改为 `amount`。

- [ ] **Step 3: 运行测试**

Run: `:core:domain:testDebugUnitTest --tests "*GetAssetMonthSummary*"`(无该测试则跑 `:core:domain:testDebugUnitTest` 整体确认编译+不回归)
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetMonthSummaryUseCase.kt
git commit -m "refactor(core/domain): GetAssetMonthSummaryUseCase 月度结余复用 recordAmount(消除第4处副本)"
```

### Task 5: 两个 Pie UseCase 用 analyticsPieAmount + TRANSFER 金丝雀

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsPieUseCase.kt:48-52,74-78`
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsPieSecondUseCase.kt:48-52,66-70`
- Create/Modify: `core/domain/src/test/.../TransRecordViewsToAnalyticsPieUseCaseTest.kt`(补 TRANSFER 用例)

- [ ] **Step 1: 写 TRANSFER 金丝雀失败测试**

查既有 PieUseCaseTest(Glob)。补一条 TRANSFER 用例:构造一条 `RecordViewsModel`(type.typeCategory=TRANSFER,amount=10000,charges=200,concessions=50,type.typeLevel=FIRST),调 `useCase(RecordTypeCategoryEnum.TRANSFER, listOf(record))`,断言返回 entity 的 `totalAmount == 9800`(amount-charges,Pie 口径)。
> 若无既有 PieUseCaseTest,新建之(含 EXPENDITURE/INCOME/TRANSFER 三态,用 core:testing Fake 构造 RecordViewsModel)。

- [ ] **Step 2: 运行确认通过**(重构前 — 现有代码 TRANSFER 已走 else=amount-charges,金丝雀应先 PASS,固化现状)

Run: `:core:domain:testDebugUnitTest --tests "*AnalyticsPie*"`
Expected: PASS（固化 TRANSFER→amount-charges 现状,作为重构护栏）

- [ ] **Step 3: 改两个 Pie UseCase 委托 analyticsPieAmount**

两文件各 2 处 `if (typeCategory == EXPENDITURE) { amount + charges - concessions } else { amount - charges }`(`it.`/`record.` 前缀)替换为(import `analyticsPieAmount`)：
```kotlin
analyticsPieAmount(typeCategory, it.amount, it.charges, it.concessions)
```
（Second UseCase 用其推断的 `typeCategory` 变量,字段前缀按上下文 `record.`/`it.`）

- [ ] **Step 4: 运行确认仍通过**(行为等价,含 TRANSFER 金丝雀)

Run: `:core:domain:testDebugUnitTest --tests "*AnalyticsPie*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsPieUseCase.kt core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsPieSecondUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsPieUseCaseTest.kt
git commit -m "refactor(core/domain): 两个 Pie UseCase 复用 analyticsPieAmount + 补 TRANSFER 金丝雀测试"
```

### Task 6: 4 个排序型 caller 批量化

**Files(均 Modify):**
- `core/domain/.../usecase/GetAssetRecordViewsUseCase.kt:51-52`
- `core/domain/.../usecase/GetSearchRecordViewsUseCase.kt:48-50`
- `core/domain/.../usecase/GetTagRecordViewsUseCase.kt:48-50`
- `core/domain/.../usecase/GetTypeRecordViewsUseCase.kt:65-67`
- Test: 各对应 `*UseCaseTest`(查既有,补"批量==单条逐元素"断言)

- [ ] **Step 1: 写/补失败测试(以 GetTypeRecordViewsUseCase 为例,其余同构)**

用 core:testing Fake 装入 N 条记录,断言 `useCase(...)` 结果与"逐条 `recordModelTransToViewsUseCase(it).asEntity()`"结果逐元素相等(含顺序、relatedRecord)。其余 3 个 caller 同样补一条等价断言测试。

- [ ] **Step 2: 运行确认失败/或先 PASS 固化**(若测试是新写的等价断言,重构前现状即单条,应 PASS,作护栏)

Run: `:core:domain:testDebugUnitTest --tests "*GetTypeRecordViews*" --tests "*GetAssetRecordViews*" --tests "*GetSearchRecordViews*" --tests "*GetTagRecordViews*"`

- [ ] **Step 3: 4 个文件批量化改造(模式相同)**

每个的 `.sortedByDescending { it.recordTime }.map { recordModelTransToViewsUseCase(it).asEntity() }` 改为：
```kotlin
recordModelTransToViewsUseCase(
    <原查询结果>.sortedByDescending { it.recordTime }
).map { it.asEntity() }
```
即先 `sortedByDescending` 得 `List<RecordModel>`,整体传入 `recordModelTransToViewsUseCase(list)`(重载 transBatch),再 `.map { it.asEntity() }`。

- [ ] **Step 4: 运行确认通过**

Run: 同 Step 2 四个 test
Expected: PASS（批量结果与单条逐元素相等）

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetRecordViewsUseCase.kt core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetSearchRecordViewsUseCase.kt core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCase.kt core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypeRecordViewsUseCase.kt core/domain/src/test/
git commit -m "perf(core/domain): 4 个排序型 RecordViews UseCase 批量化消除 N+1 + 等价测试"
```

### Task 7: GetCurrentMonthRecordViewsUseCase(Flow)批量化

**Files:**
- Modify: `core/domain/.../usecase/GetCurrentMonthRecordViewsUseCase.kt:38-39`
- Test: `*GetCurrentMonthRecordViewsUseCaseTest`

- [ ] **Step 1: 补等价测试**(Flow 收集首值,断言批量==单条逐元素)
- [ ] **Step 2: 运行(护栏 PASS)** — Run: `:core:domain:testDebugUnitTest --tests "*GetCurrentMonthRecordViews*"`
- [ ] **Step 3: 改造** — `mapLatest { list -> list.map { recordModelTransToViewsUseCase(it).asEntity() } }` 改为 `mapLatest { list -> recordModelTransToViewsUseCase(list).map { it.asEntity() } }`（保留 mapLatest 流式语义）
- [ ] **Step 4: 运行确认通过** — Expected: PASS
- [ ] **Step 5: Commit**
```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetCurrentMonthRecordViewsUseCase.kt core/domain/src/test/
git commit -m "perf(core/domain): GetCurrentMonthRecordViewsUseCase Flow 内批量化消除 N+1 + 等价测试"
```

### Task 8: GetRelatedRecordViewsUseCase(filter)批量化

**Files:**
- Modify: `core/domain/.../usecase/GetRelatedRecordViewsUseCase.kt:63-66`
- Test: `*GetRelatedRecordViewsUseCaseTest`

- [ ] **Step 1: 补等价测试**(含被 filter 过滤的记录,断言批量结果==单条逐元素,filter 语义保持)
- [ ] **Step 2: 运行(护栏)** — Run: `:core:domain:testDebugUnitTest --tests "*GetRelatedRecordViews*"`
- [ ] **Step 3: 改造** — `.filter { queryRelatedRecordCountById(it.id) <= 0 }.map { recordModelTransToViewsUseCase(it).asEntity() }` 改为：
```kotlin
.filter { queryRelatedRecordCountById(it.id) <= 0 }
.let { recordModelTransToViewsUseCase(it) }
.map { it.asEntity() }
```
（filter 仍前置逐条 `queryRelatedRecordCountById`——范围外不动;仅 transToViews 批量化）
- [ ] **Step 4: 运行确认通过** — Expected: PASS
- [ ] **Step 5: Commit**
```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetRelatedRecordViewsUseCase.kt core/domain/src/test/
git commit -m "perf(core/domain): GetRelatedRecordViewsUseCase filter 后批量化消除 N+1 + 等价测试"
```

---

## Phase D — core/data(#9a parser/matcher + #9b + #10a)

### Task 9: BillCategoryMatcherTest(脱敏)

**Files:**
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/BillCategoryMatcherTest.kt`

- [ ] **Step 1: 写测试**(表驱动,**全脱敏**:对手方用虚构名)

```kotlin
class BillCategoryMatcherTest {
    private val types = listOf(/* 用 createRecordTypeModel 造含餐饮/交通等关键词的 RecordTypeModel */)

    @Test fun matches_by_keyword() {
        val result = BillCategoryMatcher.match("虚构商户", "餐饮美食", types)
        assertThat(result?.name).isEqualTo("餐饮")
    }

    @Test fun returns_null_when_no_rule_hit() {
        assertThat(BillCategoryMatcher.match("虚构", "无法匹配文本xyz", types)).isNull()
    }
}
```
> 先 Read `BillCategoryMatcher.kt:35` 的 `CATEGORY_RULES` 关键词表,据实造 types 与断言。

- [ ] **Step 2: 运行确认**(可能直接 PASS,纯函数已实现) — Run: `:core:data:testDebugUnitTest --tests "*BillCategoryMatcherTest*"`
- [ ] **Step 3: 如有断言不符,调整测试以匹配实现真实行为**(不改生产代码)
- [ ] **Step 4: Commit**
```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/BillCategoryMatcherTest.kt
git commit -m "test(core/data): 补 BillCategoryMatcher 表驱动测试(脱敏 fixture)"
```

### Task 10: BillPaymentMatcherTest(脱敏)

**Files:**
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/BillPaymentMatcherTest.kt`

- [ ] **Step 1: 写测试**(4 阶策略 + extractCardSuffix;卡号后四位用 `0000`/`1234` 虚构值)

```kotlin
class BillPaymentMatcherTest {
    @Test fun matches_exact_asset_name() { /* assets 含名为"招商银行"的 AssetModel,paymentMethods=["招商银行"] → 命中 */ }
    @Test fun matches_bank_keyword_with_card_suffix() { /* "招商银行储蓄卡(0000)" → 命中按行+后四位 */ }
    @Test fun unmatched_returns_no_mapping() { /* "未知方式" → 未匹配项 */ }
}
```
> 先 Read `BillPaymentMatcher.kt:85-157` 的 matchSingle/extractCardSuffix 精确策略与返回结构 PaymentMethodMapping,据实写。

- [ ] **Step 2-4: 运行/调整/Commit**(同 Task 9 模式)
```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/BillPaymentMatcherTest.kt
git commit -m "test(core/data): 补 BillPaymentMatcher 4阶匹配+卡号正则测试(脱敏)"
```

### Task 11: WechatBillParser 纯函数(parseDateTime/convertToItem)internal + 测试

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParser.kt`(parseDateTime:310、convertToItem:258 由 `private` 改 `internal` + `@VisibleForTesting`)
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParserTest.kt`

- [ ] **Step 1: 改可见性** — `parseDateTime`、`convertToItem` 加 `@VisibleForTesting`(import `androidx.annotation.VisibleForTesting`)并由 `private` 改 `internal`。
- [ ] **Step 2: 写测试**(parseDateTime 三格式 + convertToItem 边界)

```kotlin
class WechatBillParserTest {
    @Test fun parse_iso_datetime() { assertThat(WechatBillParser.parseDateTime("2026-03-26T11:50:04")).isEqualTo(/*expected millis*/) }
    @Test fun parse_standard_datetime() { /* "2026-03-26 11:50:04" */ }
    @Test fun parse_excel_serial() { /* "46107.493..." */ }
    @Test fun convert_to_item_maps_fields() { /* 给脱敏行数据 List<String>,断言 ImportedBillItem 字段 */ }
}
```
> 先 Read `WechatBillParser.kt:258-332` 确认 parseDateTime/convertToItem 签名与返回,写精确断言(时间用固定时区计算预期 millis)。

- [ ] **Step 3: 运行确认** — Run: `:core:data:testDebugUnitTest --tests "*WechatBillParserTest*"` Expected: PASS
- [ ] **Step 4: Commit**
```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParser.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParserTest.kt
git commit -m "test(core/data): WechatBillParser parseDateTime/convertToItem 改 internal+@VisibleForTesting 并补纯函数测试"
```

### Task 12: WechatBillParser.parse() PoC + 端到端/退化决策

**Files:**
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParserTest.kt`
- 可能 Create: `core/data/src/test/resources/wechat-sample.xlsx`(脱敏)
- 可能 Modify: `core/data/build.gradle.kts`(若需 Robolectric)

- [ ] **Step 1: PoC — 写最小 parse() 测试**

构造最小脱敏 xlsx(或字节流)喂 `WechatBillParser.parse(inputStream)`,断言返回非 null:
```kotlin
@Test fun parse_minimal_sample() {
    val input = javaClass.classLoader!!.getResourceAsStream("wechat-sample.xlsx")!!
    val result = WechatBillParser.parse(input)
    assertThat(result).isNotNull()
    assertThat(result!!.items).isNotEmpty()
}
```

- [ ] **Step 2: 实跑判定 XmlPullParser 可行性**

Run: `:core:data:testDebugUnitTest --tests "*parse_minimal_sample*"`
- 若 **PASS** → 纯 JVM 可跑,补完整 parse() 端到端断言(金额分/方向/时间),进 Step 4。
- 若 **FAIL with `Stub!`/XmlPullParserException** → XmlPullParser 在纯 JVM 不可用。决策(spec H3):优先**退化**——删除 parse_minimal_sample,parse() 端到端不在 JVM 测(纯函数 Task 11 已覆盖核心逻辑);在 `WechatBillParser` 加注释说明 parse() 需 instrumented/Robolectric。**不**为单测引入 Robolectric(范围控制),除非后续单独决定。

- [ ] **Step 3: 制作脱敏 fixture(若 Step 2 走端到端)**

手工构造 `wechat-sample.xlsx`:2-3 行,对手方"测试商户A/测试商户B",卡号后四位 `0000`,单号全 `0`,金额整数元(如 100.00),顶部 sheet 注释"人工合成测试数据"。commit 前 grep 校验无真实姓名/真实卡号(`grep -rE '\([0-9]{4}\)' core/data/src/test/resources/` 仅出现 0000)。

- [ ] **Step 4: 运行 + Commit**
```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParserTest.kt
# 若走端到端: git add core/data/src/test/resources/wechat-sample.xlsx core/data/build.gradle.kts
git commit -m "test(core/data): WechatBillParser.parse() PoC + 端到端/退化(脱敏 fixture)"
```

### Task 13: #9b 重命名 6 个 ImplTest + 审计 MappingTest 补缺

**Files:**
- Rename/Modify: `core/data/src/test/.../repository/impl/{Asset,Books,Record,Setting,Tag,Type}RepositoryImplTest.kt`
- Audit: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/MappingTest.kt`

- [ ] **Step 1: 逐一核实 6 个 ImplTest 实际被测对象**

逐个 Read,分类:
- 纯测 Fake DAO/DataSource 委托(无 Impl 实例、无手工复刻 Impl 逻辑)→ 重命名为反映被测对象(如 `*RepositoryImplTest` → `Fake*DaoBehaviorTest` 或 `*MappingTest` 若测映射)。
- 含手工复刻 Impl 业务逻辑的(如 `AssetRepositoryImplTest:168-191` "模拟 updateAsset 逻辑")→ **保留原文件**,在该测试方法上加注释"// 手工复刻 Impl 逻辑,非测 Impl 本体(CombineProtoDataSource final 无法实例化)"。

- [ ] **Step 2: 执行重命名**(git mv 保留历史)

对纯 Fake 委托的文件 `git mv` 改名 + 改类名 + 改文件内 class 声明。

- [ ] **Step 3: 审计 MappingTest 补缺**

Read `MappingTest.kt`,对照 6 类 Table↔Model 的字段,列出未断言的字段(如新增列)。有缺口则补断言;无缺口则记录"已全覆盖,无需补"。

- [ ] **Step 4: 运行整模块测试确认编译+通过**

Run: `:core:data:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（重命名后整源集编译通过）

- [ ] **Step 5: Commit**
```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/
git commit -m "test(core/data): 诚实重命名误导的 RepositoryImplTest + 审计 MappingTest 覆盖(#9b 测试债)"
```

### Task 14: #10a BackupRecoveryManager 接口 + Impl + Fake 改返回 state

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupRecoveryManager.kt:41,43`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt:146-172,361-506`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeBackupRecoveryManager.kt:72-79`

> 注:本 Task 是 #10a 接口契约改动,行为重构由既有恢复/备份链路 + Task 16 Worker 测试间接覆盖;本 Task 自身重点是编译通过 + 既有 core:data 测试不回归。

- [ ] **Step 1: 改接口签名**

`BackupRecoveryManager.kt`:
```kotlin
suspend fun requestAutoBackup(): BackupRecoveryState
suspend fun requestBackup(onlyLocal: Boolean = false): BackupRecoveryState
```

- [ ] **Step 2: 改 Impl**

`requestAutoBackup():146`:`= withContext(ioCoroutineContext) { requestBackup(onlyLocal = ...) }`(withContext 末表达式即返回值,自动返回 requestBackup 的 state)。

`requestBackup():153-172` 签名加 `: BackupRecoveryState`,各分支返回最终态:
```kotlin
override suspend fun requestBackup(onlyLocal: Boolean): BackupRecoveryState =
    withContext(ioCoroutineContext) {
        updateBackupState(BackupRecoveryState.InProgress)
        val backupPath = settingRepository.appSettingsModel.first().backupPath
        if (backupPath.isBlank()) {
            updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)
        } else if (grantedPermissions(backupPath)) {
            startBackup(backupPath, onlyLocal)
        } else {
            updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED))
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED)
        }
    }
```

`startBackup():361` 签名加 `: BackupRecoveryState`;`:367` blank early-return 改：
```kotlin
updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
return BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)
```
`:499-505` 末尾改：
```kotlin
val state = if (result == BackupRecoveryState.SUCCESS_BACKUP)
    BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP)
else BackupRecoveryState.Failed(result)
updateBackupState(state)
logger().i("startBackup(), result = <$result>")
return state
```

- [ ] **Step 3: 改 Fake**

`FakeBackupRecoveryManager.kt`:
```kotlin
private var autoBackupResult: BackupRecoveryState = BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP)
fun setAutoBackupResult(state: BackupRecoveryState) { autoBackupResult = state }

override suspend fun requestAutoBackup(): BackupRecoveryState {
    requestBackupCalled = true
    return autoBackupResult
}
override suspend fun requestBackup(onlyLocal: Boolean): BackupRecoveryState {
    requestBackupCalled = true
    lastRequestBackupOnlyLocal = onlyLocal
    return autoBackupResult
}
```

- [ ] **Step 4: 全量编译相关模块(确认 ViewModel 调用方不破坏)**

Run: `:core:data:testDebugUnitTest`(含已有 BackupRecovery 测试);再 `:feature:settings:testDebugUnitTest`(确认 `BackupAndRecoveryViewModel` 调 requestBackup 忽略返回值仍编译)。
Expected: 两者 BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**
```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupRecoveryManager.kt core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeBackupRecoveryManager.kt
git commit -m "feat(core/data): requestAutoBackup/requestBackup 返回 BackupRecoveryState 供 Worker 据状态重试(#10a)"
```

---

## Phase E — feature/record-import(#9a VM)

### Task 15: RecordImportViewModelTest 扩充 happy-path

**Files:**
- Modify: `feature/record-import/src/test/kotlin/cn/wj/android/cashbook/feature/record/imports/viewmodel/RecordImportViewModelTest.kt`

- [ ] **Step 1: 读现状** — Read 现有测试(已覆盖去重 POSSIBLE/NONE),确认 Fake 装配方式。
- [ ] **Step 2: 补 happy-path 测试** — 验证导入预览:金额由元(Double)正确转分(Long,`toCent`)、direction→收支方向、去重状态。用 `FakeRecordRepository` 等既有 Fake。
- [ ] **Step 3: 运行** — Run: `:feature:record-import:testDebugUnitTest` Expected: PASS
- [ ] **Step 4: Commit**
```bash
git add feature/record-import/src/test/kotlin/cn/wj/android/cashbook/feature/record/imports/viewmodel/RecordImportViewModelTest.kt
git commit -m "test(feature/record-import): 扩充 RecordImportViewModel happy-path(金额分/方向/去重)"
```

---

## Phase F — sync/work(#10a Worker)

### Task 16: AutoBackupWorker 据状态返回 Result + Worker 测试

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/AutoBackupWorker.kt:55-77`
- Modify: `sync/work/build.gradle.kts`(加 test 依赖块)
- Create: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/workers/AutoBackupWorkerTest.kt`

- [ ] **Step 1: 加测试依赖** — `sync/work/build.gradle.kts` `dependencies {}` 加:
```kotlin
testImplementation(libs.androidx.work.testing)
testImplementation(libs.junit)
testImplementation(libs.google.truth)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.robolectric)            // 若版本目录有
testImplementation(projects.core.testing)
```
（先 Read `gradle/libs.versions.toml` 确认 robolectric alias;无则 Worker 测试改用 `TestListenableWorkerBuilder` + Robolectric runner,版本按既有 feature 模块测试配置对齐。）

- [ ] **Step 2: 写失败测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class AutoBackupWorkerTest {
    @Test fun success_state_returns_success() = runTest {
        val fake = FakeBackupRecoveryManager().apply { setAutoBackupResult(BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP)) }
        val worker = buildWorker(fake)
        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
    }
    @Test fun webdav_failure_returns_retry() = runTest {
        val fake = FakeBackupRecoveryManager().apply { setAutoBackupResult(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_WEBDAV)) }
        assertThat(buildWorker(fake).doWork()).isEqualTo(ListenableWorker.Result.retry())
    }
    @Test fun config_failure_returns_failure() = runTest {
        val fake = FakeBackupRecoveryManager().apply { setAutoBackupResult(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)) }
        assertThat(buildWorker(fake).doWork()).isEqualTo(ListenableWorker.Result.failure())
    }
    private fun buildWorker(fake: FakeBackupRecoveryManager): AutoBackupWorker =
        TestListenableWorkerBuilder<AutoBackupWorker>(ApplicationProvider.getApplicationContext())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(c: Context, n: String, p: WorkerParameters) =
                    AutoBackupWorker(c, p, fake, UnconfinedTestDispatcher())
            }).build()
}
```
> 注:`AutoBackupWorker` 经 `DelegatingWorker` 委托运行,直接 `TestListenableWorkerBuilder<AutoBackupWorker>` 构造本体测 doWork 即可(绕过 Hilt assisted)。`delay(2_000L)` 在 runTest 虚拟时钟下不阻塞。

- [ ] **Step 3: 运行确认失败** — Run: `:sync:work:testDebugUnitTest` Expected: FAIL(doWork 当前恒 success)

- [ ] **Step 4: 改 doWork**

```kotlin
override suspend fun doWork(): Result {
    logger().i("doWork(), requestAutoBackup")
    val state = withContext(ioDispatcher) {
        delay(2_000L)
        backupRecoveryManager.requestAutoBackup()
    }
    return when {
        state is BackupRecoveryState.Success -> Result.success()
        state is BackupRecoveryState.Failed && state.code == BackupRecoveryState.FAILED_BACKUP_WEBDAV -> Result.retry()
        else -> Result.failure()
    }
    // 不附带 outputData(避免备份路径/异常信息泄露)
}
```
（import `BackupRecoveryState`。退避在 WorkRequest 构建侧:`startUpOneTimeBackupWork`/`startUpPeriodicBackupWork` 的 builder 加 `.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)`。）

- [ ] **Step 5: 运行确认通过** — Run: `:sync:work:testDebugUnitTest` Expected: PASS（3 测全绿）

- [ ] **Step 6: Commit**
```bash
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/AutoBackupWorker.kt sync/work/build.gradle.kts sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/workers/AutoBackupWorkerTest.kt
git commit -m "feat(sync/work): AutoBackupWorker 据备份状态返回 success/retry/failure + 退避 + Worker 测试(#10a)"
```

---

## 最终验证

- [ ] **全量回归**:涉及模块逐个跑 `testDebugUnitTest`(core:model `:test`),全 BUILD SUCCESSFUL。
- [ ] **节点 2 full-review**:对 `git diff main..HEAD` 跑 `comprehensive-review:full-review`,修 Critical/High。
- [ ] **finishing-branch**:FF 合并回 main。

## 已知局限(spec Out of Scope,plan 不实现)

- #10a `startBackup` catch-all 把瞬时 IO 归 UNAUTHORIZED → 被判 failure 不重试(注释标注)。
- `GetRelatedRecordViewsUseCase.queryRelatedRecordCountById` 逐条 N+1、`LauncherContentViewModel` Paging 批量化、#5 Keystore 均不做。
