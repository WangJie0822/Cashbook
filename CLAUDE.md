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

# DAO instrumented 测试（需模拟器/真机；如 TransactionDao 余额回退/批量删/回滚真机验证）
./gradlew :core:database:connectedDebugAndroidTest
```

> **`connectedDebugAndroidTest` 首次运行需联网拉 UTP（Unified Test Platform）依赖**（`_internal-unified-test-platform-*` 配置，offline 缓存无，故不能加 `--offline`）。本机经代理拉 Maven Central（repo1.maven.org）时注意 TLS 传输稳定性——探活 `curl -x 代理 -sI` 返回 `200 Connection established` 仅代表 CONNECT 隧道建立，**不代表能完整下载**（实下载 `curl -o` 才暴露隧道内 HTTPS 传输被 reset、`HTTP=000`）。代理传输不稳时 instrumented 测试无法跑，属环境问题（非代码）。

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
- **`finalAmount` 为净自付语义（2026-06-08 重构，main `7114045e`）**：被报销/退款吸收的支出存「净自付额」= recordAmount − 被对冲额（**≥0**）；报销/退款款（吸收者）存「溢出额」= max(0, recordAmount − 对冲额)（**≥0**，通常 0）；未吸收记录 = recordAmount；转账 = concessions − charge。**全部非负，禁止重引入旧吸收模型**（被吸收支出=0 / 吸收者 = recordAmount−Σ被吸收 可负——会污染月度分项统计）。增删改由 `TransactionDao.recalculateFinalAmountForCluster`（= `discoverClusterIds` BFS 发现簇+`outEdges` 缓存 + `recalculateFinalAmountFromCluster` 吸收者 id 升序顺序贪心填充；2026-06-11 F-1 拆分以消 N+1/2x BFS，main `4c0dad0b`）维护；删账本/删资产走 `deleteRecordsBatch`（逐条 `deleteRecordCore` 余额回退+清关联+删、**无重算** + 删后对存活簇去重重算一次，消 O(N²)；单删 `deleteRecordTransaction` 委托之），全量重算走 `recalculateAllFinalAmount`（迁移 gate / 备份恢复复用，二者同算法同序保证增量=全量）。吸收边界：仅报销款(`-2002`)/退款款(`-2001`) INCOME 能吸收 EXPENDITURE（`TypeRepositoryImpl.needRelated`），关系表 `db_record_with_related` 单向二部图（`record_id`=吸收者收入、`related_record_id`=被吸收支出）。
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
- **`CbTabRow`（或任何内容）放进 `CbTopAppBar` 的 `title` 槽时，modifier 必须用 `Modifier.fillMaxWidth()`，禁止 `fillMaxSize()`**：`fillMaxSize` 含 `fillMaxHeight`，在 Material3 `TopAppBar` title 槽下会使 TopAppBar 按 title 撑满全屏高度、`CbScaffold` body 区塌陷为 0 高 → 该屏内容（如分类网格）完全不渲染、tab 浮于屏幕垂直中部。曾致 Critical bug（记账/我的分类支出分类不渲染、无法记账，main `20a0e502` 修复，回归测试 `a0190d5e` 用 `assertIsDisplayed` 守护）。lint 不覆盖此 modifier 误用，靠本规则 + 回归测试防回归。**新增此类「topbar title 内 tab 行」优先用 `core/design` 的 `CbTabTopAppBar`（fillMaxWidth 已写死在封装内、不暴露 modifier，从 API 层杜绝误用；ARCH-1 引入），勿再手写 title 内 CbTabRow。**
- **抽屉/导航回调聚合阈值**：当同一组相关点击/事件回调 **≥4 个** 且需跨 **≥3 层** Composable/navigation 透传时，聚合为单个 `XxxActions` data class 整体透传，避免逐参透传导致的签名漂移与参数爆炸；`dismiss`/导航后副作用在 `Route` 层用 `wrap` 包装（先执行回调再副作用）。**聚合类置于 `navigation` 包**（紧邻消费它的 `xxxScreen` navigation 函数）。被 public 跨模块 navigation 函数（如 `settingsLauncherScreen`）引用时**必须 public**，否则 `compileDebugKotlin` 报 `'public' function exposes its 'internal' parameter type`。实证：M1 抽屉 7 回调 4 层透传聚合为 `LauncherDrawerActions`（2026-06-22 main `49ac8c60`）。
- `compose_compiler_config.conf` 声明了 `core/model` 中模型类的 Compose 稳定性
- 测试使用自定义 TestRunner: `cn.wj.android.cashbook.core.testing.CashbookTestRunner`
- 包名: `cn.wj.android.cashbook`

## 规范（强制）
- 所有修改新增功能必须确认是否新增对应测试，功能开发必须在测试通过才算完成
- 修改 Composable 或 ViewModel 的签名（参数增删）时，必须同步更新该模块 `src/test` 下对应的截图测试（`*ScreenshotTests`）与 `*ViewModelTest` 的构造/调用——模块测试源集整体编译，任一测试文件签名不匹配会导致整个模块 `testDebugUnitTest` 编译失败（既往多次踩坑：feature:settings 的 BackupAndRecoveryScreen/ViewModel 签名漂移、feature:records/assets 截图测试）
- 测试替身（`core/testing` 的 `FakeXxxRepository` / `core/data` test 的 `FakeXxxDao`）的方法必须**忠实复刻真实 DAO/SQL 的匹配语义**，禁止用 `emptyList()` 桩或宽松 `contains` 替代真实条件——否则该路径成"假阳性覆盖"（测试绿但真实代码不这样执行，回归抓不到）。既往踩坑：`queryByTimeAndAmount`（元/分单位错配桩使去重永不命中）、`queryByWechatTransactionId`（`emptyList` 桩使 EXACT 路径从未覆盖 + 裸 `contains` 偏离真实 `remark LIKE '%[微信单号:<id>]%'` 方括号定界），均在评审时才暴露
- 注：`core/data` 的 test 源集**不依赖 `core:testing`**（仅 junit+truth），各测试文件自带 `private fun createXxx` 构造数据；`core/domain`/`feature` 的 test 才用 `core:testing` 的 `FakeXxxRepository`/`createXxxModel`
- **抽纯函数便于单测的惯例**：为可测性把逻辑从 ViewModel/全局耦合中抽出为纯函数时，优先 **top-level `internal fun`**（同模块 test 源集可直接调用、无需实例化宿主类或反射私有成员）；仅当该逻辑强依赖宿主类的私有状态/常量、抽成 top-level 反而需传一长串参数时，才放 `companion object` 内 `internal fun`。原 `private` 方法保留为对纯函数的薄委托，维持调用方与行为不变。实证：`computeNeedUpdate` 纯函数 + `private needUpdate` 委托之（main `f7b548ca`）。
- **数据库表重建 migration 用 `_new` 四步紧邻模式**：SQLite 重建表（改列类型/裁字段）时，今后新增 migration 统一采用 `建新表 db_xxx_new → INSERT...SELECT 拷数据 → DROP 旧表 db_xxx → ALTER db_xxx_new RENAME TO db_xxx` 四步紧邻写法（参见 `Migration11To12`），**禁止** `rename 旧表→db_xxx_temp → 建新表 → 拷数据 → 末尾 DROP temp` 的旧式 rename-to-temp 模式——后者 DROP 与创建/拷贝在视觉上分离、收尾 DROP 易漏（曾致 `Migration6To7.migrateRecord` 漏删 `db_record_temp`，`runMigrationsAndValidate(validateDroppedTables=true)` 报 `Unexpected table`，main `f4cc7514` 修复）。**已发布的历史 migration 不得为统一模式而重写**（线上已执行、不可变），本约定仅约束今后新增。每个表重建 migration 必须有 `DatabaseTest.migrateX_Y` 真机测试守护（带 `validateDroppedTables=true` 校验无残留临时表）。
- **Route 的 `hiltViewModel().apply { 副作用 }` 默认参须幂等或加 key 守卫**：`@Composable` 函数默认参表达式**每次重组都求值**，`Route(viewModel = hiltViewModel().apply { updateXxx(...) })` 的 `apply` 块每次重组执行。若该方法会写 ViewModel 内的「用户可交互改变」状态（如月份选择 `_dateSelection`），而 Route 又 `collectAsStateWithLifecycle` 收集该状态 → 状态一变就重组 → `apply` 重跑 → **把用户交互结果重置回入口初值**（曾致分类/标签统计翻月失效，main `9d59785c` 修复）。规避：① 副作用方法保持幂等、**不碰用户可交互状态**（参见 `AssetInfoContentViewModel.updateAssetId` 只设 id、不碰 dateSelection，dateSelection 仅 `updateMonth` 改）；② 必须从入口初始化此类状态时，加「入口参数 key 守卫」仅在参数变化时执行一次（如 `appliedDateKey`）。纯 UI 派生值（如 `monthSwitchable = dateSelection is ByMonth`）优先在 Route/Composable **本地派生**，勿建 `WhileSubscribed` stateIn——后者无 collector 时 `.value` 恒为 initialValue（单测/首帧陷阱）。
- **dependabot 依赖升级合并前必查 compileSdk 兼容**：androidx 依赖（core-ktx / lifecycle / activity 等）的某些版本会在 AAR metadata 中声明**消费方最低 compileSdk**。当依赖要求 > 当前 `ProjectSetting.Config.COMPILE_SDK`（现 36）时，AGP `:app:checkXxxAarMetadata` 任务编译期直接 fail（`> N issues were found when checking AAR metadata: Dependency 'X' requires libraries and applications that depend on it to compile against version Y or later`）。**合并 dependabot PR 前必看 CI build 是否绿**，红的若是 `checkAarMetadata` 失败说明依赖要求更高 compileSdk，不可直接合（也不可 `--admin` 绕，会编译阻断 main）。实证 2026-06-23：`--admin` 绕红 CI 合入 #485 core-ktx 1.19.0 + #487 lifecycle 2.11.0（均要求 SDK 37），main 编译阻断，回退至 1.18.0 / 2.9.1 修复（PR #490，main `5e4c6456`）。跟进升 SDK 37 需连带升 AGP（当前 8.12.0 最大推荐 36），属大版本升级专项。
- **依赖回退/baseline 修复必须走 PR 不直接 push main**：`.github/workflows/Build.yaml` 的 Dependency Guard 与 Roborazzi 截图机制**仅在 `event_name == 'pull_request'` 时**自动 `dependencyGuardBaseline` / `recordRoborazziDevDebug` 并 auto-commit 回 PR 分支（51-103 行）。直接 push 到 main 时 fork 检查路径会 `exit 1`，baseline 不会自动重生成。故依赖版本变更（包括回退）必须**开 PR**让 CI 自动回填 baseline，再 `--admin` merge（review_required 单人 owner 无法自我 approve）。
- **改 `.github/workflows/*` 的 PR 需 gh CLI 含 `workflow` scope**：默认 OAuth token 缺该 scope 时 `gh pr merge` 报 `GraphQL: Repository rule violations found · refusing to allow an OAuth App to create or update workflow ... without 'workflow' scope`，`--admin` 也绕不过（与 CLAUDE.local.md「gh token 与 git PAT 不互通」一致）。补 scope：`gh auth refresh -h github.com -s workflow`（设备码授权）。实证 2026-06-23 #486（actions/checkout 6→7）首次合并被拒，补 scope 后成功。
- 