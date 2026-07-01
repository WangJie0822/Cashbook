# Cashbook Android 内部 KMP 化设计

> 日期：2026-07-01
> 状态：design
> 范围：仅 Android 单平台内部重构，不新增 iOS/Desktop 编译目标

## 背景

Cashbook 当前为纯 Android 应用（Kotlin 2.3.20 + Jetpack Compose + Clean Architecture，25 模块，651 Kotlin 文件），业务逻辑分散在 `core:domain`、`core:model`、`core:common` 中。虽无跨平台编译需求，但基础设施层（DI/Room/DataStore/WorkManager）与 Android 深度耦合。

目标：**Android 单平台先行 KMP 化**——将纯 Kotlin 业务逻辑从 Android 模块中剥离到 KMP 结构的 `commonMain` source set，为未来跨平台扩展预留入口，同时保持 Android 应用现有功能不变。

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

| 步骤 | 内容 | 预估文件变动 |
|------|------|-------------|
| 2.1 | 审查 61 个 UseCase，分类：纯 Kotlin vs 含 Android 依赖 | — |
| 2.2 | `GetDefaultAssetUseCase`（依赖 Context）→ 抽象 interface 注入 | 2-3 |
| 2.3 | 纯 UseCase 迁入 `shared/commonMain/domain` | 40-50 |
| 2.4 | `core:domain` 保留为薄壳（Hilt DI 绑定 + 剩余 Android 特定 UseCase），依赖 `:shared` | ~15 |
| 2.5 | 更新所有消费 `core:domain` 的模块（feature/*, core/data）的 import 路径 | ~10 |
| 2.6 | 全量编译 + 单元测试验证 | — |

**关键风险**：
- UseCase 中可能隐式依赖 `javax.inject.Inject`（JSR-330，纯注解无平台依赖）→ 不阻碍迁移
- 部分 UseCase 通过 Hilt 注入 `Repository` 接口——Repository 接口本身在 `core:data`，需确认接口签名不依赖 Android 类型

### Phase 3：`core:common` 分裂

**产物**：纯工具函数迁入 `shared/commonMain/common`，Android 专属工具保留

| 步骤 | 内容 | 预估文件变动 |
|------|------|-------------|
| 3.1 | 金额扩展函数（`Money.kt`、`Amount.kt`及其依赖）→ `shared/commonMain/common` | 5-8 |
| 3.2 | 日期工具、字符串扩展 → `shared/commonMain/common` | 5-10 |
| 3.3 | `Bitmap.kt`、`Resource.kt`、`Intent.kt`、`File.kt` → 保留 `core:common`（Android 专属） | — |
| 3.4 | 清理消费方的 import 路径（`core:common` → `:shared` 或保持原路径） | ~20 |
| 3.5 | 全量编译 + 单元测试验证 | — |

**关键风险**：
- 工具函数之间可能存在内部依赖（如金额工具依赖字符串工具），需整体迁入避免循环引用
- `DocumentOperationManager`、`AppManager` 等强依赖 Context 的类保持不动

## 关键技术决策

### 1. 抽象边界：expect/actual vs interface（遵循 `kotlin-multiplatform` skill）

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

## 风险总览

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| KMP plugin 与 AGP 9.2.1 不兼容 | Phase 0 阻塞 | Phase 0 即验证，失败则降级为纯 Java/Kotlin library 多模块共享 |
| core:model 含 Room 注解（`@ColumnInfo` 等） | Phase 1 需额外处理 | 事前 Grep 审查，可在 shared 中 add Room 注解依赖或保留在 core:model |
| 消费方 import 大面积漂移 | 编译失败面广 | 批量 sed + 逐模块 `compileDebugKotlin` 验证 |
| convention plugin 适配 KMP 成本高 | build-logic 改动大 | shared 模块先用原始 KMP plugin 不用 convention plugin，后续再封装 |
| config-cache 兼容性 | `org.gradle.configuration-cache=true` 下 KMP plugin 可能不兼容 | Phase 0 验证，不兼容则 shared 模块 `--no-configuration-cache` 或修复（按 config-cache 契约：执行阶段禁访问 Task.project） |

## 工作量

| 阶段 | 文件变动 | 会话预期 |
|------|---------|---------|
| Phase 0 | ~5 文件（验证用临时模块） | 单会话：验证 + 决策（是否继续/调整） |
| Phase 1 | ~80 文件（60 迁入 + 20 build 调整） | 单会话：迁移 + 编译验证 + 测试 |
| Phase 2 | ~70 文件（50 迁入 + 15 壳 + 10 import） | 单会话：排查 + 剥离 + 编译验证 |
| Phase 3 | ~40 文件（15 迁入 + 20 import + 5 保留） | 单会话：分裂 + 编译验证 |
| **合计** | **~190 文件** | 4 个会话串行（Phase 0→1→2→3），每 Phase 产出 1+ commit |

> 按 Claude 工作流评估：非人力工时，每 Phase 产物明确、依赖关系串行可验证。
