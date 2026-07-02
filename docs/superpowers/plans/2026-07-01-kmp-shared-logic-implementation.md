# Cashbook Android 内部 KMP 化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Cashbook 纯 Kotlin 业务逻辑从 `core:model`/`core:domain`/`core:common` 剥离到新建的 KMP `shared/` 模块 `commonMain` source set，为未来跨平台扩展预留入口，Android 现有功能不退化。

**Architecture:** 新建 `shared/` KMP 模块（`commonMain` + `androidMain`），分 4 阶段渐进迁移：Phase 0 工具链验证（硬门槛）→ Phase 1 core:model 60 文件迁入 → Phase 2 core:domain 29 文件剥离（含 DI 胶水层）→ Phase 3 core:common 分裂。shared 模块用原始 KMP plugin（不经 convention plugin），package 声明不随物理迁移改变（零 import 变更策略）。

**Tech Stack:** Kotlin 2.3.20 · AGP 9.2.1 · Gradle 9.6.0 · KMP plugin (`org.jetbrains.kotlin.multiplatform`) · kotlinx-coroutines-core · kotlinx-serialization-json · kotlinx-datetime · Hilt 2.59.2（仅 androidMain 侧）

**Spec:** `docs/superpowers/specs/2026-07-01-kmp-shared-logic-design.md`
**Design Doc:** 同上

## Global Constraints

- `org.gradle.configuration-cache=true` —— 全程必须兼容 config-cache（复现 Phase 0 验收标准 #2-3）
- `org.gradle.parallel=false` —— 串行构建，不启用并行
- `org.gradle.workers.max=2` —— 最大 worker 数
- Package 声明不随物理迁移改变 —— 沿用 `cn.wj.android.cashbook.core.model/domain/common` 包名，Phase 1 零 import 变更
- shared 模块用原始 KMP plugin（不经 build-logic convention plugin 封装）—— 仅增量适配，不改造 convention plugin 体系
- 所有 Phase 收尾步骤必须跑 `:app:compileOnlineDebugKotlin`（全 Hilt 图验证）
- 每 Phase 必须 `testDebugUnitTest` 通过（非仅 compileDebugKotlin）
- `core:testing` 每 Phase 须双依赖过渡（保持对旧模块的依赖 + 新增对 `:shared` 的依赖）
- 金额计算全链路不改语义（Long 分单位约定保持 `Money.kt` 中 `String.toAmountCent()`/`Double.toCent()` 行为一致）
- java.time → kotlinx-datetime 替换必须保持 API 语义等价（尤其 `DateSelectionEntity.toDateRange()` 的 `ZoneId.systemDefault()` → `TimeZone.currentSystemDefault()`）

---

## Phase 0 实证发现（2026-07-02·已通过硬门槛，勘误下游）

Phase 0 实测已通过（config-cache stored+reused、PlatformTest 通过），H1 HIGH 风险解决。实证发现修正了 plan 原假设，下游 Phase 1-3 须按以下事实执行：

1. **shared 模块 build 配置**（非 plan 原写的 `kotlin.multiplatform` + `androidTarget()`）：AGP 9 禁止 `com.android.library` + `kotlin.multiplatform` 联用，`androidTarget()` 报「requires Android Gradle Plugin」。实证正确配置：
   ```kotlin
   plugins {
       alias(libs.plugins.kotlin.multiplatform)
       alias(libs.plugins.android.kotlin.multiplatform.library)  // AGP9 KMP 库插件
   }
   kotlin {
       androidLibrary {
           namespace = "cn.wj.android.cashbook.shared"
           compileSdk = 36
           minSdk = 24
           withHostTestBuilder {}  // 启用 JVM host 跑 commonTest
       }
       sourceSets { commonMain.dependencies { ... }; commonTest.dependencies { implementation(kotlin("test")) } }
   }
   ```
2. **root `build.gradle.kts` 须加**：`alias(libs.plugins.kotlin.multiplatform) apply false` + `alias(libs.plugins.android.kotlin.multiplatform.library) apply false`（否则「plugin already on classpath with unknown version」冲突）。
3. **版本目录须加**：`android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "android-gradle-plugin" }`（已在 Task 0.1/0.3 提交）。
4. **shared 模块编译/测试 task 名**：`:shared:testAndroidHostTest`（跑 commonTest）；**非** plan 各处写的 `:shared:compileKotlinAndroid`——下游所有 `:shared:compile*` 验证命令替换为 `:shared:testAndroidHostTest` 或 `:shared:assemble`。
5. **commonMain 不含 java.time**（JVM-only API）——凡迁入 commonMain 的文件（core:model / 工具函数）必须先做 java.time → kotlinx-datetime 替换（T1.3/T3.3 已含）。

## Phase 0：工具链验证（硬门槛·不通过则全方案降级）

### Task 0.1: 版本目录新增 KMP 依赖声明

**Files:**
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: `libs.kotlinx.coroutines.core`、`libs.kotlin.multiplatform` plugin、`libs.kotlinx.datetime`

- [ ] **Step 1: 在 `[libraries]` 段新增 KMP 依赖**

在 `gradle/libs.versions.toml` 的 `[libraries]` 段末尾（`kotlinx-coroutines-android` 附近）新增：

```toml
# KMP shared 模块依赖（Phase 0 工具链验证）
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version = "0.6.2" }
```

- [ ] **Step 2: 在 `[plugins]` 段新增 KMP plugin ID**

在 `gradle/libs.versions.toml` 的 `[plugins]` 段末尾新增：

```toml
# KMP shared 模块插件（Phase 0 工具链验证）
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

- [ ] **Step 3: 确认 `javax.inject` 已可用**

```bash
grep 'javax-inject' gradle/libs.versions.toml
```
若无条目，新增：
```toml
javax-inject = { group = "javax.inject", name = "javax.inject", version = "1" }
```

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "[build|deps|KMP][公共]Phase0: 版本目录新增 KMP 依赖声明（kotlinx-coroutines-core + kotlinx-datetime + kotlin-multiplatform plugin）"
```

---

### Task 0.2: 创建 shared/ KMP 模块骨架

**Files:**
- Create: `shared/build.gradle.kts`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces: `:shared` Gradle 模块，含 `commonMain` + `androidMain` source set

- [ ] **Step 1: 创建 `shared/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // 仅 Android 目标（未来可扩展 iosMain/desktopMain）
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

- [ ] **Step 2: 在 `settings.gradle.kts` 注册 shared 模块**

在 `settings.gradle.kts` 的 `include` 块中（`include(":core:model")` 附近）新增：

```kotlin
include(":shared")
```

- [ ] **Step 3: 创建 source 目录结构**

```bash
mkdir -p shared/src/commonMain/kotlin
mkdir -p shared/src/androidMain/kotlin
mkdir -p shared/src/commonTest/kotlin
```

- [ ] **Step 4: Commit**

```bash
git add shared/build.gradle.kts settings.gradle.kts shared/src/
git commit -m "[build|shared|KMP][公共]Phase0: 创建 shared/ KMP 模块骨架（commonMain+androidMain）"
```

---

### Task 0.3: 验证 config-cache 兼容性 + KMP 编译

**Files:**
- Create: `shared/src/commonMain/kotlin/Platform.kt`
- Create: `shared/src/androidMain/kotlin/Platform.android.kt`
- Create: `shared/src/commonTest/kotlin/PlatformTest.kt`

**Interfaces:**
- Produces: 验证通过日志（`BUILD SUCCESSFUL` + `Reusing configuration cache`）
- Consumes: Task 0.2 的 shared 模块

- [ ] **Step 1: 创建 expect/actual PoC：`Platform.kt`**

写入 `shared/src/commonMain/kotlin/Platform.kt`：

```kotlin
package cn.wj.android.cashbook.shared

expect fun platformName(): String
```

- [ ] **Step 2: 创建 androidMain actual：`Platform.android.kt`**

写入 `shared/src/androidMain/kotlin/Platform.android.kt`：

```kotlin
package cn.wj.android.cashbook.shared

actual fun platformName(): String = "Android"
```

- [ ] **Step 3: 创建 commonTest：`PlatformTest.kt`**

写入 `shared/src/commonTest/kotlin/PlatformTest.kt`：

```kotlin
package cn.wj.android.cashbook.shared

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun `platform name is non-empty`() {
        val name = platformName()
        assertNotNull(name)
        assertTrue(name.isNotEmpty())
    }
}
```

- [ ] **Step 4: 首次编译（config-cache 冷启动）**

```bash
./gradlew :shared:compileKotlinAndroid --configuration-cache --no-daemon --console=plain 2>&1 | tee /tmp/phase0-build1.log
```

验证标准：
- `grep 'BUILD SUCCESSFUL' /tmp/phase0-build1.log` 有输出
- 配置阶段无 `cannot serialize` / `Configuration cache problems` 错误

- [ ] **Step 5: 二次编译（config-cache 热命中）**

```bash
./gradlew :shared:compileKotlinAndroid --configuration-cache --no-daemon --console=plain 2>&1 | tee /tmp/phase0-build2.log
```

验证标准：
- `grep 'BUILD SUCCESSFUL' /tmp/phase0-build2.log` 有输出
- `grep 'Reusing configuration cache' /tmp/phase0-build2.log` 有输出（缓存命中）

- [ ] **Step 6: 运行 commonTest**

```bash
./gradlew :shared:allTests --no-daemon --console=plain 2>&1 | tee /tmp/phase0-test.log
```

验证标准：
- `grep 'BUILD SUCCESSFUL' /tmp/phase0-test.log` 有输出
- `PlatformTest.platform name is non-empty` PASS

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/ shared/src/androidMain/ shared/src/commonTest/
git commit -m "[test|shared|KMP][公共]Phase0: expect/actual PoC + commonTest 通过，config-cache 兼容性验证通过"
```

---

### Task 0.4: Gate check —— 决策继续或降级

**不做代码变更。仅基于 Phase 0 验证结果做决策。**

- [ ] **Step 1: 检查 Phase 0 验收标准**

对照 spec 的 5 条验收标准逐条确认：
1. `shared/` 模块创建并提交 ✅/❌
2. 首次 `compileKotlinAndroid --configuration-cache` → `BUILD SUCCESSFUL` + 0 problem ✅/❌
3. 二次编译 → `Reusing configuration cache` 缓存命中 ✅/❌
4. 版本目录新增项已提交 ✅/❌
5. expect/actual + commonTest 通过 ✅/❌

- [ ] **Step 2a: 全部通过 → 继续 Phase 1**

```bash
git commit --allow-empty -m "[gate|KMP][公共]Phase0 验收通过：KMP+AGP9.2.1+config-cache 兼容，推进 Phase 1"
```

- [ ] **Step 2b: 任一失败 → 降级方案**

若 config-cache 不兼容（二次编译未缓存命中 / 有 `cannot serialize` 异常）：
- 降级为纯 `kotlin.jvm` library 多模块共享（不使用 KMP plugin）
- Phase 1-3 的 shared 模块架构需重新评估——**停止执行本 plan，回到 spec 修订**
- 记录降级原因 commit：
```bash
git commit --allow-empty -m "[gate|KMP][公共]Phase0 降级：KMP+config-cache 不兼容（<具体原因>），方案降级为纯 JVM library 共享"
```

---

## Phase 1：core:model → shared/commonMain

### Task 1.1: 审计 core:model 文件依赖

- [ ] **Step 1: 确认零 Android 依赖**

```bash
grep -r 'import android\.' core/model/src/main/kotlin/ | grep -v '//' || echo "PASS: zero Android imports in core:model"
grep -r 'import androidx\.' core/model/src/main/kotlin/ | grep -v '//' || echo "PASS: zero AndroidX imports in core:model"
```

- [ ] **Step 2: 确认零 Room 注解**

```bash
grep -rE '@(ColumnInfo|Entity|Database|Dao|Query|Insert|Update|Delete)' core/model/src/main/kotlin/ || echo "PASS: zero Room annotations in core:model"
```

- [ ] **Step 3: 确认零 Compose 注解**

```bash
grep -rE '@(Stable|Immutable)' core/model/src/main/kotlin/ || echo "PASS: zero Compose stability annotations in core:model"
```

- [ ] **Step 4: 检出 java.time 引用清单**

```bash
grep -rn 'java\.time\.' core/model/src/main/kotlin/ > /tmp/phase1-java-time-refs.txt
cat /tmp/phase1-java-time-refs.txt
```
记录涉及的 model 类（预计 `DateSelectionEntity.kt` 及其关联文件）。

- [ ] **Step 5: Commit audit results**

```bash
git add /tmp/phase1-java-time-refs.txt  # 不 commit 临时文件，仅记录
git commit --allow-empty -m "[audit|core:model|KMP]Phase1: core:model 审计通过——零 Android/AndroidX/Room/Compose 注解依赖，java.time 引用清单已记录"
```

---

### Task 1.2: 迁 core:model 文件到 shared/commonMain

**Files:**
- Move: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/` → `shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model/`
- Move: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/` → `shared/src/commonTest/kotlin/cn/wj/android/cashbook/core/model/`
- Modify: `shared/build.gradle.kts`

- [ ] **Step 1: 迁移 source 文件（保持 package 不变）**

```bash
# 创建目标目录
mkdir -p shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model

# 复制全部 model source 文件（保持 package 声明不变 = 零 consumer import 变更）
cp -r core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/* \
     shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model/

# 验证文件数
echo "Source files migrated: $(ls shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model/ | wc -l)"
```

- [ ] **Step 2: 迁移测试文件到 commonTest**

```bash
mkdir -p shared/src/commonTest/kotlin/cn/wj/android/cashbook/core/model
cp -r core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/* \
     shared/src/commonTest/kotlin/cn/wj/android/cashbook/core/model/

echo "Test files migrated: $(ls shared/src/commonTest/kotlin/cn/wj/android/cashbook/core/model/ | wc -l)"
```

- [ ] **Step 3: 验证 shared 模块编译**

```bash
./gradlew :shared:compileKotlinAndroid --no-daemon --console=plain 2>&1 | tee /tmp/phase1-compile.log
grep 'BUILD SUCCESSFUL' /tmp/phase1-compile.log
```

如有编译错误，记录并逐一修复（预期：若 java.time 在 `DateSelectionEntity.kt` 中使用，kotlinx-datetime 替换按 Task 1.3 执行）。

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/ shared/src/commonTest/
git commit -m "[feat|shared|KMP][公共]Phase1: core:model source+test 文件迁入 shared/commonMain（package 声明不变）"
```

---

### Task 1.3: java.time → kotlinx-datetime 替换（DateSelectionEntity 聚焦）

> **注意**：此 Task 仅处理 `shared/commonMain` 中的文件。`core:model` 原文件暂保留（回退用），Phase 1 全量验证通过后再清理。

- [ ] **Step 1: 替换 DateSelectionEntity 的 java.time 导入**

对 `shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntity.kt` 执行以下替换：

| 原 (`java.time`) | 替换为 (`kotlinx.datetime`) |
|---|---|
| `import java.time.LocalDate` | `import kotlinx.datetime.LocalDate` |
| `import java.time.YearMonth` | `import kotlinx.datetime.YearMonth` |
| `import java.time.ZoneId` | `import kotlinx.datetime.TimeZone` |

- [ ] **Step 2: 重写 API 调用**

| 原调用 | 替换为 |
|--------|--------|
| `ZoneId.systemDefault()` | `TimeZone.currentSystemDefault()` |
| `LocalDate.now()` | `Clock.System.todayIn(TimeZone.currentSystemDefault())` |
| `YearMonth.now()` | `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let { YearMonth(it.year, it.monthNumber) }` |
| `LocalDate.of(y, m, d)` | `LocalDate(y, m, d)` |
| `localDate.atStartOfDay(zone)` | `localDate.atStartOfDayIn(zone)` |

- [ ] **Step 3: 替换测试文件中的 java.time**

对 `shared/src/commonTest/` 下 `DateSelectionEntityTest.kt`、`DateSelectionEntityMonthCycleTest.kt` 执行同样替换。

- [ ] **Step 4: 验证 shared 模块编译**

```bash
./gradlew :shared:compileKotlinAndroid --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)|e: '
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/ shared/src/commonTest/
git commit -m "[refactor|shared|KMP][公共]Phase1: DateSelectionEntity java.time → kotlinx-datetime 替换（含测试）"
```

---

### Task 1.4: 更新消费方模块引用

**Files:**
- Modify: 18 个消费模块的 `build.gradle.kts`（`implementation(projects.core.model)` → `implementation(projects.shared)`）
- Modify: `core/testing/build.gradle.kts`（双依赖过渡）
- Modify: `core/model/build.gradle.kts`（清空为壳）

- [ ] **Step 1: 批量替换 Gradle 依赖声明**

```bash
# 找出所有引用 core:model 的 build.gradle.kts
grep -rl 'projects\.core\.model' --include='*.kts' . | grep -v '.claude/' | grep -v '.git/'

# 逐个模块替换（手工逐文件 Edit，非批量 sed——避免误改）
# 每模块编辑后立即 compileDebugKotlin 验证
```

需替换的模块列表（18 个·待实测确认）：
- `app/build.gradle.kts`
- `core/domain/build.gradle.kts`
- `core/data/build.gradle.kts`
- `core/database/build.gradle.kts`
- `core/datastore/build.gradle.kts`
- `core/network/build.gradle.kts`
- `core/common/build.gradle.kts`
- `core/testing/build.gradle.kts`
- `core/ui/build.gradle.kts`
- `core/design/build.gradle.kts`
- `feature/tags/build.gradle.kts`
- `feature/types/build.gradle.kts`
- `feature/books/build.gradle.kts`
- `feature/assets/build.gradle.kts`
- `feature/records/build.gradle.kts`
- `feature/settings/build.gradle.kts`
- `feature/record-import/build.gradle.kts`
- `feature/budget/build.gradle.kts`

每个模块的 `implementation(projects.core.model)` 替换为 `implementation(projects.shared)`。

- [ ] **Step 2: core:testing 双依赖过渡**

修改 `core/testing/build.gradle.kts`：

```kotlin
// Phase 1: 双依赖过渡（待 core:model 清空后可移除 projects.core.model）
implementation(projects.core.model)  // 保留·过渡期
implementation(projects.shared)       // 新增
```

- [ ] **Step 3: 逐模块验证编译**

每替换一个模块后立即：
```bash
./gradlew :<module>:compileDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 4: Commit**

```bash
git add -u
git commit -m "[build|deps|KMP][公共]Phase1: 18 个消费模块 project(:core:model) → project(:shared)，core:testing 双依赖过渡"
```

---

### Task 1.5: 全量编译 + 测试 + 清理 core:model

- [ ] **Step 1: 全量 compile + test**

```bash
./gradlew :app:compileOnlineDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
./gradlew testDebugUnitTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 2: 验证 core:model 可以清空**

```bash
# 确认所有 consumer 已切到 :shared
grep -r 'projects\.core\.model' --include='*.kts' . | grep -v '.claude/' | grep -v '.git/' | grep -v 'core/model/'
# 预期：仅剩 core:testing 的过渡依赖 + core/model/build.gradle.kts 自身
```

- [ ] **Step 3: 清理 core:model 为壳**

修改 `core/model/build.gradle.kts`，移除所有 source 依赖，仅保留：
```kotlin
plugins {
    id("cashbook.jvm.library")
}

dependencies {
    api(projects.shared)  // 透传 shared 的 model 类
}
```

删除 `core/model/src/main/kotlin/` 下的已迁移文件：
```bash
rm -rf core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/*
```

- [ ] **Step 4: 再次全量验证**

```bash
./gradlew :app:compileOnlineDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
./gradlew testDebugUnitTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 5: Commit**

```bash
git add -u core/model/
git add core/model/build.gradle.kts
git commit -m "[refactor|core:model|KMP][公共]Phase1 完成：core:model 清空为壳（api projects.shared 透传），全量编译+测试通过"
```

---

## Phase 2：core:domain 剥离

### Task 2.1: UseCase 审计与分类

- [ ] **Step 1: 列出全部 UseCase 文件**

```bash
ls core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/ | wc -l
# 预期：29
```

- [ ] **Step 2: 检出 Android 依赖**

```bash
grep -rn 'import android\.' core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/
# 预期 2 处：GetDefaultAssetUseCase(Context) + GetCurrentMonthRecordViewsMapUseCase(ArrayMap)
```

- [ ] **Step 3: 检出 Android 日志库依赖**

```bash
grep -rn 'logger()' core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/
# 预期 3 处：TransRecordViewsToAnalyticsPie/Bar/PieSecond
```

- [ ] **Step 4: 检出 java.util/JVM 依赖**

```bash
grep -rnE 'import java\.(util\.Calendar|util\.Date)' core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/
# 预期 1 处：GetCurrentMonthRecordViewsMapUseCase(Calendar)
```

- [ ] **Step 5: 分类清单写入文件并 commit**

```bash
cat > /tmp/phase2-classification.md << 'EOF'
# Phase 2 UseCase 分类

## 纯 Kotlin（24 个·直接迁入 shared/commonMain）
<列出 24 个无 Android 依赖的 UseCase 文件名>

## 需重构（5 个·重构后迁入）
1. GetDefaultAssetUseCase - Context → interface 注入
2. GetCurrentMonthRecordViewsMapUseCase - ArrayMap → LinkedHashMap / Calendar → kotlinx-datetime
3. TransRecordViewsToAnalyticsPieUseCase - 移除 logger()
4. TransRecordViewsToAnalyticsBarUseCase - 移除 logger()
5. TransRecordViewsToAnalyticsPieSecondUseCase - 移除 logger()
EOF

# 分类清单不 commit，仅作实施参考
```

- [ ] **Step 6: Commit audit**

```bash
git commit --allow-empty -m "[audit|core:domain|KMP]Phase2: 29 UseCase 审计——24 纯 Kotlin + 5 需重构（2 Android/3 logger）"
```

---

### Task 2.2: 重构 5 个含 Android/JVM 依赖的 UseCase

- [ ] **Step 1: GetCurrentMonthRecordViewsMapUseCase —— 替换 ArrayMap + Calendar**

修改 `core/domain/src/main/.../usecase/GetCurrentMonthRecordViewsMapUseCase.kt`：
- `import android.util.ArrayMap` → `import java.util.LinkedHashMap`
- `ArrayMap<LocalDate, ArrayList<RecordViewsEntity>>()` → `LinkedHashMap<LocalDate, ArrayList<RecordViewsEntity>>()`
- `import java.util.Calendar` → 用 `kotlinx-datetime` API 替换 `Calendar.getInstance()` 逻辑（具体替换视 L72-76 的实际代码而定）

- [ ] **Step 2: GetDefaultAssetUseCase —— Context → interface 注入**

将 `(context: Context)` 参数替换为抽象的 interface：
```kotlin
// shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/model/AssetDefaultsProvider.kt
interface AssetDefaultsProvider {
    fun defaultAssetName(): String
    fun defaultAssetIcon(): String
}

// core/domain 的 UseCase 改为接受 AssetDefaultsProvider
class GetDefaultAssetUseCase @Inject constructor(
    private val assetDefaultsProvider: AssetDefaultsProvider
)
```

- [ ] **Step 3: 移除 3 个 Analytics UseCase 的 logger() 调用**

对以下 3 个文件，删除 `.logger().i(...)` 调用（调试性日志，非业务逻辑）：
- `TransRecordViewsToAnalyticsPieUseCase.kt`
- `TransRecordViewsToAnalyticsBarUseCase.kt`
- `TransRecordViewsToAnalyticsPieSecondUseCase.kt`

- [ ] **Step 4: 验证 core:domain 编译**

```bash
./gradlew :core:domain:compileDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/
git commit -m "[refactor|core:domain|KMP]Phase2: 重构 5 个 UseCase——替换 ArrayMap/Calendar/Context/logger，消除 Android/JVM 依赖"
```

---

### Task 2.3: 迁纯 UseCase 到 shared/commonMain + 建 Hilt 胶水层

- [ ] **Step 1: 迁移 24+5 个已重构 UseCase 到 shared**

```bash
mkdir -p shared/src/commonMain/kotlin/cn/wj/android/cashbook/domain/usecase

# 迁入全部 29 个 UseCase（保持 package 不变）
cp core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/*.kt \
   shared/src/commonMain/kotlin/cn/wj/android/cashbook/domain/usecase/
```

- [ ] **Step 2: 为 29 个 UseCase 创建 Hilt 胶水文件**

在 `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/di/` 下创建 `SharedUseCaseModule.kt`：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SharedUseCaseModule {
    // 为每个迁出的 UseCase 创建 @Provides
    // 示例（每个 UseCase 对应一个 @Provides 方法）：
    @Provides
    fun provideGetBudgetProgress(getBudgetProgress: GetBudgetProgressUseCase): GetBudgetProgressUseCase = getBudgetProgress

    @Provides
    fun provideGetDefaultAsset(getDefaultAsset: GetDefaultAssetUseCase): GetDefaultAssetUseCase = getDefaultAsset

    // ... 其余 27 个 UseCase
}
```

- [ ] **Step 3: 验证 shared + core:domain 编译**

```bash
./gradlew :shared:compileKotlinAndroid :core:domain:compileDebugKotlin --no-daemon --console=plain
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/ core/domain/src/main/
git commit -m "[feat|shared|KMP][公共]Phase2: 29 UseCase 迁入 shared/commonMain + Hilt 胶水模块 SharedUseCaseModule"
```

---

### Task 2.4: 更新消费方 + core:testing 适配 + 全量验证

- [ ] **Step 1: 更新 feature/* 和 core/data 的 import（~63 处）**

feature 模块中 ViewModel 的 `import cn.wj.android.cashbook.core.domain.usecase.XxxUseCase` 保持不变（package 未变，文件在 shared 但 import 仍命中同名 package）。

检查是否有需更新的 consumer：
```bash
grep -rn 'cn.wj.android.cashbook.core.domain.usecase' --include='*.kt' feature/ app/ core/ | grep -v '.claude/' | grep -v shared/
# 预期：大部分 import 无需变更（package 声明不变），仅需确认编译通过
```

- [ ] **Step 2: core:testing 双依赖过渡**

修改 `core/testing/build.gradle.kts`，`implementation(projects.core.domain)` 旁新增 `implementation(projects.shared)`。

- [ ] **Step 3: 全量 compile + test**

```bash
./gradlew :app:compileOnlineDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
./gradlew testDebugUnitTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 4: 清理 core:domain source 文件**

```bash
# 删除已迁移到 shared 的 UseCase 源文件
rm core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/*.kt
# 保留 Hilt 胶水模块（SharedUseCaseModule.kt）
```

- [ ] **Step 5: 再次全量验证**

```bash
./gradlew :app:compileOnlineDebugKotlin testDebugUnitTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 6: Commit**

```bash
git add -u core/domain/ core/testing/ feature/ core/data/
git commit -m "[refactor|core:domain|KMP][公共]Phase2 完成：UseCase 迁 shared，core:domain 保留 Hilt 胶水层，全量编译+测试通过"
```

---

## Phase 3：core:common 分裂

### Task 3.1: 闭包分析 —— 确定迁出文件集

- [ ] **Step 1: 检出 Android 依赖文件**

```bash
grep -rln 'import android\.' core/common/src/main/kotlin/
# 预期 7 个：Bitmap.kt, Resource.kt, Intent.kt, Int.kt, MyFormatStrategy.kt, AppManager.kt, DocumentOperationManager.kt
```

- [ ] **Step 2: 检出 JVM 专属依赖**

```bash
grep -rlnE 'import java\.(math\.|text\.|util\.Calendar|util\.Date)' core/common/src/main/kotlin/
# 预期：Money.kt(BigDecimal), Number.kt(BigDecimal), Any.kt(DecimalFormat)
```

- [ ] **Step 3: 分析跨文件引用（闭包）**

```bash
# 枚举/注解/工具包的引用关系
grep 'import cn.wj.android.cashbook.core.common\.\(ext\|enums\|annotation\|util\|tools\)' \
  core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ext/*.kt
```

确定"迁出闭包"的最小文件集——包含所有被迁出文件传递引用的枚举/注解/工具。

- [ ] **Step 4: 输出分类清单**

| 类别 | 文件 | 处理 |
|------|------|------|
| 迁出（纯 Long 重写） | Money.kt, Number.kt | Task 3.2 |
| 迁出（含 kotlinx-datetime） | Time.kt, tools/Time.kt, LunarUtils.kt | Task 3.3 |
| 迁出（纯 Kotlin） | String.kt, Flow.kt, 及枚举/注解包 | Task 3.3 |
| 保留（Android 专属） | Bitmap.kt, Resource.kt, Intent.kt, Int.kt, MyFormatStrategy.kt, AppManager.kt, DocumentOperationManager.kt | 不动 |

- [ ] **Step 5: Commit**

```bash
git commit --allow-empty -m "[audit|core:common|KMP]Phase3: 闭包分析完成——确定迁出文件集"
```

---

### Task 3.2: Money.kt + Number.kt 纯 Long 算术重写

- [ ] **Step 1: 审查现有纯 Long 实现**

`core/common/ext/Money.kt` 中已有 `toMoneyString()` 纯 Long 实现。参照此模式重写 `String.toAmountCent()` 和 `Double.toCent()` 的 BigDecimal 段为纯 Long/字符串算术。

- [ ] **Step 2: 重写 String.toAmountCent()**

```kotlin
// shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/common/ext/Money.kt

// 原：BigDecimal(scala=0, HALF_UP)
// 改为：字符串解析 + 纯 Long 算术
fun String.toAmountCent(): Long {
    val cleaned = this.trim()
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return 0L

    val parts = cleaned.split(".")
    val integerPart = parts[0].toLongOrNull() ?: 0L
    val decimalPart = when {
        parts.size > 1 -> parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: 0L
        else -> 0L
    }

    val sign = if (integerPart < 0 || cleaned.startsWith("-")) -1L else 1L
    return sign * (kotlin.math.abs(integerPart) * 100 + decimalPart)
}
```

- [ ] **Step 3: 重写 Double.toCent()**

```kotlin
// 用 roundToLong() 替代 BigDecimal(HALF_UP)
fun Double.toCent(): Long = (this * 100).roundToLong()
```

- [ ] **Step 4: 重写 Any.kt 中的 DecimalFormat 部分**

`decimalFormat()` 和 `moneyFormat()` 函数需用纯 Kotlin 字符串格式化重写（`String.format`/`buildString`）。

- [ ] **Step 5: 将重写后的 Money.kt + Number.kt 迁入 shared**

```bash
mkdir -p shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/common/ext
cp ... # 迁入重写后的文件
```

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/ core/common/src/main/
git commit -m "[refactor|shared|KMP][公共]Phase3: Money.kt/Number.kt 纯 Long 算术重写迁入 shared/commonMain"
```

---

### Task 3.3: 纯 Kotlin 工具函数迁入 + import 适配

- [ ] **Step 1: 迁入纯 Kotlin 工具函数**

```bash
mkdir -p shared/src/commonMain/kotlin/cn/wj/android/cashbook/core/common/{ext,enums,annotation,util,tools}

# 迁入闭包确定的文件（除 7 个 Android 专属文件 + 已在 Task 3.2 处理的 Money.kt/Number.kt）
# 具体文件列表由 Task 3.1 闭包分析确定
```

- [ ] **Step 2: java.time 替换**

迁入的 `Time.kt`、`tools/Time.kt`、`LunarUtils.kt` 中 `java.time` 引用 → `kotlinx-datetime`
（参照 Task 1.3 的替换表）。

- [ ] **Step 3: shared 编译验证**

```bash
./gradlew :shared:compileKotlinAndroid --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)|e: '
```

- [ ] **Step 4: 消费方 import 适配（~425 处）**

```bash
# 找出所有引用 core:common 工具函数的文件
grep -rln 'cn.wj.android.cashbook.core.common\.\(ext\|enums\|annotation\|util\)' \
  --include='*.kt' . | grep -v '.claude/' | grep -v '.git/' | grep -v 'core/common/' | grep -v 'shared/'

# 逐模块验证编译（非批量 sed）：
for module in $(ls -d core/*/ feature/*/ app/ sync/*/ 2>/dev/null); do
  ./gradlew :$(echo $module | tr '/' ':'):compileDebugKotlin --no-daemon 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
done
```

由于 package 声明不变，大部分 import 无需变更——仅需确认编译通过。

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/
git commit -m "[feat|shared|KMP][公共]Phase3: 纯 Kotlin 工具函数迁入 shared/commonMain（含 java.time→kotlinx-datetime 替换）"
```

---

### Task 3.4: 清理 core:common + 全量验证 + 收尾

- [ ] **Step 1: core:testing 双依赖**

修改 `core/testing/build.gradle.kts`，新增 `implementation(projects.shared)`（如同 Phase 1/2）。

- [ ] **Step 2: 删除 core:common 中已迁移的源文件**

```bash
# 删除已迁入 shared 的文件
rm core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ext/Money.kt
rm core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ext/Number.kt
rm core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ext/Time.kt
# ... （具体列表由 Task 3.1 确定）
```

- [ ] **Step 3: 全量编译 + 测试**

```bash
./gradlew :app:compileOnlineDebugKotlin --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
./gradlew testDebugUnitTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 4: Lint 验证**

```bash
./gradlew :app:lintOnlineRelease --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 5: DAO instrumented 测试（回归）**

```bash
./gradlew :core:database:connectedDebugAndroidTest --no-daemon --console=plain 2>&1 | grep -E 'BUILD (SUCCESSFUL|FAILED)'
```

- [ ] **Step 6: 最终 diff 审计**

```bash
git diff --stat main  # 确认变更文件数在预期范围内
```

- [ ] **Step 7: Commit**

```bash
git add -u core/common/ core/testing/
git commit -m "[refactor|KMP|公共]Phase3 完成：core:common 纯工具迁 shared，全量编译+测试+lint+instrumented 通过~460 files changed"
```

---

## 执行顺序

```
Phase 0 (T0.1→T0.2→T0.3→T0.4) ──gate passed──→ Phase 1 (T1.1→T1.2→T1.3→T1.4→T1.5)
                                                     │
                                                     ├──→ Phase 2 (T2.1→T2.2→T2.3→T2.4)
                                                     │
                                                     └──→ Phase 3 (T3.1→T3.2→T3.3→T3.4)
```

- Phase 0 是**硬门槛**——不通过则全方案降级，不执行 Phase 1-3
- Phase 1/2/3 串行依赖（2 依赖 1 的 shared 模块存在，3 依赖 2 同理）
- 每 Task 内步骤串行，每步骤是独立可提交单元

