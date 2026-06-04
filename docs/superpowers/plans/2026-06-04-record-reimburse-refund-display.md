# 记录报销/退款显示逻辑 A/B/D 显示层修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复记账记录在关联报销/退款时的显示逻辑：统一列表/详情关联金额口径（A）、区分报销/退款状态（B）、新增「待报销」标识（D）。

**Architecture:** 在 `core/domain` 的 `RecordModelTransToViewsUseCase` 计算「关联性质」枚举并统一关联金额口径，作为列表/详情的单一数据源；显示层（`feature/records`）按枚举渲染文案。不触碰主金额/实际金额/删除线/月度汇总，与全 App 其它消费方保持同源自洽（C 净额与数据层 finalAmount 重构另立项）。

**Tech Stack:** Kotlin · Jetpack Compose · Hilt · JUnit4 + Truth · Fake Repository（`core/testing`）。

**Spec:** `docs/superpowers/specs/2026-06-04-record-reimburse-refund-display-design.md`（已过节点 1 team-review，finding 处置见 spec §10）。

---

## 文件结构

| 文件 | 职责 | 动作 |
|---|---|---|
| `core/model/.../core/model/enums/RecordRelatedNatureEnum.kt` | 关联性质枚举 | 创建 |
| `core/model/.../core/model/model/RecordViewsModel.kt` | domain 视图模型 + relatedNature 字段 | 修改 |
| `core/model/.../core/model/entity/RecordViewsEntity.kt` | UI 视图实体 + relatedNature 字段 | 修改 |
| `core/model/.../core/model/transfer/ModelTransfer.kt` | Model→Entity 映射 | 修改 |
| `core/domain/.../usecase/RecordModelTransToViewsUseCase.kt` | A 口径统一 + relatedNature 计算 | 修改 |
| `core/domain/.../test/.../RecordModelTransToViewsUseCaseTest.kt` | domain 单测 | 修改 |
| `core/ui/.../res/values/strings_records.xml` | 文案 | 修改 |
| `feature/records/.../screen/LauncherContentScreen.kt` | 列表前缀 B + 待报销标签 D | 修改 |
| `feature/records/.../view/RecordDetailsSheet.kt` | 详情关联金额 A + 状态标注 B | 修改 |
| `feature/records/.../preview/RecordDetailsSheetPreviewParameterProvider.kt` | 详情截图样例 | 修改 |

**约束（team-review 采纳项）**：
- **H1**：新字段给默认值 `= RecordRelatedNatureEnum.NONE`（收敛 ~13 个构造点编译破坏面为 0）；`ModelTransfer` + `RecordModelTransToViewsUseCase` **必须真实赋值**（防默认值掩盖未赋值）。
- **H2**：关联 category 由主 category 取反推断，**禁止 per-record 查库**。
- **M5**：测试/preview fixture 的关联记录 `typeId` 必须用 `FIXED_TYPE_ID_REIMBURSE(-2002)/FIXED_TYPE_ID_REFUND(-2001)`，否则全判 MIXED 致假阳性。
- **M7**：详情关联记录行内联重算改用 `relatedAmount`。
- **L3**：relatedNature 在已物化 `relatedRecord` 上计算，复用现有遍历，勿引入 N+1。

**本机构建约定（CLAUDE.local.md）**：增量验证用 `env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew <task> --offline --no-daemon --console=plain`；只信 `grep -E "^BUILD (SUCCESSFUL|FAILED)"`，不信 bash exit。下文命令省略前缀。

---

## Task 1: 关联性质枚举 + 视图模型字段 + 映射

**Files:**
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/enums/RecordRelatedNatureEnum.kt`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordViewsModel.kt:52`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/RecordViewsEntity.kt:67`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/transfer/ModelTransfer.kt:99`

> 纯结构铺设（无行为逻辑），验证 = 编译通过 + 现有 `core:model` 测试不破。relatedNature 真值在 Task 3 计算，本任务字段一律取默认 NONE。

- [ ] **Step 1: 创建枚举**

`RecordRelatedNatureEnum.kt`（License header 照抄同目录 `RecordTypeCategoryEnum.kt`）：

```kotlin
package cn.wj.android.cashbook.core.model.enums

/**
 * 记录关联性质（仅对被吸收支出有意义）
 * - NONE: 无关联（或非支出主记录）
 * - REIMBURSED: 关联记录全部为报销类型，已报销
 * - REFUNDED: 关联记录全部为退款类型，已退款
 * - MIXED: 报销 + 退款混合，或其它
 */
enum class RecordRelatedNatureEnum {
    NONE,
    REIMBURSED,
    REFUNDED,
    MIXED,
}
```

- [ ] **Step 2: RecordViewsModel 增字段（默认 NONE）**

在 `RecordViewsModel.kt` 的 `relatedAmount: Long,`（:51）后、`recordTime: Long,`（:52）前插入：

```kotlin
    val relatedAmount: Long,
    val relatedNature: cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum =
        cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum.NONE,
    val recordTime: Long,
```

（或在文件顶部 import `RecordRelatedNatureEnum` 后写短名；本仓 `RecordViewsModel.kt` 当前无 import 块，二选一即可。）

- [ ] **Step 3: RecordViewsEntity 增字段（默认 NONE）**

在 `RecordViewsEntity.kt` 的 `relatedAmount: Long,`（:65 注释下方 `val relatedAmount`）后、`recordTime: Long,` 前插入同样字段：

```kotlin
    /** 关联金额，单位：分 */
    val relatedAmount: Long,
    val relatedNature: cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum =
        cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum.NONE,
    /** 记录时间，毫秒时间戳 */
    val recordTime: Long,
```

- [ ] **Step 4: ModelTransfer 真实传递（H1）**

`ModelTransfer.kt` 的 `RecordViewsModel.asEntity()`，在 `relatedAmount = this.relatedAmount,`（:99）后插入：

```kotlin
        relatedAmount = this.relatedAmount,
        relatedNature = this.relatedNature,
        recordTime = this.recordTime,
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:model:test --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（字段有默认值，下游构造点无需改动即编译通过）

> 勘误：`core:model` 为 JVM 库（`cashbook.jvm.library`），测试任务是 `test`，无 `compileDebugKotlin`/`testDebugUnitTest`。

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/enums/RecordRelatedNatureEnum.kt core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordViewsModel.kt core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/RecordViewsEntity.kt core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/transfer/ModelTransfer.kt
git commit -m "[feat|core|model][公共]新增 RecordRelatedNatureEnum + RecordViews 关联性质字段(默认NONE)"
```

---

## Task 2: A — 统一关联金额口径（sumRelatedAmount 取反 recordAmount）

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt:181-196`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCaseTest.kt`

> 现状 EXPENDITURE 主分支用裸 `record.amount`（漏关联收入手续费）；统一为 `recordAmount(取反category)`。INCOME 主分支逐字段不变。

- [ ] **Step 1: 写失败测试（关联收入带手续费）**

在 `RecordModelTransToViewsUseCaseTest.kt` 末尾（`batch_empty_input_returns_empty` 后、类结束 `}` 前）新增：

```kotlin
    @Test
    fun given_expenditure_type_when_related_income_has_charges_then_related_amount_excludes_charges() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 20000L)
        recordRepository.addRecord(record)
        // 关联的收入记录（报销款）带手续费 500
        val relatedRecord = createRecordModel(id = 2L, typeId = 2L, amount = 8000L, charges = 500L)
        recordRepository.addRecord(relatedRecord)
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        val result = useCase(record)

        // A 修复后：支出主记录关联收入 = recordAmount(INCOME) = amount - charges = 8000 - 500 = 7500
        assertThat(result.relatedAmount).isEqualTo(7500L)
    }
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RecordModelTransToViewsUseCaseTest.given_expenditure_type_when_related_income_has_charges*" --offline --no-daemon --console=plain`
Expected: FAIL —— 实际 8000（现状 `Σamount`），期望 7500。

- [ ] **Step 3: 改实现（取反 recordAmount，H2）**

`RecordModelTransToViewsUseCase.kt` 替换 `sumRelatedAmount`（:181-196）整段为：

```kotlin
    /**
     * 计算关联金额，单条与批量共用同一口径，保证两者逐字段等价。
     * 关联 category 由主 category 取反推断（零额外查询）：
     * - 主支出：关联收入，recordAmount(INCOME)=amount−charges
     * - 主收入：关联支出，recordAmount(EXPENDITURE)=amount+charges−concessions
     * - 其它（TRANSFER 等）：不累加
     */
    private fun sumRelatedAmount(
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
```

在文件 import 区加（若未存在）：

```kotlin
import cn.wj.android.cashbook.core.model.model.recordAmount
```

- [ ] **Step 4: 运行验证通过 + 回归**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RecordModelTransToViewsUseCaseTest*" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL` —— 新测试 PASS；既有 `given_income_type...`（关联支出 amount+charges-concessions=10500）、`given_expenditure_type...`（关联收入 amount=8000、charges 默认 0 → 8000-0=8000 不变）、`batch_produces_field_equivalent` 均仍 PASS。

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCaseTest.kt
git commit -m "[fix|core|domain][公共]A:sumRelatedAmount 统一 recordAmount 取反口径(关联收入扣手续费)"
```

---

## Task 3: relatedNature 计算与赋值（单条 + 批量）

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt`（新增私有函数 + 单条 `:65-82` / 批量 `:154-171` 两处赋值）
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCaseTest.kt`

- [ ] **Step 1: 写失败测试（四态，fixture 用固定负 ID — M5）**

在测试类末尾新增（顶部 import 处加 `import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE`、`import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND`、`import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum`）：

```kotlin
    @Test
    fun given_expenditure_related_all_reimburse_then_nature_reimbursed() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        // 关联收入为报销类型（固定负 ID）
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 8000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
    }

    @Test
    fun given_expenditure_related_all_refund_then_nature_refunded() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REFUND, amount = 8000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.REFUNDED)
    }

    @Test
    fun given_expenditure_related_mixed_then_nature_mixed() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 4000L))
        recordRepository.addRecord(createRecordModel(id = 3L, typeId = FIXED_TYPE_ID_REFUND, amount = 3000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L, 3L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.MIXED)
    }

    @Test
    fun given_expenditure_no_related_then_nature_none() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }

    @Test
    fun given_income_absorber_with_related_then_nature_none() = runTest {
        // 收入吸收者（报销款主记录）relatedNature 恒 NONE（仅被吸收支出有性质）
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.INCOME)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 8000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 5L, amount = 10000L))
        recordRepository.setRelatedIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RecordModelTransToViewsUseCaseTest.given_expenditure_related_all_reimburse*" --offline --no-daemon --console=plain`
Expected: FAIL —— 实际 NONE（字段默认值未计算），期望 REIMBURSED。

- [ ] **Step 3: 加计算函数 + 两处赋值**

在 `RecordModelTransToViewsUseCase.kt` 加私有函数（紧邻 `sumRelatedAmount`）：

```kotlin
    /**
     * 计算被吸收支出的关联性质（L3：在已物化 relatedRecord 上判定，零额外查询）。
     * 仅 EXPENDITURE 主记录有性质；relatedRecord 为吸收它的收入（报销/退款款），
     * 其 typeId 经 migrateSpecialTypes 恒为固定负 ID（见 spec §1）。
     */
    private fun computeRelatedNature(
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

import 区加：

```kotlin
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
```

单条路径（`invoke(recordModel)`，`RecordViewsModel(...)` 构造块，`relatedAmount = totalRelated,` 后）插入：

```kotlin
                relatedAmount = totalRelated,
                relatedNature = computeRelatedNature(type.typeCategory, relatedRecord),
                recordTime = recordModel.recordTime,
```

批量路径（`transBatch`，`RecordViewsModel(...)` 构造块，`relatedAmount = totalRelated,` 后）插入同样两行（此处变量名同为 `type`、`relatedRecord`、`totalRelated`，见 `:144-171`）：

```kotlin
                    relatedAmount = totalRelated,
                    relatedNature = computeRelatedNature(type.typeCategory, relatedRecord),
                    recordTime = recordModel.recordTime,
```

- [ ] **Step 4: 运行验证通过 + 回归（含 batch 等价 + 无 N+1）**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RecordModelTransToViewsUseCaseTest*" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL` —— 四态 + income NONE 测试 PASS；`batch_produces_field_equivalent_result_to_single`（单条与批量 relatedNature 一致）PASS；`batch_does_not_query_per_record_but_uses_batch_apis`（无新增查询）PASS。

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCaseTest.kt
git commit -m "[feat|core|domain][公共]计算被吸收支出 relatedNature(报销/退款/混合/无,零查库)"
```

---

## Task 4: B 列表前缀 + D 待报销标签 + 文案

**Files:**
- Modify: `core/ui/src/main/res/values/strings_records.xml`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt:706-746`

- [ ] **Step 1: 新增文案**

`strings_records.xml`（在现有 `reimbursable`/`reimbursed`/`refund_reimbursed_simple` 附近）新增：

```xml
    <string name="refunded">已退款</string>
    <string name="pending_reimbursement">待报销</string>
```

- [ ] **Step 2: 列表前缀按 relatedNature + 待报销标签**

`LauncherContentScreen.kt` 顶部 import 加：

```kotlin
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
```

替换 trailing `Row`（:706-746）内**前缀 Text 块**（现状 `if (item.relatedRecord.isNotEmpty()) { Text(...) }`，:709-722）为：

```kotlin
                    if (item.relatedRecord.isNotEmpty()) {
                        val prefixRes = when (item.relatedNature) {
                            RecordRelatedNatureEnum.REIMBURSED -> R.string.reimbursed
                            RecordRelatedNatureEnum.REFUNDED -> R.string.refunded
                            RecordRelatedNatureEnum.MIXED -> R.string.refund_reimbursed_simple
                            RecordRelatedNatureEnum.NONE -> R.string.related
                        }
                        Text(
                            text = stringResource(id = prefixRes) + "(${item.relatedAmount.toMoneyCNY()})",
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    } else if (item.reimbursable) {
                        Text(
                            text = stringResource(id = R.string.pending_reimbursement),
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
```

> 主金额 `displayAmount` / 删除线 `isReimbursed`（:723-744）**保持现状不动**（C 另立项）。`NONE` 分支保留 `R.string.related` 覆盖收入吸收者（relatedRecord 非空但非支出性质）。

- [ ] **Step 3: 编译 + 测试**

Run: `./gradlew :feature:records:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（现有 `LauncherContentViewModelTest`/截图测试构造 `RecordViewsEntity` 因默认值不破；新增字段渲染分支编译通过）。

- [ ] **Step 4: Commit**

```bash
git add core/ui/src/main/res/values/strings_records.xml feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt
git commit -m "[feat|feature|record-display][公共]B:列表前缀区分报销/退款 + D:待报销标签 + 文案"
```

---

## Task 5: A 详情关联金额单一数据源 + B 详情状态标注

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordDetailsSheet.kt`（金额行 :192-206、关联记录行 :264-302）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/preview/RecordDetailsSheetPreviewParameterProvider.kt`

- [ ] **Step 1: 详情金额行标注按 relatedNature（B）**

`RecordDetailsSheet.kt` 顶部 import 加 `import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum`。替换「金额」CbListItem 内的标注块（现状 `if (recordData.typeCategory == EXPENDITURE && recordData.reimbursable) { ... }`，:192-206）为：

```kotlin
                                val statusRes = when {
                                    recordData.relatedNature == RecordRelatedNatureEnum.REIMBURSED -> R.string.reimbursed
                                    recordData.relatedNature == RecordRelatedNatureEnum.REFUNDED -> R.string.refunded
                                    recordData.relatedNature == RecordRelatedNatureEnum.MIXED -> R.string.refund_reimbursed_simple
                                    recordData.reimbursable -> R.string.pending_reimbursement
                                    else -> null
                                }
                                statusRes?.let { res ->
                                    Text(
                                        text = stringResource(id = res),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
```

> 术语与列表统一：已报销/已退款/已退\|报/待报销。

- [ ] **Step 2: 关联记录行改用 relatedAmount（A / M7）**

替换「关联记录」CbListItem 内 `trailingContent` 的内联重算（现状 `:275-295` 两个 `var total` 累加）为：

```kotlin
                            trailingContent = {
                                val text =
                                    if (recordData.typeCategory == RecordTypeCategoryEnum.INCOME) {
                                        stringResource(id = R.string.related_record_display_format).format(
                                            recordData.relatedRecord.size,
                                            recordData.relatedAmount.toMoneyCNY(),
                                        )
                                    } else {
                                        stringResource(id = R.string.refund_reimbursed_format).format(
                                            recordData.relatedAmount.toMoneyCNY(),
                                        )
                                    }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
```

> 数值不变（详情原本即 `amount−charges`；A 已把 `relatedAmount` 对齐到此口径），仅改为单一数据源。

- [ ] **Step 3: 补 preview 样例（M5：关联记录 typeId 固定负 ID）**

`RecordDetailsSheetPreviewParameterProvider.kt`：将「已报销」样例（`reimbursedExpenditureRecordViewsData` 附近，:75）的关联记录 `relatedRecordList` 改为 `typeId = FIXED_TYPE_ID_REIMBURSE`，并显式设 `relatedNature = RecordRelatedNatureEnum.REIMBURSED`；「退款」样例（:125）关联记录 `typeId = FIXED_TYPE_ID_REFUND`、`relatedNature = RecordRelatedNatureEnum.REFUNDED`；为「待报销」新增一个 `reimbursable = true` 且 `relatedRecord = emptyList()`、`relatedNature = NONE` 的支出样例。import `FIXED_TYPE_ID_REIMBURSE`/`FIXED_TYPE_ID_REFUND`/`RecordRelatedNatureEnum`。

```kotlin
// 「已报销」样例的关联收入记录改为固定报销类型
val relatedRecordList = listOf(
    RecordModel(/* id */ 2L, /* typeId */ FIXED_TYPE_ID_REIMBURSE, /* ...其余沿用原样例参数... */),
)
// 对应 RecordViewsEntity 构造增 relatedNature = RecordRelatedNatureEnum.REIMBURSED
```

> 按文件现有 `RecordModel(...)` 位置参数顺序填写（`RecordModel.kt:37-50`：id, typeId, ...）。退款样例同理换 `FIXED_TYPE_ID_REFUND` + `REFUNDED`。

- [ ] **Step 4: 编译 + 测试**

Run: `./gradlew :feature:records:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（详情截图测试/预览编译通过；`RecordDetailsSheet` 渲染四态）。

- [ ] **Step 5: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordDetailsSheet.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/preview/RecordDetailsSheetPreviewParameterProvider.kt
git commit -m "[fix|feature|record-display][公共]A:详情关联金额复用 relatedAmount + B:状态标注区分报销/退款"
```

---

## Task 6: 全量验证

**Files:** 无（仅验证）

- [ ] **Step 1: 全量单测（含跨模块 H1 构造点）**

Run: `./gradlew :core:model:test :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:assets:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（`feature:assets` / `core:domain` 的 `RecordViewsEntity`/`RecordViewsModel` 构造点因默认值不破）。

- [ ] **Step 2: Spotless 格式**

Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，如有格式化改动一并 commit。

- [ ] **Step 3: 验证 grep — 确认无残留旧口径**

Run: `grep -n "totalRelated += record.amount\b" core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt`
Expected: 无输出（旧 EXPENDITURE 裸 amount 口径已移除）。

Run: `grep -rn "it.amount - it.charges\|it.amount + it.charges - it.concessions" feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordDetailsSheet.kt`
Expected: 无输出（详情两处内联重算已删）。

- [ ] **Step 4: 若有 spotless 改动则提交**

```bash
git add -A
git commit -m "[chore|all|record-display][公共]spotless 格式化"
```

---

## Self-Review 对照（writing-plans）

- **Spec 覆盖**：A→Task 2(口径)+Task 5(详情单一数据源)；B→Task 4(列表)+Task 5(详情标注)；D→Task 4；relatedNature→Task 1(枚举/字段)+Task 3(计算)。C/数据层=spec §8 另立项，本计划不含 ✓
- **采纳 finding**：H1→Task 1(默认值)+Task 6(跨模块验证)；H2→Task 2(取反推断)；M5→Task 3+Task 5(固定负 ID fixture)；M7→Task 5 Step 2；L3→Task 3(已物化遍历)+Task 3 Step 4(N+1 回归) ✓
- **类型一致**：`RecordRelatedNatureEnum{NONE/REIMBURSED/REFUNDED/MIXED}` 全任务统一；`relatedNature` 字段名一致；`computeRelatedNature`/`sumRelatedAmount` 签名一致 ✓
- **无 placeholder**：各 Step 含真实代码/命令/期望输出 ✓
