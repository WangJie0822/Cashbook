# Cashbook Android 内部 KMP 化设计

> 日期：2026-07-01
> 状态：design · 节点1四维评审已完成（4 HIGH / 6 MEDIUM / 2 LOW 已纳入修订）
> 范围：仅 Android 单平台内部重构，不新增 iOS/Desktop 编译目标
> **【未验证】KMP plugin + AGP 9.2.1 + config-cache 兼容性——Phase 0 先决验证**
> **执行偏差（2026-07-27）**：T3.3 日期工具**不迁**（YAGNI，17 消费文件全 Android 模块）、`kotlinx-serialization-json` 未落 commonMain（死依赖已删）——本 spec 中「日期工具→shared」「serialization 依赖」相关表述以 plan『执行终局记录』为准

## 背景

Cashbook 当前为纯 Android 应用（Kotlin 2.3.20 + Jetpack Compose + Clean Architecture，25 模块，651 Kotlin 文件），业务逻辑分散在 `core:domain`、`core:model`、`core:common` 中。虽无跨平台编译需求，但基础设施层（DI/Room/DataStore/WorkManager）与 Android 深度耦合。

目标：**Android 单平台先行 KMP 化**——将纯 Kotlin 业务逻辑从 Android 模块中剥离到 KMP 结构的 `commonMain` source set，为未来跨平台扩展预留入口，同时保持 Android 应用现有功能不变。

## 评审发现与修订（2026-07-01 节点1四维评审）

本 spec 已经 feasibility / security / reverse / impact 四维 reviewer 并行评审（4 HIGH / 6 MEDIUM / 2 LOW）。以下关键约束已纳入修订：

| # | 严重度 | 发现 | 处理方式 |
|---|--------|------|---------|
| H1 | HIGH | KMP+AGP+config-cache 兼容性零实证 | Phase 0 设置为硬门槛：通过前不推进 Phase 1；附录 Phase 0 验收标准 |
| H2 | HIGH | java.time 深度嵌入（DateSelectionEntity 字段类型级联 25 模块） | 新增"JVM API 迁移约束"节，java.time 替换策略 + kotlinx-datetime 引入计划 |
| H3 | HIGH | BigDecimal/DecimalFormat 阻塞金额工具迁移 | Phase 3 修订为纯 Long 算术重写策略（非直接迁入） |
| H4 | HIGH | 29 UseCase @Inject 无 Hilt 处理 + Import 规模 1000+（非 ~10-20） | 新增 DI 策略节 + 修正 import 波及范围估计 |
| M1-M6 | MEDIUM | 版本目录缺失/日志库依赖/测试策略未定义等 | 已纳入对应 Phase 步骤 |
| L1-L2 | LOW | 计数偏差（61→29）、scope 遗漏等 | 已修正数据

## 目标与非目标

### 目标
- 新建 `shared/` KMP 模块，承载纯 Kotlin 业务逻辑（commonMain）
- 迁出 `core:model`（60 文件，已无 Android 依赖）→ `shared/commonMain`
- 剥离 `core:domain` 中纯 UseCase 逻辑 → `shared/commonMain`
- 分裂 `core:common` 中纯工具函数 → `shared/commonMain`
- 所有 Android 现有功能、编译、测试保持通过

### 非目标
- 不新增 iOS / Desktop / Web 编译目标
- 不迁移 Compose UI 层（`core:design`、`core:ui`、`feature/*`、`app`）
- 不迁移 Android 专属模块（`core:database`、`core:datastore`、`core:network`、`sync/work`）
- 不做 Room/DataStore/WorkManager 的 KMP 替代方案
- 不改造 build-logic convention plugin 体系（仅增量适配 KMP plugin）

## 目标架构

```
app/                          # 不变：Android Application + Compose UI 入口
feature/*/                    # 不变：Compose 页面（UI 层保持平台特定）
sync/work/                    # 不变：WorkManager（Android 专属后台任务）

shared/                       # 🆕 KMP 结构（当前仅 commonMain + androidMain）
├── build.gradle.kts          # kotlin.multiplatform 插件
├── src/
│   ├── commonMain/kotlin/    # 纯 Kotlin，零平台依赖
│   │   ├── model/            # ← 从 core:model 迁入
│   │   ├── domain/           # ← 从 core:domain 剥离纯 UseCase 逻辑
│   │   └── common/           # ← 从 core:common 剥离纯工具函数（金额/日期/字符串）
│   └── androidMain/kotlin/   # Android 实现（expect actuals + interface bindings）
│       └── （Phase 2/3 按需补充）
│
core/                         # 保持但逐步瘦身
├── model/                    # → 清空，依赖 :shared（或删除模块、统一引 :shared）
├── domain/                   # → 保留 Hilt DI 绑定 + 少量 Android 特定 UseCase
├── data/                     # 不变：Repository 实现（依赖接口而非具体平台类型）
├── database/                 # 不变：Room
├── datastore/                # 不变：DataStore
├── datastore-proto/          # 不变：Proto 定义
├── network/                  # 不变：OkHttp/Retrofit
├── common/                   # → 保留 Android 专属工具（Bitmap/File/Context/Uri）
├── design/                   # 不变：Compose 设计系统
├── ui/                       # 不变：Compose UI 组件
└── testing/                  # → 保留 Android 测试替身（Hilt 相关）
```

## 分阶段实施

### Phase 0：工具链验证

**产物**：可编译的 KMP 模块验证 commit

1. 在 `shared/` 新建最小 KMP 模块（`kotlin.multiplatform` 插件 + `commonMain` + `androidMain`）
2. 验证与现有 AGP 9.2.1 / Kotlin 2.3.20 / Gradle 9.6.0 的兼容性
3. 确认现有 `kotlinx.coroutines`、`kotlinx.serialization` 的 KMP 变体在版本目录中可用
4. 验证 convention plugin `cashbook.jvm.library` 可平滑过渡到 KMP plugin

**风险**：KMP plugin 与现有 build-logic convention plugin 可能存在 source set 冲突——Phase 0 即验证，失败则调整方案

### Phase 1：`core:model` → `shared/commonMain`

**产物**：shared 模块承载 60 个 model 文件，25 个消费模块编译通过

| 步骤 | 内容 | 预估文件变动 |
|------|------|-------------|
| 1.1 | `shared/build.gradle.kts` 配置 `commonMain` dependencies（kotlinx.coroutines, kotlinx.serialization） | 1 |
| 1.2 | 迁 `core/model/src/main/kotlin/` 下全部文件到 `shared/src/commonMain/kotlin/` | 60 |
| 1.3 | 检查并替换 Android 专属注解（如有 `androidx.compose.runtime.Stable` 替换为 Compose runtime KMP 版） | 0-5 |
| 1.4 | 全局替换消费方的 `project(":core:model")` → `project(":shared")` | ~20 |
| 1.5 | `:app:compileOnlineDebugKotlin` + `:core:domain:testDebugUnitTest` + 全量 `testDebugUnitTest` 验证 | — |
| 1.6 | `core:model` 模块清空或变为空壳（依赖 `:shared` 做 re-expose，或直接删除） | 1-2 |

**关键风险**：
- Model 类可能含 `@ColumnInfo`（Room 注解）→ 保持 `core:model` 中或评估是否迁移（Room 注解为纯注解，但依赖 Room 库）
- Compose `@Stable` / `@Immutable` 注解——Compose runtime 已有 KMP 变体，需确认版本对齐

### Phase 2：`core:domain` 剥离

**产物**：纯 UseCase 逻辑迁入 `shared/commonMain/domain`，`core:domain` 保留为 Hilt 胶水层

**实测基线**：`core/domain/src/main/.../usecase/` 共 **29 个文件**（非初始估 61），其中：
- 2 个含 Android 依赖（`GetDefaultAssetUseCase.kt` → `android.content.Context`；`GetCurrentMonthRecordViewsMapUseCase.kt` → `android.util.ArrayMap` + `java.util.Calendar`）
- 3 个含 Android 日志库依赖（`TransRecordViewsToAnalytics*UseCase` → `orhanobut.logger`）
- 24 个纯 Kotlin 可迁入

| 步骤 | 内容 | 预估文件变动 |
|------|------|-------------|
| 2.1 | 审查 29 个 UseCase 文件，分类：纯 Kotlin vs 含 Android/JVM 依赖 | — |
| 2.2 | `GetDefaultAssetUseCase`（Context）→ interface 注入；`GetCurrentMonthRecordViewsMapUseCase`（ArrayMap+Calendar）→ 替换为 `LinkedHashMap` + `kotlinx-datetime` | 2-3 |
| 2.3 | 3 个 `TransRecordViewsToAnalytics*UseCase`（logger）→ 移除 Android 日志调用后迁入 | 3 |
| 2.4 | 24 个纯 Kotlin UseCase 迁入 `shared/commonMain/domain` | 24 |
| 2.5 | `core:domain` 保留为 Hilt 胶水层（~29 个 `@Module`+`@Provides` 文件，按"DI 策略"节方案），依赖 `:shared` | ~29 |
| 2.6 | 更新所有消费模块（feature/*, core/data）的 import 路径（~63 处，分布 35 文件） | ~35 |
| 2.7 | 全量编译 + 单元测试（含 `core:testing` 依赖链适配）验证 | — |

**关键风险**（已实证·四维评审确认）：
- `javax.inject.Inject`（JSR-330 纯注解，KMP 可用）→ 注解本身不阻碍迁移，但 Hilt 不扫描 commonMain → 需 ~29 胶水文件（见"DI 策略"节）
- Repository 接口在 `core:data` ——需确认接口签名不依赖 Android 类型（Phase 2.1 审查时验证）
- `core:testing` 依赖 `core:domain`，迁移时需同步更新其 import 否则阻塞所有 feature 测试编译

### Phase 3：`core:common` 分裂

**产物**：纯工具函数迁入 `shared/commonMain/common`，Android 专属工具保留

**实测基线**：`core/common/src/main/kotlin/` 中 7 个文件含 Android 导入（`Bitmap.kt`、`Resource.kt`、`Intent.kt`、`AppManager.kt`、`DocumentOperationManager.kt`、`Int.kt`、`MyFormatStrategy.kt`）；纯 Kotlin 工具函数约 25-30 文件（含内部传递依赖链需整体确定迁出闭包）。

| 步骤 | 内容 | 预估文件变动 |
|------|------|-------------|
| 3.1 | 金额扩展函数（`Money.kt`、`Number.kt`）→ **重写为纯 Long 算术**（非直接迁入，消除 `BigDecimal`/`DecimalFormat` 依赖）后迁入 `shared/commonMain/common` | 2-3 |
| 3.2 | 日期工具、字符串扩展 → `shared/commonMain/common`（需同步替换 `java.time` → `kotlinx-datetime`） | 5-10 |
| 3.3 | 确定迁出闭包（grep 跨包引用）：ext/、enums/、annotation/ 等纯 Kotlin 子包整体迁出 | 20-30 |
| 3.4 | `Bitmap.kt`、`Resource.kt`、`Intent.kt`、`Int.kt`（Android 资源色/图扩展）、`MyFormatStrategy.kt`（Logcat 日志格式化）、`AppManager.kt`、`DocumentOperationManager.kt` → 保留 `core:common` | — |
| 3.5 | 清理消费方 import 路径（~425 处，分布 179 文件） | ~180 |
| 3.6 | 全量编译 + 单元测试验证 | — |

**关键风险**（已实证）：
- 工具函数存在内部传递依赖（如 `Money.kt` → `Number.kt` → `String.kt` + 枚举/注解包），不可逐文件迁移——Phase 3.3 的闭包分析是关键
- `BigDecimal`/`RoundingMode`/`DecimalFormat` 不可迁入 commonMain——Phase 3.1 为纯 Long 算术重写（项目已有 `toMoneyString()` 纯 Long 实现可参照）

## 关键技术决策

### 0. Phase 0 硬性验收标准（阻塞性门槛）

Phase 0 通过前**不推进 Phase 1**。验收标准：

1. `shared/` 最小 KMP 模块（`commonMain` + `androidMain`）创建并提交
2. `./gradlew :shared:compileKotlinJvm --configuration-cache` → `BUILD SUCCESSFUL` + 配置阶段 0 problem
3. `./gradlew :shared:compileKotlinJvm --configuration-cache` 第二次运行 → 缓存命中（`Reusing configuration cache`）
4. 版本目录新增项提交：`kotlinx-coroutines-core` + `kotlin-multiplatform` plugin ID
5. 一个 trivial `expect/actual`（如 `expect fun platformName(): String`）+ `commonTest` 测试用例通过

若 Phase 0 失败（config-cache 不兼容等），**整个方案降级**为纯 Java/Kotlin library 多模块共享（不引入 KMP plugin），后续 Phase 1-3 的 shared 模块架构需重新评估。

### 1. JVM API 迁移约束（针对 H2/H3）

**不得直接迁入 commonMain 的 JVM API**：

| JVM API | 涉及文件 | KMP 替代 | 迁移时机 |
|---------|---------|---------|---------|
| `java.time.LocalDate/YearMonth/ZoneId` | `DateSelectionEntity.kt`、`Time.kt`、`LunarUtils.kt` 等 10+ 文件 | `kotlinx-datetime`（需先引入版本目录） | **Phase 0 预引入 kotlinx-datetime，Phase 1/2 逐文件替换** |
| `java.math.BigDecimal/RoundingMode` | `Money.kt:19-20`、`Number.kt:19` | 纯 Long 算术（项目已有 `toMoneyString()` 纯 Long 实现可参照） | Phase 3 重写，非直接迁入 |
| `java.text.DecimalFormat` | `Any.kt:21` | 纯 Kotlin 字符串格式化 | Phase 3 重写 |
| `java.util.Calendar` | `GetCurrentMonthRecordViewsMapUseCase.kt:25` | `kotlinx-datetime` | Phase 2 替换 |
| `android.util.ArrayMap` | `GetCurrentMonthRecordViewsMapUseCase.kt:19` | `LinkedHashMap`（标准库） | Phase 2 替换 |
| `com.orhanobut.logger.Printer` | 3 个 `TransRecordViewsToAnalytics*UseCase` | 删除日志调用（调试性日志）或 expect/actual 日志接口 | Phase 2 移除后迁移 |

### 2. DI 策略：UseCase 在 commonMain 中的注入（针对 H4）

全部 29 个 UseCase 使用 `@Inject constructor`（`javax.inject.Inject`，JSR-330 纯注解，KMP 可用）。Hilt KSP 不扫描 KMP `commonMain` source set ——迁出后 `@Inject` 不会被 Hilt 自动发现。

**选定策略：`core:domain` 保留为 Hilt 胶水层，每迁出的 UseCase 在 `core:domain` 中建 1 文件 `@Module` + `@Provides`（或委托包装类）**。预估 ~29 个胶水文件（非 spec 初始估的 ~15）。

备选策略（评估后放弃）：委托模式（UseCase 壳委托纯函数 —— 项目已有惯例，`CLAUDE.md` "抽纯函数"节，但此处 UseCase 已是顶层类，额外委托层增加维护负担）。

### 3. 抽象边界：expect/actual vs interface（遵循 `kotlin-multiplatform` skill）

| 场景 | 策略 | 理由 |
|------|------|------|
| 纯 Kotlin 无平台 API | `commonMain` 直接实现 | 无需抽象 |
| 需要 DI / 需要测试替身 | common interface + platform binding | 可测性 > 编译时分发 |
| 仅值/常量不同 | `expect` val / typealias | 简单编译时特化 |
| 仅 Android 使用 | 保持 androidMain，不建 expect | 避免过早抽象（skill: "Wait until second platform needs it"） |

### 2. 不迁移的边界

依 `kotlin-multiplatform` skill 决策树——以下能力**仅在 Android 上使用、无第二平台需求、或差异过大**，不做抽象：

- **Navigation**（Compose Navigation 2.9.8 已经是平台特定；skill 明确列入 "Never Abstract"）
- **Room / DataStore / WorkManager**（Android 专属框架，迁移时再找 KMP 替代）
- **Compose UI 组件**（`core:design`、`core:ui`——保持平台特定，skill: "Screen layouts → platform-specific only"）
- **权限 / 通知 / 生物识别**（完全平台特定 API）

### 3. dependency 版本对齐

`shared/commonMain` 使用与现有 `libs.versions.toml` 相同版本的 KMP 兼容库：
- `kotlinx-coroutines-core`（已是 multiplatform）
- `kotlinx-serialization-json`（已是 multiplatform）
- `kotlinx-datetime`（按需引入，替代 `java.time` 局部用法）

## 测试策略

迁移过程中的测试覆盖策略（针对 M3）：

### 迁移前（Phase 0）
- 在 shared 模块 `commonTest` 中写一个 `kotlin.test.Test` 验证 KMP 测试运行器可用
- 确认 `kotlin.test` + JUnit4 双框架共存（KMP 模块内用 `kotlin.test`，原 Android 模块保持 JUnit4+Truth）

### 迁移中（Phase 1-3）
- 每 Phase 的编译验证步骤包含 `testDebugUnitTest`（非仅 `compileDebugKotlin`）——确保测试文件 import 路径已同步更新
- `core:testing` 是依赖链关键节点（依赖全部三个待迁移模块 + 被 8 个 feature 模块依赖），每 Phase 必须同步适配：
  - Phase 1：`core:testing` 的 `implementation(projects.core.model)` → 新增 `implementation(projects.shared)`，暂时双依赖过渡
  - Phase 2/3：同理，对 `core:domain`/`core:common` 保留原依赖直到迁移完成

### 回归守护
- `:app:compileOnlineDebugKotlin`（全 Hilt 图验证）作为每 Phase 收尾步骤
- Phase 2/3 完成后跑 DAO instrumented 测试（`:core:database:connectedDebugAndroidTest`）验证数据库层无回归

## 风险总览（修订·四维评审后）

| 风险 | 严重度 | 影响 | 缓解措施 |
|------|--------|------|---------|
| KMP plugin + AGP 9.2.1 + config-cache 兼容性未验证 | **HIGH** | Phase 0 阻塞 → 全方案降级 | Phase 0 硬性验收标准（见"关键技术决策 0"） |
| java.time API 全链路嵌入（DateSelectionEntity 字段类型级联 25 模块） | **HIGH** | 迁移≠搬文件，是全类型重写 + 消费方级联 | Phase 0 预引入 kotlinx-datetime；每 Phase 逐文件替换，保留 java.time 兼容评估 |
| BigDecimal/DecimalFormat 阻塞金额工具迁移 | **HIGH** | Money.kt/Number.kt/Any.kt 不可直接迁 commonMain | Phase 3.1 纯 Long 算术重写（项目已有 `toMoneyString()` 纯 Long 实现可参照） |
| 29 UseCase @Inject 无 Hilt 处理 + Import 规模 1000+ | **HIGH** | DI 注入全链路编译失败 + 批量修改面广 | ~29 胶水文件 + 逐模块 compileDebugKotlin 验证（非批量 sed） |
| 版本目录缺 KMP 依赖声明 | MEDIUM | Phase 0/1 编译阻塞 | Phase 0 新增 `kotlinx-coroutines-core` + `kotlin-multiplatform` plugin ID |
| domain 含 2 处 Android 依赖 + 3 处日志库依赖 | MEDIUM | 低估迁移阻点 | Phase 2.1 grep 审计全覆盖（`android.*`/`java.util.Calendar`/`logger()`） |
| 测试策略未定义 + core:testing 是回归阻断节点 | MEDIUM | 测试源集编译失败 | "测试策略"节 + 每 Phase 双依赖过渡 |
| core:common 工具函数内部传递依赖链 | MEDIUM | 逐文件迁移编译失败 | Phase 3.3 闭包分析（grep 跨包引用） |
| shared/ 模块不受 Android lint 覆盖 | MEDIUM | 代码质量覆盖降低 | 记录为已知影响（纯 Kotlin 逻辑层，lint 实际影响小） |
| sed 批量 import 替换无 diff 审计 guard | LOW | 可能误改源码 | 逐模块 compileDebugKotlin 验证（非批量 sed），每批后 `git diff --stat` 审计 |
| spec 计数偏差（UseCase 61→29、scope 遗漏 Int.kt/MyFormatStrategy.kt） | LOW | 误导工作量预期 | 已修正数据（grep 实证） |
| config-cache 不兼容时 Gradle 9 不支持按模块禁用 | LOW | 需全项目关闭 config-cache | Phase 0 验证；不兼容则降级方案或全项目 `--no-configuration-cache` |
| compose_compiler_config / dependency-guard 绕过 | LOW | 边缘影响 | Phase 0 后验证全限定名 + shared 模块配置 dependencyGuard |

## 工作量（修订·四维评审后）

| 阶段 | 文件变动 | 主导风险 | 会话预期 |
|------|---------|---------|---------|
| Phase 0 | ~10 文件（验证模块+版本目录+PoC expect/actual） | H1 工具链兼容性 | 单会话：验证 + 通过/降级决策（**硬门槛**） |
| Phase 1 | ~100 文件（60 迁入 + 20 build + ~20 test import） | java.time 替换 + 811 import 适配 | 单会话：迁移 + 编译 + 测试验证 |
| Phase 2 | ~120 文件（24 UseCase 迁入 + 29 胶水 + 35 import + ~30 test） | DI 策略 + 日志库移除 | 单会话：审计+剥离+编译+测试 |
| Phase 3 | ~230 文件（30 迁入 + 180 import + 20 闭包分析） | Long 算术重写 + 闭包依赖 | 单会话：分裂+重写+编译+测试 |
| **合计** | **~460 文件** | H1 通过为前提 | 4 会话串行（Phase 0→1→2→3），每 Phase 产出 1+ commit |

> 修订说明：初始估 ~190 文件基于"直接搬文件"假设。四维评审实证发现：(a) java.time/java.math 需重写而非搬迁、(b) import 波及 ~1000 条（非 ~30）、(c) DI 胶水需 ~29 额外文件、(d) 测试文件未纳入。修订后 ~460 文件为更接近实际的估计。按 Claude 工作流：每 Phase 产物和依赖明确，串行可验证。
