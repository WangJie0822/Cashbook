# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Cashbook 是一个 Android 记账应用，使用 Kotlin + Jetpack Compose 构建，采用多模块架构。支持多账本、资产管理、标签、统计图表、WebDAV 备份等功能。

## 常用命令

### 构建

```bash
# 构建所有 flavor 的 APK
./gradlew :app:assemble

# 构建特定 flavor (Online/Offline/Canary/Dev) + buildType (Debug/Release)
./gradlew :app:assembleOnlineDebug
./gradlew :app:assembleOfflineRelease
```

### 代码格式检查 (Spotless)

```bash
# 检查格式
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache

# 自动修复格式
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache

# build-logic 的格式修复
./gradlew -p build-logic spotlessApply --init-script ../gradle/init.gradle.kts --no-configuration-cache
```

### Lint

```bash
./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true
```

### 测试

```bash
# 单元测试 (指定 flavor)
./gradlew testOnlineDebugUnitTest
./gradlew testOfflineDebugUnitTest

# 运行单个模块的测试（⚠️ 模块类型决定测试任务名）
# JVM 库（cashbook.jvm.library，如 core:model）用 :test，无 compileDebugKotlin/testDebugUnitTest（误用会报 task not found）
./gradlew :core:model:test
# Android 库（cashbook.android.library，如 core:data/core:domain/feature:*）用 :testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:records:testDebugUnitTest

# 截图测试 (Roborazzi)
./gradlew verifyRoborazziOnlineDebug   # 校验
./gradlew recordRoborazziOnlineDebug   # 生成基准截图
```

## 架构

### 分层结构

```
app → feature/* → core/*
```

- **app**: 主入口，Hilt Application，导航设置，多渠道配置
- **feature/**: 功能模块 (tags, types, books, assets, records, settings)
- **core/**: 基础模块

### 核心模块职责

| 模块 | 职责 |
|------|------|
| `core/domain` | UseCase 业务逻辑 |
| `core/data` | Repository 数据仓库 |
| `core/database` | Room 数据库，schema 在 `core/database/schemas/` |
| `core/datastore` | Proto DataStore 偏好存储 |
| `core/datastore-proto` | Proto 定义 (JVM 库) |
| `core/network` | Retrofit + OkHttp 网络层 |
| `core/model` | 数据模型 (JVM 库，Compose 稳定性标注) |
| `core/design` | 设计系统、主题、通用 Composable |
| `core/ui` | 业务相关 UI 组件 |
| `core/common` | 公共工具、常量、BuildConfig |
| `core/testing` | 测试工具、自定义 TestRunner |

### Convention Plugins (build-logic/)

所有模块配置通过 `build-logic/convention/` 下的约定插件统一管理，插件 ID 定义在 `gradle/convention.versions.toml`，以 `cashbook.*` 为前缀。Feature 模块统一使用 `cashbook.android.library.feature` 插件，自动包含 Compose、Hilt、Jacoco 及 `:core:design`、`:core:ui` 依赖。

### 关键技术栈

- **DI**: Hilt (通过 KSP)
- **UI**: Jetpack Compose (BOM 管理版本)
- **导航**: Navigation Compose + Hilt Navigation
- **数据库**: Room
- **网络**: Retrofit 3 + OkHttp + Kotlin Serialization
- **异步**: Kotlin Coroutines + Flow
- **同步**: WorkManager (`sync/work` 模块)
- **图表**: Compose Canvas 自绘制 (`core/design` 中的 CbPieChart、CbLineChart)

### 金额约定（强制）

- 数据库及全链路金额统一使用 **`Long` 类型，单位：分**（1 元 = 100）
- `RecordTable.amount`、`finalAmount`、`concessions`、`charge` 以及 `AssetTable.balance` 均为 `Long`
- 外部输入（如导入账单的 `Double` 元值）必须通过 `Double.toCent()` 或 `String.toAmountCent()` 转换为分再存入数据库（工具方法在 `core/common/ext/Money.kt`）
- 计算 `recordAmount` 应复用 `TransactionDao.calculateRecordAmount()` 方法，禁止自行用 `BigDecimal` / `Double` 重新实现
- UI 显示时使用 `Long.toMoneyString()` / `Long.toMoneyFormat()` / `Long.toMoneyCNY()` 转回元
- **金额计算两口径不可混用**（`core/model/.../model/RecordAmount.kt`）：`recordAmount(category, amount, charges, concessions)` 为 DAO/月度结余口径（INCOME=amount−charges；EXPENDITURE/**TRANSFER**=amount+charges−concessions，转账当支出）；`analyticsPieAmount(typeCategory, ...)` 为 Analytics 饼图口径（EXPENDITURE=amount+charges−concessions；INCOME/**TRANSFER**=amount−charges，转账当收入）。两函数签名完全相同、对 TRANSFER 处理相反，选错只会静默算错——`TransactionDao.calculateRecordAmount`/`GetAssetMonthSummaryUseCase` 用 `recordAmount`，两个 `TransRecordViewsToAnalyticsPie(Second)UseCase` 用 `analyticsPieNetAmount`
- **`finalAmount` 为净自付语义（2026-06-08 重构，main `7114045e`）**：被报销/退款吸收的支出存「净自付额」= recordAmount − 被对冲额（**≥0**）；报销/退款款（吸收者）存「溢出额」= max(0, recordAmount − 对冲额)（**≥0**，通常 0）；未吸收记录 = recordAmount；转账 = concessions − charge。**全部非负，禁止重引入旧吸收模型**（被吸收支出=0 / 吸收者 = recordAmount−Σ被吸收 可负——会污染月度分项统计）。增删改由 `TransactionDao.recalculateFinalAmountForCluster`（BFS 吸收簇 + 吸收者 id 升序顺序贪心填充）维护，全量重算走 `recalculateAllFinalAmount`（迁移 gate / 备份恢复复用，二者同算法同序保证增量=全量）。吸收边界：仅报销款(`-2002`)/退款款(`-2001`) INCOME 能吸收 EXPENDITURE（`TypeRepositoryImpl.needRelated`），关系表 `db_record_with_related` 单向二部图（`record_id`=吸收者收入、`related_record_id`=被吸收支出）。
- **饼图第三口径 `analyticsPieNetAmount(typeCategory, finalAmount, amount, charges, concessions)`**（`RecordAmount.kt`，2026-06-08 新增）：EXPENDITURE/INCOME 用 `finalAmount`（净自付，被吸收支出按净额计入分类占比、报销款溢出不虚增收入），TRANSFER 仍委托 `analyticsPieAmount`（=amount−charges，守 #10b 金丝雀，TRANSFER 两口径不可统一）。两个 `TransRecordViewsToAnalyticsPie(Second)UseCase` 已切此口径；`analyticsPieAmount` 仍被其 TRANSFER 分支复用，非 dead code。

### 多渠道 (Product Flavors)

- **Online**: 在线版 (支持网络更新检查)
- **Offline**: 离线版
- **Canary**: 预览版
- **Dev**: 开发版

### 签名配置

本地构建 Release 需要在 `gradle/signing.versions.toml` 中配置签名信息（该文件不入库）。

## 代码规范

- 源文件需包含 Apache 2.0 License Header（Spotless 自动检查，模板在 `spotless/` 目录）
- Kotlin 格式化使用 ktlint (android mode)
- 自定义 Lint 规则在 `lint/` 模块，通过 `:core:design` 发布
- **禁止在 `app/`、`feature/`、`core/ui/` 中直接使用以下 Material3 组件，必须使用 `core/design` 中对应的设计系统封装：**

  | Material3 组件 | 项目封装 |
  |---|---|
  | `MaterialTheme()` | `CashbookTheme` |
  | `TopAppBar` | `CbTopAppBar` |
  | `Scaffold` | `CbScaffold` |
  | `Divider` | `CbHorizontalDivider` / `CbVerticalDivider` |
  | `TextField` | `CbTextField` |
  | `OutlinedTextField` | `CbOutlinedTextField` |
  | `FloatingActionButton` | `CbFloatingActionButton` |
  | `SmallFloatingActionButton` | `CbSmallFloatingActionButton` |
  | `ListItem` | `CbListItem` |
  | `ModalBottomSheet` | `CbModalBottomSheet` |
  | `TextButton` | `CbTextButton` |
  | `IconButton` | `CbIconButton` |
  | `Card` | `CbCard` |
  | `ElevatedCard` | `CbElevatedCard` |
  | `AlertDialog` | `CbAlertDialog` |
  | `BaseAlterDialog` | `CbBaseAlterDialog` |
  | `Tab` | `CbTab` |
  | `TabRow` | `CbTabRow` |
  | `Icons` | `CbIcons` |

  > 访问 `MaterialTheme.colorScheme`/`.typography`/`.shapes` 属性是允许的，仅禁止作为 composable 调用。
  > 违反此规则会触发 lint `Design` error，构建将中止。
- `compose_compiler_config.conf` 声明了 `core/model` 中模型类的 Compose 稳定性
- 测试使用自定义 TestRunner: `cn.wj.android.cashbook.core.testing.CashbookTestRunner`
- 包名: `cn.wj.android.cashbook`

## 规范（强制）
- 所有修改新增功能必须确认是否新增对应测试，功能开发必须在测试通过才算完成
- 修改 Composable 或 ViewModel 的签名（参数增删）时，必须同步更新该模块 `src/test` 下对应的截图测试（`*ScreenshotTests`）与 `*ViewModelTest` 的构造/调用——模块测试源集整体编译，任一测试文件签名不匹配会导致整个模块 `testDebugUnitTest` 编译失败（既往多次踩坑：feature:settings 的 BackupAndRecoveryScreen/ViewModel 签名漂移、feature:records/assets 截图测试）
- 测试替身（`core/testing` 的 `FakeXxxRepository` / `core/data` test 的 `FakeXxxDao`）的方法必须**忠实复刻真实 DAO/SQL 的匹配语义**，禁止用 `emptyList()` 桩或宽松 `contains` 替代真实条件——否则该路径成"假阳性覆盖"（测试绿但真实代码不这样执行，回归抓不到）。既往踩坑：`queryByTimeAndAmount`（元/分单位错配桩使去重永不命中）、`queryByWechatTransactionId`（`emptyList` 桩使 EXACT 路径从未覆盖 + 裸 `contains` 偏离真实 `remark LIKE '%[微信单号:<id>]%'` 方括号定界），均在评审时才暴露
- 注：`core/data` 的 test 源集**不依赖 `core:testing`**（仅 junit+truth），各测试文件自带 `private fun createXxx` 构造数据；`core/domain`/`feature` 的 test 才用 `core:testing` 的 `FakeXxxRepository`/`createXxxModel`
- 