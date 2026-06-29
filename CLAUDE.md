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

> **单 feature 模块 lint 用 `:feature:X:lintRelease`**（不是 `lintDevRelease`/`lintOnlineRelease`——flavor-specific lint 变体只在 `:app:` 级存在；feature 库模块误用报 `task 'lintDevRelease' not found`，候选 `lintFixRelease`/`lintRelease`）。**首次跑 lint 离线缓存缺 `com.android.tools.lint:lint-gradle`**（在 Google Maven `google()` 非 Maven Central），`--offline` 会报 `No cached version ... available for offline mode` → 去 `--offline` + 清继承代理后加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 从 google() 拉暖缓存（之后增量可 `--offline`）。

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
  | `LinearProgressIndicator` | `CbLinearProgressIndicator` |
  | `CircularProgressIndicator` | `CbCircularProgressIndicator` |
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
- **纯加列 migration（`ALTER TABLE ADD COLUMN`）必须配 `@ColumnInfo(defaultValue = "...")`**：仅加字段（不改类型/不裁列）用纯 `ALTER TABLE db_xxx ADD COLUMN col TYPE NOT NULL DEFAULT v`（**非** `_new` 重建，参见 `Migration13To14` 加 `db_record.reimbursed`）。**关键**：实体字段须写 `@ColumnInfo(name = ..., defaultValue = "v")`，否则 Room 生成的 schema JSON 该列**无 SQL default**，而迁移加了 `DEFAULT v` → `runMigrationsAndValidate` 报 schema 不一致 fail。Room schema 按**列名**校验（非位置），故 ADD COLUMN 追加末列与实体声明顺序无关都匹配。注意 `@ColumnInfo.defaultValue`（Room 建表 SQL 用）与 Kotlin 默认参 `= SWITCH_INT_OFF`（代码层构造 RecordTable 用）**服务不同层、二者都要**。新增列写实体**末位**（避免破坏 `RecordTable(...)` 位置参数构造，如 androidTest helper / PreviewParameterProvider）。每个加列 migration 同样需 `DatabaseTest.migrateX_Y` 守护（建 vX 库插一行→迁移到 vX+1→断言存量行该列为 default）。实证 2026-06-24 待报销手动标记（main 本地 `79a35cdb`，真机 `migrate13_14` + `migrateAll` 1→14 通过）。
- **Route 的 `hiltViewModel().apply { 副作用 }` 默认参须幂等或加 key 守卫**：`@Composable` 函数默认参表达式**每次重组都求值**，`Route(viewModel = hiltViewModel().apply { updateXxx(...) })` 的 `apply` 块每次重组执行。若该方法会写 ViewModel 内的「用户可交互改变」状态（如月份选择 `_dateSelection`），而 Route 又 `collectAsStateWithLifecycle` 收集该状态 → 状态一变就重组 → `apply` 重跑 → **把用户交互结果重置回入口初值**（曾致分类/标签统计翻月失效，main `9d59785c` 修复）。规避：① 副作用方法保持幂等、**不碰用户可交互状态**（参见 `AssetInfoContentViewModel.updateAssetId` 只设 id、不碰 dateSelection，dateSelection 仅 `updateMonth` 改）；② 必须从入口初始化此类状态时，加「入口参数 key 守卫」仅在参数变化时执行一次（如 `appliedDateKey`）。纯 UI 派生值（如 `monthSwitchable = dateSelection is ByMonth`）优先在 Route/Composable **本地派生**，勿建 `WhileSubscribed` stateIn——后者无 collector 时 `.value` 恒为 initialValue（单测/首帧陷阱）。
- **dependabot 依赖升级合并前必查 compileSdk 兼容**：androidx 依赖（core-ktx / lifecycle / activity 等）的某些版本会在 AAR metadata 中声明**消费方最低 compileSdk**。当依赖要求 > 当前 `ProjectSetting.Config.COMPILE_SDK`（现 36）时，AGP `:app:checkXxxAarMetadata` 任务编译期直接 fail（`> N issues were found when checking AAR metadata: Dependency 'X' requires libraries and applications that depend on it to compile against version Y or later`）。**合并 dependabot PR 前必看 CI build 是否绿**，红的若是 `checkAarMetadata` 失败说明依赖要求更高 compileSdk，不可直接合（也不可 `--admin` 绕，会编译阻断 main）。实证 2026-06-23：`--admin` 绕红 CI 合入 #485 core-ktx 1.19.0 + #487 lifecycle 2.11.0（均要求 SDK 37），main 编译阻断，回退至 1.18.0 / 2.9.1 修复（PR #490，main `5e4c6456`）。跟进升 SDK 37 需连带升 AGP（当前 8.12.0 最大推荐 36），属大版本升级专项。
- **依赖回退/baseline 修复必须走 PR 不直接 push main**：`.github/workflows/Build.yaml` 的 Dependency Guard 与 Roborazzi 截图机制**仅在 `event_name == 'pull_request'` 时**自动 `dependencyGuardBaseline` / `recordRoborazziDevDebug` 并 auto-commit 回 PR 分支（51-103 行）。直接 push 到 main 时 fork 检查路径会 `exit 1`，baseline 不会自动重生成。故依赖版本变更（包括回退）必须**开 PR**让 CI 自动回填 baseline，再 `--admin` merge（review_required 单人 owner 无法自我 approve）。
- **改 `.github/workflows/*` 的 PR 需 gh CLI 含 `workflow` scope**：默认 OAuth token 缺该 scope 时 `gh pr merge` 报 `GraphQL: Repository rule violations found · refusing to allow an OAuth App to create or update workflow ... without 'workflow' scope`，`--admin` 也绕不过（与 CLAUDE.local.md「gh token 与 git PAT 不互通」一致）。补 scope：`gh auth refresh -h github.com -s workflow`（设备码授权）。实证 2026-06-23 #486（actions/checkout 6→7）首次合并被拒，补 scope 后成功。
- **app 模块 Gradle compile/test task 必须带 flavor**：app 有 Product Flavor（Online/Offline/Canary/Dev），`:app:compileDebugKotlin` / `:app:testDebugUnitTest` **ambiguous 报错**（候选 compileOnlineDebugKotlin/compileOfflineDebugKotlin/…），必须用带 flavor 的 `:app:compileOnlineDebugKotlin` / `:app:testOnlineDebugUnitTest`；feature/core 模块无 flavor，`compileDebugKotlin` 正常。⚠️ 配置阶段 task 名歧义失败信号 =「BUILD FAILED + 全 up-to-date + 无 `e:` 错误行」（非编译错，是 task 解析失败）。验证 app 整体（含跨模块 Hilt 全图）须用 `:app:compileOnlineDebugKotlin`——各 core/feature 模块单独 `compileDebugKotlin` 不验跨模块 Hilt 图。实证 2026-06-24 预算管理（误用 `:app:compileDebugKotlin` 配置失败 → 改 Online flavor）。
- **DAO 接口新增抽象方法（`@Query`/`@Insert`/`@Upsert` 无 body）须同步 `core/data` test 的 `FakeXxxDao`**：否则该 Fake 报「'FakeXxxDao' is not abstract and does not implement abstract member」→ 整个 `:core:data:compileDebugUnitTestKotlin` 失败。被 `@Transaction` default 方法（有 body）内部调用的新抽象方法尤易漏（如 `TransactionDao.deleteBookTransaction` 内新调 `deleteBudgetsByBookId`，FakeTransactionDao 须补实现）。Fake 实现可留 no-op + 注释「真实语义由 androidTest 真 DAO 验证」（符合「测试替身忠实复刻」——级联/约束真实覆盖在 instrumented 层、非假阳性）。实证 2026-06-24 预算管理。
- **用户主动写动作的失败反馈：ViewModel `suspend` 返 `Boolean` + UI 层 await 决定副作用**：用户主动触发的写库动作（标记已报销/删除等），ViewModel 方法用 `suspend fun xxx(): Boolean`（`try { useCase(...); true } catch (e: CancellationException) { throw e } catch (t: Throwable) { false }`——**CancellationException 必须先于 Throwable rethrow**，否则吞协程取消误判失败），UI 层在 `rememberCoroutineScope().launch` 中 await，**成功才推进副作用**（如 dismiss 弹窗）、失败 Toast/Snackbar 提示重试；替代 `viewModelScope.launch { useCase() }` fire-and-forget（静默吞异常 + 无条件副作用，写失败用户无感知）。ViewModel **不碰 Context/Toast**（保持纯 JVM 可测：测返回值 + 用 `RecordRepository by delegate` 接口委托抛异常装饰器测失败分支），Toast 在 Composable 层（`stringResource` 在 `@Composable` 顶层取、lambda 内用）。⚠️ **抽出的 lambda 变量（函数类型）调用不能用命名参数**：`val fn:(Long,Boolean)->Unit=…; fn(id, reimbursed=true)` 报 `Named arguments are prohibited for function types`，只能位置参数 `fn(id, true)`（仅命名函数支持命名参数）。实证 2026-06-24 标记已报销写失败 Toast（main `ac79b971`/`d28d7cef`）。
- **Route 级带 `hiltViewModel()` 包装的 Composable 归 `screen` 包（非 navigation/view）**：如 `RecordDetailSheetContent`（注入 VM + 编排回调 + 调 view 层 Composable）与 `*Route` 同类，放 `screen` 包。放 `navigation` 包会致消费它的 screen（SearchScreen/TypedAnalyticsScreen）产生 **screen→navigation 反向依赖**；放 `view` 包破坏 view 纯 UI 约定（view 包 Composable 无 hiltViewModel）。screen 包此类 Composable 默认 `internal`，**仅当被 app 模块作 sheet content slot 跨模块直接调用（不经 `NavGraphBuilder.composable<>` 注册）才 public**——public 时加 KDoc 注明原因防误收窄 internal 断 MainApp 编译。实证 2026-06-24 RecordDetailSheetContent 下沉 screen 包消反向依赖（main `ac79b971`）。
- **lint `Design` ban 扩展不误伤 `core/design` 自身**：`core/design` 用 `lintPublish(projects.lint)`（`build.gradle.kts:35`）仅把 `Design` 检查**发布给消费方**、**不约束自身**——故把 Material3 组件加入 `DesignDetector.METHOD_NAMES` ban 清单后，core/design 内部 Cb 封装实现调同名 Material3 组件**不报**（现有 `CbTabRow` 内部调 `TabRow` 即如此，构建不中止反证）。「新建 Cb 封装 + 纳入 lint ban」的正确顺序：先建封装 → **迁移完所有 app/feature/core:ui 消费方裸用** → 再加 ban 清单（否则消费方 `lintRelease` 立即失败）+ `DesignDetectorTest` 补 `expectContains` 用例。实证 2026-06-25 ProgressIndicator 纳入设计系统（main `a5752026`）。
- **截图基线 record 与判失败方法论**：① 判 `verifyRoborazzi` 失败**只信本次 run 的 `testDebugUnitTest` FAILED 标记**（gradle 输出）+ `build/outputs/roborazzi/*_compare.png` 的 **mtime**，**勿被上次 run 残留的 stale compare 图误导**（曾据残留误判「settings 17 + records 1 截图失败」，核查 b-verify3 日志实际仅 assets FAILED、re-record 后 `git status` 0 变化反证基线本就正确，main 2026-06-25）。② 缺失基线判据：`git ls-files <module>/src/test/screenshots/ | wc -l` = 0 即测试代码入库但基线**从未 record**（本地 verify 必失败、Reference 空白；CI 走 record 模式 auto-commit 不暴露）→ `recordRoborazziDebug` 生成 → **record 前先 grep `\.now()` 排查时间脆弱性** → 视觉抽检 New 非塌陷（`android screen capture` 对 Compose 恒全白，靠 Read 基线 PNG）→ `verifyRoborazzi` 0 diff 确认确定性 → 入库。实证 2026-06-25 补齐 assets/core:ui/tags/books 缺失基线 178 张（main `9db577aa`）。
- **config-cache 已开启（`org.gradle.configuration-cache=true`，2026-06-26 起）—— 自定义 task 执行阶段禁访问 `Task.project`**：新增/修改任何自定义 Gradle task（`tasks.register`/`@TaskAction`/`doFirst`/`doLast`）时，**执行阶段(action 体)严禁访问 `Task.project` / `rootDir`(=project.rootDir) / `project.delete` / `project.fileTree` 等 Project 成员**——否则 `cannot serialize 'DefaultProject'` / `Invocation of 'Task.project' at execution time is unsupported` 致构建 fail。正确做法：配置阶段（task 注册的配置 block、`fun Project.xxx()` 顶部）捕获所需值为局部 `val` 或 `@Input` Property，action 体只用捕获值；`project.delete(x)`→`x.deleteRecursively()`（File API），`project.fileTree(...)`→`objectFactory.fileTree().setDir(...)`。配置阶段访问 project **允许**（仅 doFirst/doLast/@TaskAction 执行阶段不可）。验证：`./gradlew help --configuration-cache`（配置阶段 0 problem，但 help 不执行 task）+ **CI 全 task 才是执行阶段权威**（含 release assemble→Outputs copyApk / coverage→Jacoco / androidTest→PrintApk）。实证 2026-06-26 修复 copyLegalDocsToAssets/generateReleaseFile/GenerateBadgingTask/Jacoco 执行阶段 project 访问（main `cb64c0d4`，PR #494 全 task 绿）；已兼容写法参考 `GenerateFlavorTask`（纯 `@Input`/`@OutputDirectory` Property）。
- **KDoc 注释禁用含范围操作符 `..` 的方括号表达 `[xxx..yyy]`**：项目 spotless 用 ktlint(android mode)，ktlint 把 KDoc 内 `[...]` 当作 doc reference link 解析，遇 `[from..today]` 这类含 `..` 的报 `Closing bracket expected` 致 `spotlessApply`/`spotlessCheck` **FAILED——但 Kotlin 编译器不报**（编译通过、仅 spotless 报，易误判为编译问题）。单标识符 `[paramName]`（合法 param link）不受影响。描述区间改用「from 到 today」等无方括号表达。实证 2026-06-26 `ReminderLogic.kt` KDoc `区间 [from..today]` 致 spotless fail（提醒通知功能）。
- **`org.json` 在 JVM 单元测试是 stub（"Method not mocked" RuntimeException），用到须加 test 依赖**：Android 的 `org.json`（`JSONObject`/`JSONArray`）仅**运行时/androidTest** 由系统 `android.jar` 提供真实现；本地 JVM 单测（如 `core:data:testDebugUnitTest`）调它抛 `java.lang.RuntimeException: Method ... not mocked`。修：给该模块加 `testImplementation("org.json:json:20180813")`（真实 jar 在 unit-test classpath 先于 android.jar stub，shadow 掉）。⚠️ **离线要选 metadata 完整缓存的版本**——`20231013` gradle 缓存只有 jar 无 resolution metadata，`--offline` 报 `No cached version ... available for offline mode`；`20180813` 有 `metadata-2.x/descriptors/` 故离线可解析（或先清继承代理 + `-Dhttp.proxyHost=127.0.0.1...` 拉一次暖缓存）。实证 2026-06-27 `SettingsBackupCodec`/`BackupManifest` 用 org.json 编解码（图片备份功能，main `4db4494a`）。
- **Roborazzi 截图基线由 CI 管理，本机不录/不判**：本机 `verifyRoborazzi` 对**未改动**的屏（如 BackupAndRecoveryScreen）仍报 `*_dynamic`（动态配色 Material You）变体 "is changed"——本机渲染与 CI/Linux 录制的基线不一致。故 `.github/workflows/Build.yaml` 在 `event_name == 'pull_request'` 时 `recordRoborazziDevDebug` 并 auto-commit 回 PR 分支。**新增/改动常驻 UI（改基线）时本地只提交代码、不录基线**（本地录出来与 CI 不一致，CI 会重录覆盖）；判截图失败也勿信本机 verify。区别于「测试代码入库但基线从未 record」（`git ls-files <module>/src/test/screenshots/`=0，那种须本地首录）。实证 2026-06-27 备份页加只读说明行（改 BackupAndRecoveryScreen 74 基线）+ 记录详情含图片截图用例，均留 CI 重录。
- **新增深链/快捷方式入口走 `PendingDeepLink` 一次性消费统一模式**：所有深链 extra（`shortcutsType`/`reminderTarget`/`reminderAssetId` 等）由 `MainActivity` 经 `parsePendingDeepLink` 纯函数（app `cn.wj.android.cashbook.ui` 包，reminder 优先 shortcut）解析为**单一 `PendingDeepLink` sealed 态**，传 `MainApp(pendingDeepLink, onConsumePendingDeepLink)`；`MainApp` **单个** `LaunchedEffect(pendingDeepLink, uiState)` 在 `!needRequestProtocol && !needVerity` 门控内 `when(pendingDeepLink)` 导航，**导航后调 `onConsumePendingDeepLink()` 复位 `None` + `intent.removeExtra(...)` 清 extra**；`onNewIntent` 须 `setIntent(intent)` 使 consume 清除作用于最新 intent。**禁止**旧的「深链值持于 `mutableState` 消费后不复位」写法——会致 uiState 重发（needVerity 切换/配置变更/进程恢复）时带**陈旧值弹回**目标页（旧 sticky bug）。消费置于门控通过后天然延后到安全验证通过（顺带防「安全门未过时深链丢失」）。⚠️ `when (val link = pendingDeepLink){}` 的 `link` 作用域仅限 when 体内，块后消费守卫须用 `pendingDeepLink`（写 `link` 编译失败）。实证 2026-06-27（main 本地 `800093ed`/`6f41ec94`，模拟器 journey 验 4 分支+旋转不弹回）。
- **APK 发布命名经 `configureOutputs` renamer，渠道段必须显式拼入且小写**：AGP9 `variant.outputs[].versionName` **不含** flavor `versionNameSuffix`（旧 AGP8 `ApplicationVariant.versionName` 含），故 release APK 命名若仅按 versionName 会致 Online/Offline 同名 `Cashbook_<ver>.apk` 在 `outputs/apk/` 互相覆盖（2 个 APK 退化为 1 个）、且连带击穿应用内检查更新；命名须显式拼 flavor 段。`variant.flavorName` 返回 flavor **枚举常量名**（首字母大写 `Online`/`Offline`/`Canary`/`Dev`，因 `Variants.kt` `create(it.name)`），**必须 `.lowercase()`**——否则 ① 偏离历史 `_online` 格式 ② 击穿 `SettingRepositoryImpl.syncLatestVersion` 大小写敏感 `contains("_online")`/`contains("_canary")` 的检查更新匹配（`firstOrNull` 选不到资产、静默失效）。命名集中在 `buildReleaseApkName` 纯函数（`build-logic/.../Outputs.kt`，**public** 供 `app/build.gradle.kts` 跨脚本类路径调用，`internal` 会编译失败 `Cannot access ... it is internal`；`build-logic/convention` 加 `src/test`+`testImplementation(libs.junit)`+`tasks.withType<Test>{useJUnit()}` JVM 单测守护小写契约 + Online≠Offline 不同名回归）；渠道段须与 `CashbookFlavor.versionNameSuffix` 去前导下划线后一致，新增 flavor 时同步核对。本机验证用 **debug 双渠道**（临时 `condition` 含 debug，命名逻辑 buildType 无关）看 `outputs/apk/` 出 2 个独立产物，避开 release baseline profile GMD 本机挂死（`_pre` tag 只跑 Canary 单渠道、无法复现 online/offline 冲突）。⚠️ `variant.flavorName` 须**配置阶段**(`onVariants` 回调内)捕获为局部 `val`（config-cache：rename 闭包执行阶段只用捕获 String、不访问 variant/project）。实证 2026-06-29 v1.3.0 命名退化（AGP9 改造 `c4200504` 引入）修复 main `cc5526f5`、发版 v1.3.1（`v1.3.1_26062920` Release CI run 28372704996 success）。
