# Cashbook 模拟器全量功能测试 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **本计划非典型 TDD 代码计划**：主体是「构建→起模拟器→编写 journey XML→串行执行落盘→汇总报告」的操作型任务，验证手段是命令输出/设备观测而非红绿循环；仅 Phase 4（按需 instrumented，条件触发）走 TDD。journey 动作为语义化步骤，运行时按 live `android layout` 解析锚点，不预写坐标。

**Goal:** 在 `Medium_Phone` 模拟器上对 Cashbook OfflineDebug 做全量功能 journey 探索测试 + 确认现有金额测试基线，产出 PASS/FAIL + bug 清单报告；仅当 journey 暴露 UI 层金额异常时按需补一个确定性测试。

**Architecture:** journey 广覆盖探索为核心（controller 逐条读 XML + `adb`/`android layout`/`android screen` 手工驱动、LLM 评估、即时落盘）；金额算法确定性依赖现有 JVM 单测 + `core:database` DAO androidTest（跑一次确认绿作基线）；按需 instrumented 条件触发（落在实际渲染该值的界面）。

**Tech Stack:** android-cli（emulator/run/layout/screen）、adb、Gradle（OfflineDebug + connectedDebugAndroidTest）、journey XML、Roborazzi/JUnit（现有）、Hilt+Compose UI test（仅按需）。

**关键事实基线（已 hands-on 核验）：**
- 现有可命中 `TestTag`：`CB_TOP_APP_BAR`、`Launcher.LAUNCHER_PROTOCOL_CONFIRM`、`Launcher.LAUNCHER_TITLE`（`core/common/.../TestTag.kt`）。`feature/*/src/main` 全量 testTag 仅 1 个，`EditRecordScreen=0`。
- 首屏 `LauncherContentScreen`：FAB=记账入口（`onAddClick`→EditRecord，:154/:228），左上 menu 图标=打开抽屉导航中枢（`onMenuClick`，:367）。
- 首启协议 gate：`MainAppViewModel.kt:138 needRequestProtocol=!agreedProtocol`（默认 false 必弹）；同意按钮 testTag=`LAUNCHER_PROTOCOL_CONFIRM`。
- `finalAmount` 在 `LauncherContentScreen.kt:734-740` 渲染，**不在** `EditRecordScreen`。
- library 模块无 flavor → instrumented task 名 `connectedDebugAndroidTest`（不带 flavor）。
- `feature/records/src/androidTest` 不存在 → `disableUnnecessaryAndroidTests` gate 关闭 variant，按需 instrumented 须先建目录。

---

## File Structure

| 文件 | 职责 | 阶段 |
|---|---|---|
| `.gitignore`（修改） | 排除 `docs/testing/reports/evidence/` 截图证据 | P0 |
| `docs/testing/journeys/00-seed.xml` | seed：协议 gate + 建基线虚构数据 + 校验 | P2 |
| `docs/testing/journeys/01-records.xml` … `11-books.xml` | 各功能 journey 动作清单 | P2 |
| `docs/testing/reports/2026-06-13-emulator-full-test.md` | 最终报告（journey 结果/金额基线/semantics 缺口/bug 清单/超范围） | P3-P5 |
| `docs/testing/reports/evidence/*.png`（gitignore） | journey 截图/layout 证据 | P3 |
| `feature/<x>/src/androidTest/...`（仅按需） | 条件触发的确定性 UI 测试 | P4 |

---

## Phase 0：环境准备（单会话产物：可用模拟器 + 已装 OfflineDebug APK）

### Task 0.1：.gitignore 排除截图证据 + 建目录

**Files:**
- Modify: `.gitignore`
- Create: `docs/testing/reports/evidence/.gitkeep`

- [ ] **Step 1: 在 .gitignore 末尾追加证据排除规则**

```gitignore

# 模拟器功能测试截图/layout 证据（不入库，避免仓库膨胀与隐私）
docs/testing/reports/evidence/
```

- [ ] **Step 2: 建 evidence 目录占位**

Run: `mkdir -p docs/testing/reports/evidence && touch docs/testing/reports/evidence/.gitkeep`
Expected: 目录创建成功（.gitkeep 自身因上面规则可能被忽略，用 `git add -f` 强加占位见 Step 3）

- [ ] **Step 3: 提交**

```bash
git add .gitignore
git add -f docs/testing/reports/evidence/.gitkeep
git commit -m "[test|reimbursement|模拟器测试][公共]gitignore 排除 journey 截图证据 + 建 evidence 占位目录"
```

### Task 0.2：构建 OfflineDebug APK

**Files:** 无（构建产物）

- [ ] **Step 1: 查内存压力（CLAUDE.local.md 强制）**

Run（PowerShell）:
```
$os=Get-CimInstance Win32_OperatingSystem; "Avail: {0:N0}MB Used%: {1:N1}" -f ($os.FreePhysicalMemory/1024),((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)
```
Expected: 可用 >2000MB 且使用率 <80% 才继续；否则停下来按 CLAUDE.local.md 处理（杀 daemon/关大应用）。

- [ ] **Step 2: 探活本地代理（仅当 Step 3 缺依赖报网络错时需要）**

Run（清继承代理后探活）:
```
curl -x http://127.0.0.1:7897 --max-time 20 -sI https://repo1.maven.org/maven2/ | head -1
```
Expected: `HTTP/...200` 或 `301`。若 `000` 则上游未出网，等恢复。

- [ ] **Step 3: 构建 OfflineDebug APK（优先 offline 暖缓存；缺依赖时带代理暖一次）**

Run（offline 优先）:
```
./gradlew :app:assembleOfflineDebug --offline --no-daemon --console=plain 2>&1 | tee /tmp/build_offline.log
```
若报缺依赖（offline 模式下载失败），改用带代理一次：
```
env -u http_proxy -u https_proxy -u ALL_PROXY ./gradlew :app:assembleOfflineDebug --no-daemon --console=plain -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 2>&1 | tee /tmp/build_offline.log
```

- [ ] **Step 4: 判定构建结果（只信这一条，CLAUDE.md 长时构建监控）**

Run: `grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/build_offline.log`
Expected: `BUILD SUCCESSFUL`。FAILED 则读 log 定位根因修复后重跑，不进下一步。

- [ ] **Step 5: 确认 APK 产物存在**

Run: `ls -1 app/build/outputs/apk/offline/debug/*.apk`
Expected: 列出 OfflineDebug APK 文件（如 `app-offline-debug.apk`）。

- [ ] **Step 6: 杀残留 Gradle daemon JVM（释放内存再起模拟器，CLAUDE.local.md）**

Run（PowerShell）: `Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force`
Expected: 无输出（已清）。

### Task 0.3：起模拟器 + 安装 + 冒烟（过协议进主界面）

**Files:** 无

- [ ] **Step 1: 启动 Medium_Phone 模拟器**

Run: `android emulator start Medium_Phone`
Expected: 命令在模拟器完全就绪后返回。

- [ ] **Step 2: 确认设备在线**

Run: `D:/Work/Development/AndroidSdk/platform-tools/adb.exe devices`
Expected: 列出一个 `emulator-5554  device`（或类似）。

- [ ] **Step 3: 安装 APK**

Run: `android run --apks=app/build/outputs/apk/offline/debug/app-offline-debug.apk`
Expected: 安装并启动成功（或用 `adb install -r <apk>` + `adb shell am start`）。

- [ ] **Step 4: 冒烟——过首启协议 gate，确认进入主界面**

执行：
1. `android layout --pretty` 查看当前屏；若存在隐私协议对话框（含 testTag/text 同意按钮），`adb shell input tap <同意按钮 center>`（锚点 testTag=`launcher_protocol_confirm` / 文本"确定"）。
2. `android layout --diff` 确认对话框消失、主界面渲染（含 testTag=`launcher_title` 元素）。
Expected: 主界面可见（`launcher_title` 命中）；app 不崩。**若协议框无法关闭或主界面不渲染→记为 BUG，halt 报告。**

- [ ] **Step 5: 提交冒烟记录（无代码改动，跳过 commit）**

无文件改动，本 Task 不提交；冒烟结果在 Phase 3 报告骨架中记录。

---

## Phase 1：金额确定性基线（依赖现有测试，跑一次确认绿）

### Task 1.1：跑现有 JVM 金额单测

**Files:** 无（运行现有测试）

- [ ] **Step 1: 跑 core:model / core:data / feature:records 金额相关 JVM 单测**

Run（JVM 库用 :test，Android 库用 :testDebugUnitTest，feature 用 :testDebugUnitTest）:
```
./gradlew :core:model:test :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest --no-daemon --console=plain 2>&1 | tee /tmp/money_jvm.log
```
Expected: `BUILD SUCCESSFUL`（覆盖 RecordAmountTest/MoneyTest/SaveRecordUseCaseTest/RecordRepositoryImplTest 等）。

- [ ] **Step 2: 判定**

Run: `grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/money_jvm.log`
Expected: `BUILD SUCCESSFUL`。FAILED 则该失败本身是一条金额基线 bug，记入报告 Phase 5。

- [ ] **Step 3: 杀 daemon（如新起）**

Run（PowerShell）: `Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force`

### Task 1.2：跑 core:database DAO androidTest（设备级金额基线）

**Files:** 无

- [ ] **Step 1: 确认模拟器在线（Phase 0 已起）**

Run: `D:/Work/Development/AndroidSdk/platform-tools/adb.exe devices`
Expected: 设备 online。

- [ ] **Step 2: 跑 core:database connectedDebugAndroidTest（task 名不带 flavor）**

Run:
```
./gradlew :core:database:connectedDebugAndroidTest --no-daemon --console=plain 2>&1 | tee /tmp/db_androidtest.log
```
Expected: `BUILD SUCCESSFUL`（覆盖 TransactionDaoTest 1009 行 finalAmount/簇重算/余额 + RecordDaoTest/AssetDaoTest 等）。

- [ ] **Step 3: 判定 + 记录测试数**

Run: `grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/db_androidtest.log`
Expected: `BUILD SUCCESSFUL`。结果纳入报告作"核心金额已被守护"证据。FAILED 记为基线 bug。

---

## Phase 2：编写 journey 套件（单会话产物：12 个 journey XML 文件）

> journey 动作语义化：用"点击记账 FAB / 打开抽屉导航到 X / 在金额输入框输入 N / 点击保存"等意图描述，执行时按 live layout 解析。verify 步骤以 "Verify"/"Check" 开头。

### Task 2.1：写 00-seed.xml

**Files:**
- Create: `docs/testing/journeys/00-seed.xml`

- [ ] **Step 1: 写 seed journey（协议 gate + 建基线虚构数据 + 校验）**

```xml
<journey name="00-seed">
  <description>
    建立基线虚构测试数据供后续 journey 复用。第一步必须处理首启隐私协议 gate。
    末尾逐项校验数据建成，任一缺失则本 journey FAILED、后续全 SKIP。
    所有数据均为虚构，禁止填入任何真实账户/WebDAV 凭据。
  </description>
  <actions>
    <action>If a privacy-protocol agreement dialog is shown on first launch, tap its agree/confirm button (testTag launcher_protocol_confirm, text "确定")</action>
    <action>Verify the home screen is shown (an element with testTag launcher_title is present)</action>

    <action>Open the navigation drawer by tapping the menu icon at the top-left of the home screen</action>
    <action>Navigate to the books (账本) management screen</action>
    <action>Create a new book named "测试账本A"</action>
    <action>Verify a book named "测试账本A" appears in the books list</action>
    <action>Navigate back to the home screen</action>

    <action>Open the navigation drawer and navigate to the assets (资产) management screen</action>
    <action>Create a new cash (现金) asset named "测试现金" with an initial balance of 1000</action>
    <action>Create a new savings-card (储蓄卡) asset named "测试储蓄卡" with an initial balance of 5000</action>
    <action>Create a new credit-card (信用卡) asset named "测试信用卡"</action>
    <action>Verify all three assets "测试现金", "测试储蓄卡", "测试信用卡" appear in the assets list</action>
    <action>Navigate back to the home screen</action>

    <action>Tap the add-record floating action button on the home screen</action>
    <action>Create an EXPENDITURE record: amount 100, select a category, select asset "测试现金", then save</action>
    <action>Verify the record list on the home screen shows a 100 expenditure</action>

    <action>Tap the add-record floating action button</action>
    <action>Create an INCOME record: amount 200, select an income category, select asset "测试储蓄卡", then save</action>
    <action>Verify the record list shows a 200 income</action>

    <action>Tap the add-record floating action button</action>
    <action>Create a TRANSFER record from "测试储蓄卡" to "测试现金" with amount 300, then save</action>
    <action>Verify a transfer record appears in the record list</action>

    <action>Tap the add-record floating action button</action>
    <action>Create an EXPENDITURE record amount 500 to be reimbursed later (select a category and asset, save). Note its description for later reimbursement.</action>
    <action>Tap the add-record floating action button</action>
    <action>Create an INCOME record of type 报销款 (reimbursement) amount 500, and in its related-record selection relate it to the 500 expenditure created above, then save</action>
    <action>Verify the 500 expenditure now shows as reimbursed (struck-through original amount with a reimbursed tag) in the record list</action>

    <action>Verify seed completeness: the books list contains "测试账本A"; the assets list contains "测试现金"/"测试储蓄卡"/"测试信用卡"; the record list contains at least one expenditure, one income, one transfer, and one reimbursed record</action>
  </actions>
</journey>
```

- [ ] **Step 2: 提交**

```bash
git add docs/testing/journeys/00-seed.xml
git commit -m "[test|reimbursement|模拟器测试][公共]新增 00-seed journey（协议gate+建基线虚构数据+校验）"
```

### Task 2.2：写功能 journey 01-11

**Files:**
- Create: `docs/testing/journeys/01-records.xml`, `02-view.xml`, `03-tags.xml`, `04-types.xml`, `05-assets.xml`, `06-analytics.xml`, `07-reimbursement.xml`, `08-settings.xml`, `09-import.xml`, `10-search-calendar.xml`, `11-books.xml`

- [ ] **Step 1: 写 01-records.xml（记账增删改）**

```xml
<journey name="01-records">
  <description>记账增删改：支出/收入/转账 + 手续费/优惠/标签/关联资产。前置：00-seed 已建基线。</description>
  <actions>
    <action>Tap the add-record floating action button on the home screen</action>
    <action>Create an EXPENDITURE record: amount 88, add a charge (手续费) of 2, add a concession (优惠) of 8, select a category, select asset "测试现金", attach a tag, then save</action>
    <action>Verify the new record appears in the record list</action>
    <action>Open the record just created to view its detail</action>
    <action>Edit the record: change amount to 99, then save</action>
    <action>Verify the record now shows amount 99 in the list</action>
    <action>Delete the edited record</action>
    <action>Verify the record no longer appears in the list and the app does not crash</action>
  </actions>
</journey>
```

- [ ] **Step 2: 写 02-view.xml（记录查看：列表/详情，含 finalAmount 回显）**

```xml
<journey name="02-view">
  <description>记录查看：列表/详情。重点核对被报销支出的 finalAmount 净自付回显（在列表项渲染）。</description>
  <actions>
    <action>On the home screen, verify the record list renders without crashing</action>
    <action>Locate the reimbursed 500 expenditure created in seed; verify it shows the original amount struck through and a reimbursed (已报销) indicator</action>
    <action>Tap that reimbursed expenditure to open its detail</action>
    <action>Verify the detail screen opens without crashing and shows the related reimbursement record</action>
    <action>Navigate back to the home screen</action>
    <action>Verify the monthly total/summary area on the home screen renders a number without crashing</action>
  </actions>
</journey>
```

- [ ] **Step 3: 写 03-tags.xml（标签管理）**

```xml
<journey name="03-tags">
  <description>标签管理：增删改。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the tags (标签) management screen</action>
    <action>Create a new tag named "测试标签"</action>
    <action>Verify "测试标签" appears in the tag list</action>
    <action>Rename "测试标签" to "测试标签改"</action>
    <action>Verify the tag now shows "测试标签改"</action>
    <action>Delete "测试标签改"</action>
    <action>Verify it no longer appears and the app does not crash</action>
  </actions>
</journey>
```

- [ ] **Step 4: 写 04-types.xml（类型管理：一级/二级）**

```xml
<journey name="04-types">
  <description>类型/分类管理：一级与二级类型增删改。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the categories/types (分类/类型) management screen</action>
    <action>Create a new first-level category named "测试一级类型"</action>
    <action>Verify "测试一级类型" appears in the category list</action>
    <action>Add a second-level category named "测试二级类型" under "测试一级类型"</action>
    <action>Verify "测试二级类型" appears under "测试一级类型"</action>
    <action>Delete "测试一级类型"</action>
    <action>Verify it no longer appears and the app does not crash</action>
  </actions>
</journey>
```

- [ ] **Step 5: 写 05-assets.xml（资产管理，隐藏后自恢复避免污染）**

```xml
<journey name="05-assets">
  <description>资产管理：编辑/详情/隐藏-显示。隐藏操作在本 journey 内自恢复，避免污染后续。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the assets (资产) management screen</action>
    <action>Open "测试信用卡" to view its asset detail</action>
    <action>Verify the asset detail screen renders the balance and record history without crashing</action>
    <action>Navigate back to the assets list</action>
    <action>Edit "测试信用卡": change its name to "测试信用卡改", then save</action>
    <action>Verify the asset now shows "测试信用卡改"</action>
    <action>Hide the asset "测试现金"</action>
    <action>Open the invisible/hidden assets screen and verify "测试现金" is listed there</action>
    <action>Unhide "测试现金" so it returns to the visible assets list (restore state)</action>
    <action>Verify "测试现金" is back in the visible assets list</action>
  </actions>
</journey>
```

- [ ] **Step 6: 写 06-analytics.xml（统计图表）**

```xml
<journey name="06-analytics">
  <description>统计图表：饼图/折线/分类统计/切换月份。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the analytics/statistics (统计) screen</action>
    <action>Verify a pie chart and/or line chart renders without crashing</action>
    <action>Switch the time period to a different month</action>
    <action>Verify the chart updates without crashing</action>
    <action>Tap into a category to open the typed/per-category analytics screen</action>
    <action>Verify the per-category analytics screen renders without crashing</action>
  </actions>
</journey>
```

- [ ] **Step 7: 写 07-reimbursement.xml（报销管理）**

```xml
<journey name="07-reimbursement">
  <description>报销管理：待报销抽屉项 + 报销/退款对冲。</description>
  <actions>
    <action>Open the navigation drawer and locate the pending-reimbursement (待报销) entry</action>
    <action>Tap the pending-reimbursement entry to open the reimbursement screen</action>
    <action>Verify the reimbursement screen renders without crashing</action>
    <action>Verify the previously reimbursed 500 expenditure and its reimbursement relationship are reflected correctly (no crash, amounts non-negative)</action>
    <action>Navigate back to the home screen</action>
  </actions>
</journey>
```

- [ ] **Step 8: 写 08-settings.xml（设置，WebDAV 超范围 SKIP）**

```xml
<journey name="08-settings">
  <description>设置：主题切换/安全设置入口/关于/协议。WebDAV 备份恢复超范围，不进入填写。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the settings (设置) screen</action>
    <action>Verify the settings screen renders without crashing</action>
    <action>Toggle a theme/appearance setting and verify the UI responds without crashing</action>
    <action>Open the about (关于) screen and verify it renders without crashing</action>
    <action>Open the user-protocol/markdown (协议) screen and verify it renders without crashing</action>
    <action>Do NOT enter any real WebDAV domain/account/password anywhere</action>
    <action>Navigate back to the home screen</action>
  </actions>
</journey>
```

- [ ] **Step 9: 写 09-import.xml（账单导入，无样例则 SKIP）**

```xml
<journey name="09-import">
  <description>账单导入。若无可用虚构样例账单文件，标记 SKIP 而非 FAIL。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the record-import (导入) entry if present</action>
    <action>If no fabricated sample bill file is available on the device, mark this journey SKIPPED and stop</action>
    <action>Otherwise, import the fabricated sample bill file and verify imported records appear without crashing</action>
  </actions>
</journey>
```

- [ ] **Step 10: 写 10-search-calendar.xml（搜索/日历）**

```xml
<journey name="10-search-calendar">
  <description>记录搜索与日历查看。</description>
  <actions>
    <action>Open the search (搜索) screen from the home screen</action>
    <action>Search for a keyword matching a seeded record's note/category</action>
    <action>Verify search results render without crashing</action>
    <action>Open the calendar (日历) view of records</action>
    <action>Verify the calendar screen renders records for a day without crashing</action>
  </actions>
</journey>
```

- [ ] **Step 11: 写 11-books.xml（账本管理，删除置最后）**

```xml
<journey name="11-books">
  <description>账本管理：新建/编辑/切换/删除。删除为不可逆破坏性操作，置于全套最后。</description>
  <actions>
    <action>Open the navigation drawer and navigate to the books (账本) management screen</action>
    <action>Create a new book named "测试账本B"</action>
    <action>Edit "测试账本B": change its description, then save</action>
    <action>Switch the current book to "测试账本B"</action>
    <action>Verify the home screen reflects the switched book (empty or B-specific records)</action>
    <action>Switch back to "测试账本A"</action>
    <action>Delete "测试账本B"</action>
    <action>Verify "测试账本B" no longer appears in the books list and the app does not crash</action>
  </actions>
</journey>
```

- [ ] **Step 12: 提交全部功能 journey**

```bash
git add docs/testing/journeys/01-records.xml docs/testing/journeys/02-view.xml docs/testing/journeys/03-tags.xml docs/testing/journeys/04-types.xml docs/testing/journeys/05-assets.xml docs/testing/journeys/06-analytics.xml docs/testing/journeys/07-reimbursement.xml docs/testing/journeys/08-settings.xml docs/testing/journeys/09-import.xml docs/testing/journeys/10-search-calendar.xml docs/testing/journeys/11-books.xml
git commit -m "[test|reimbursement|模拟器测试][公共]新增 11 个功能 journey XML（记账/查看/标签/类型/资产/统计/报销/设置/导入/搜索日历/账本）"
```

---

## Phase 3：串行执行 journey（即时落盘，断点可续）

> 执行规约（每个 journey 通用，对应 spec §3.3 假阳性兜底）：
> 1. 用 `android layout --pretty` 看屏，按动作语义找锚点；无锚点元素用 `android screen capture --annotate -o evidence/<j>-<n>.png` + `android screen resolve` 兜底，并**先 VISUALLY 看图**。
> 2. 每次交互后用 `android layout --diff` 确认 UI **确实变化**（排除残留/no-op）。
> 3. verify 锚定唯一标识（text/testTag），**不确定或失败一律标 FAILED，不标 PASS**。
> 4. 关键流程 `android screen capture -o docs/testing/reports/evidence/<journey>-<step>.png` 存证。
> 5. 每个 journey 跑完**立即把 JSON 结果写入报告文件**（断点可续）。
> 6. 若 app 崩溃/冻结/退出 → 该 journey FAILED 且记 BUG，停止该 journey。

### Task 3.0：初始化报告骨架

**Files:**
- Create: `docs/testing/reports/2026-06-13-emulator-full-test.md`

- [ ] **Step 1: 写报告骨架（含元信息 + Phase0 冒烟 + Phase1 金额基线结果占位 + journey 结果区 + semantics 缺口区 + bug 清单区 + 超范围区）**

```markdown
# Cashbook 模拟器全量功能测试报告（2026-06-13）

- 设备：Medium_Phone（OfflineDebug）
- 方法：android-cli journey（controller 手工驱动、LLM 评估、**非 CLI 托管可重放套件、不可 CI 化**）
- 说明：journey 为广覆盖探索；精确金额确定性由现有 JVM+DAO 测试守护。

## 0. 环境冒烟
（Phase 0 Task 0.3 结果：协议 gate 是否过、主界面是否渲染）

## 1. 金额基线（现有测试）
- JVM 金额单测：<BUILD SUCCESSFUL/FAILED + 摘要>
- core:database connectedDebugAndroidTest：<结果 + 测试数>

## 2. journey 结果汇总
（逐 journey 的 PASSED/FAILED/SKIPPED JSON）

## 3. semantics 命中缺口清单
（seed 阶段盘点：各 Screen 可命中元素 / 需 annotate 兜底的元素）

## 4. Bug 清单
（journey 暴露的崩溃/错屏/逻辑异常，含证据图引用）

## 5. 超范围项（不计 PASS/FAIL）
- WebDAV 备份恢复（OfflineDebug no-op）：SKIP
- Online 更新检查：N/A

## 6. 按需 instrumented（若触发）
（Phase 4 结果，未触发则标"本轮未触发"）
```

- [ ] **Step 2: 提交报告骨架**

```bash
git add docs/testing/reports/2026-06-13-emulator-full-test.md
git commit -m "[test|reimbursement|模拟器测试][公共]新增功能测试报告骨架"
```

### Task 3.1：执行 00-seed（含 semantics 盘点 + seed 校验，失败 halt）

**Files:**
- Modify: `docs/testing/reports/2026-06-13-emulator-full-test.md`

- [ ] **Step 1: 按执行规约逐步执行 `00-seed.xml`**

按 `docs/testing/journeys/00-seed.xml` 动作清单执行。第一步处理协议 gate。建数据过程中，对每个目标 Screen 用 `android layout` 记录可命中元素（用于 §3 semantics 缺口清单）。

- [ ] **Step 2: 执行 seed 完整性校验动作**

Expected: 末尾校验动作全部 PASS（账本/3 资产/各类型记录/报销对冲均建成）。

- [ ] **Step 3: 落盘 seed 结果 + semantics 缺口到报告 §2/§3**

把 seed 的 JSON 结果写入报告 §2，semantics 盘点写入 §3。

- [ ] **Step 4: seed 失败处理（halt gate）**

若 seed 任一校验失败：在报告标 `00-seed FAILED`，**后续 journey 全部标 SKIPPED**，跳到 Phase 5 汇总（不在残缺基线上跑）。

- [ ] **Step 5: 提交 seed 结果**

> evidence/*.png 被 .gitignore 排除，**只提交报告 md**，证据图仅本地留存、报告内引用文件名。

```bash
git add docs/testing/reports/2026-06-13-emulator-full-test.md
git commit -m "[test|reimbursement|模拟器测试][公共]执行 00-seed 结果 + semantics 盘点落盘"
```

### Task 3.2 ～ Task 3.12：逐条执行 01～11 journey

> 每个 Task 结构相同（以 3.2 为模板），对 `0N-*.xml` 各执行一次。

**Files:**
- Modify: `docs/testing/reports/2026-06-13-emulator-full-test.md`

- [ ] **Task 3.2 / Step 1：执行 `01-records.xml`**（按执行规约）
- [ ] **Task 3.2 / Step 2：落盘 01 的 PASSED/FAILED/SKIPPED JSON 到报告 §2，崩溃/异常入 §4 Bug 清单**
- [ ] **Task 3.2 / Step 3：提交** `git add docs/testing/reports/2026-06-13-emulator-full-test.md && git commit -m "[test|reimbursement|模拟器测试][公共]执行 01-records 结果落盘"`

- [ ] **Task 3.3：执行 `02-view.xml`** → 落盘 → 提交（消息改 02-view）
- [ ] **Task 3.4：执行 `03-tags.xml`** → 落盘 → 提交
- [ ] **Task 3.5：执行 `04-types.xml`** → 落盘 → 提交
- [ ] **Task 3.6：执行 `05-assets.xml`** → 落盘 → 提交
- [ ] **Task 3.7：执行 `06-analytics.xml`** → 落盘 → 提交
- [ ] **Task 3.8：执行 `07-reimbursement.xml`** → 落盘 → 提交
- [ ] **Task 3.9：执行 `08-settings.xml`** → 落盘 → 提交
- [ ] **Task 3.10：执行 `09-import.xml`**（无样例则标 SKIPPED） → 落盘 → 提交
- [ ] **Task 3.11：执行 `10-search-calendar.xml`** → 落盘 → 提交
- [ ] **Task 3.12：执行 `11-books.xml`**（删账本破坏性，最后跑） → 落盘 → 提交

> 中断恢复：因每 journey 结果已 commit 落 git，会话中断后从未完成的下一条 journey 续跑即可。中断后须 `Get-Process java | Stop-Process -Force` 清残留 daemon，并确认模拟器仍在线（否则 `android emulator start Medium_Phone` 重起，注意数据仍在 = 基线保留）。

---

## Phase 4：按需 instrumented（条件触发，本轮可能为空）

### Task 4.1：决策门——是否触发

**Files:** 无（决策 + 报告 §6）

- [ ] **Step 1: 评估 Phase 3 是否暴露「UI 层金额/状态异常，且现有 JVM/DAO 测试无法复现」**

判据：journey 在某涉钱界面（如 EditRecord 输入回显、LauncherContent finalAmount 显示）观测到金额显示/落库疑似错误，且 Phase 1 的 JVM/DAO 测试是绿的（说明算法对、疑点在 UI 接线层）。

- [ ] **Step 2: 决策并记录**

- 若**不满足** → 报告 §6 标"本轮未触发按需 instrumented"，**跳过 Task 4.2**，进 Phase 5。
- 若**满足** → 记录疑点界面与现象，进 Task 4.2。

### Task 4.2（条件）：在实际渲染该值的界面补最小确定性 UI 测试

> 仅当 Task 4.1 触发才执行。落点：finalAmount 异常→`LauncherContentScreen`；输入回显异常→`EditRecordScreen`。先 PoC 再断言。

**Files:**
- Create: `feature/records/src/androidTest/kotlin/cn/wj/android/cashbook/feature/records/<Screen>InstrumentedTest.kt`
- Modify: `feature/records/build.gradle.kts`（补 androidTest 依赖）

- [ ] **Step 1: 建 androidTest 目录（否则 disableUnnecessaryAndroidTests gate 关闭 variant）**

Run: `mkdir -p feature/records/src/androidTest/kotlin/cn/wj/android/cashbook/feature/records`

- [ ] **Step 2: 在 feature/records/build.gradle.kts 的 dependencies 补 androidTest 依赖**

```kotlin
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    kspAndroidTest(libs.hilt.compiler)
```
> 注：确认 version catalog 中存在对应 alias（`gradle/libs.versions.toml`）；缺失则先加 alias。`testInstrumentationRunner` 已由 feature 约定插件配为 `CashbookTestRunner`（`AndroidFeatureConventionPlugin.kt:39`），无需重配。

- [ ] **Step 3: 写最小 PoC 测试（先只验通 Hilt inject + Compose Rule 能渲染目标 Screen，不断言金额）**

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LauncherContentInstrumentedTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Before fun setup() { hiltRule.inject() }

    @Test fun poc_screen_renders() {
        // 先验通：能注入 + 能渲染目标 Composable（不崩即过）
        // 具体 setContent / 数据注入按目标 Screen 的真实入口补全
    }
}
```
> `HiltTestActivity` 若本仓无，则需新增一个空的 `@AndroidEntryPoint` 测试 Activity 并在 androidTest manifest 注册（`ui-test-manifest` 依赖提供 manifest 合并）。

- [ ] **Step 4: 跑 PoC（task 名不带 flavor）**

Run: `./gradlew :feature:records:connectedDebugAndroidTest --no-daemon --console=plain 2>&1 | tee /tmp/poc.log`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: PoC 失败处理（不强搭绿地基建）**

若 `grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/poc.log` = FAILED 且短时无法跑通：放弃 instrumented，改用 Phase 1 的 JVM/DAO 测试路径复现该疑点（或新增一个 DAO/UseCase JVM 测试复现），报告 §6 记录"按需 instrumented PoC 未通、改用现有测试层复现"。回滚 androidTest 目录与依赖改动：`git checkout -- feature/records/build.gradle.kts && rm -rf feature/records/src/androidTest`。

- [ ] **Step 6: PoC 通过后，补金额断言（在实际渲染该值的界面，断言精确分/元值）**

按疑点写断言（如 `composeRule.onNodeWithText("...").assertExists()` 校验回显金额）。再跑 Step 4 命令验 PASS。

- [ ] **Step 7: 提交**

```bash
git add feature/records/src/androidTest feature/records/build.gradle.kts gradle/libs.versions.toml
git commit -m "[test|reimbursement|模拟器测试][公共]按需补 <Screen> 确定性 UI 测试（journey 暴露 UI 层金额异常）"
```

---

## Phase 5：报告定稿 + 节点2评审 + 交付

### Task 5.1：汇总报告定稿

**Files:**
- Modify: `docs/testing/reports/2026-06-13-emulator-full-test.md`

- [ ] **Step 1: 补全报告各区**

汇总：§1 金额基线结果、§2 全 journey PASSED/FAILED/SKIPPED 统计、§3 semantics 缺口清单、§4 Bug 清单（每条含界面/复现步骤/证据图文件名/严重度）、§5 超范围、§6 按需 instrumented 结论。报告顶部加一句总评（通过率 + 关键 bug 数）。

- [ ] **Step 2: 提交**

```bash
git add docs/testing/reports/2026-06-13-emulator-full-test.md
git commit -m "[test|reimbursement|模拟器测试][公共]功能测试报告定稿"
```

### Task 5.2：节点2 full-review（依本次 diff 内容决定范围/降级）

- [ ] **Step 1: 看本次 git diff 性质**

Run: `git diff main...HEAD --stat`

- [ ] **Step 2: 选择评审范围（CLAUDE.md 节点2 + 文档领域豁免）**

- 若 diff **仅含** journey XML / 报告 md / .gitignore（纯文档/配置，无代码逻辑）→ 节点2 **可选**（文档领域豁免），可跳过并标注 `【纯文档/配置 diff，节点2 豁免】`。
- 若 diff **含 Phase 4 instrumented .kt 代码**（含 build.gradle/version catalog 改动）→ 跑节点2，规模 <50 行可降级 `comprehensive-review-code-reviewer` + `comprehensive-review-architect-review` 两维快审。

- [ ] **Step 3: 执行选定评审并完整呈现 finding**

调用 `comprehensive-review:full-review`（或降级两维）。blocking = Critical/High，交付前修复或列出 + 引用用户放行。

### Task 5.3：交付

- [ ] **Step 1: 收尾——清理设备/进程**

Run（PowerShell）: `Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force`；按需 `android emulator stop Medium_Phone`。

- [ ] **Step 2: 交付总结**

向用户呈现：报告路径、通过率、关键 bug 清单、超范围项、按需 instrumented 结论、节点2 结果。提醒 `/summarize-session` 沉淀。

---

## Self-Review（plan 对 spec 覆盖检查）

- **spec §2 环境** → Phase 0（构建/内存/模拟器/冒烟）✓
- **spec §3 journey 套件**（seed+协议gate+semantics盘点+seed校验+11功能journey+假阳性兜底+状态污染控制+超范围） → Task 2.1/2.2 + Phase 3 执行规约 ✓
- **spec §4 金额确定性依赖现有测试** → Phase 1（JVM + DAO androidTest）✓
- **spec §5 按需 instrumented**（触发条件+落点+前置+PoC+不触发为空） → Phase 4 ✓
- **spec §6 数据/脱敏**（虚构数据/不填真实WebDAV/证据gitignore） → Task 0.1 + 2.1 seed description + Phase 3 规约 ✓
- **spec §7 报告产物** → Task 3.0 骨架 + 5.1 定稿 ✓
- **spec §8 风险**（会话中断落盘续跑/内存/假阳性/seed失败/命中不稳/PoC不通） → Phase 3 中断恢复注 + Phase 0 内存 + 执行规约 + Task 3.1 halt + Task 4.2 Step5 ✓
- **节点2 评审**（CLAUDE.md 强制） → Task 5.2 ✓
- **placeholder 扫描**：journey XML 为完整动作清单（语义化非占位）；Phase 4 为条件触发、代码为 PoC 骨架（落点依实际疑点，已注明补全点）——属合理「条件分支待实测填充」，非占位。
- **类型/命名一致**：journey 文件名 00~11 与 Phase 3 执行 Task 一一对应；task 名统一 `connectedDebugAndroidTest`（不带 flavor）✓

> **修正备注**（Task 3.1 Step 5）：evidence/*.png 被 .gitignore 排除，提交时**只 add 报告 md**，不 `git add -f` 证据图（证据仅本地留存，报告内引用文件名）。
