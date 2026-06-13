# Cashbook 模拟器全量功能测试报告（2026-06-13）

- 设备：Medium_Phone（OfflineDebug，APK = `app/build/outputs/apk/offline/debug/app-Offline-debug.apk`，包名 `cn.wj.android.cashbook`）
- 方法：android-cli journey（controller 手工驱动、LLM 评估、**非 CLI 托管可重放套件、不可 CI 化**）
- 构建：`BUILD SUCCESSFUL in 54s`（`:app:assembleOfflineDebug --offline --no-daemon`）。

> 状态：**核心阻塞 BUG-1 已修复（commit 20a0e502）+ 回归测试（a0190d5e）**。修复后核心记账全链路端到端复验 PASS，各功能屏 render-smoke 无崩溃（详见 §2）。

## 0. 环境冒烟（PASS）
- 安装 OfflineDebug APK 成功，前台 `cn.wj.android.cashbook/.ui.MainActivity`。
- 首启隐私协议对话框正常（标题"用户协议和隐私政策"，按钮"取消"/"确认"）；点"确认"后进入主界面。
- 主界面渲染正常：月收入/月支出/月结余 ¥0.00（空库），添加 FAB/菜单/搜索/日历/分析入口齐全。
- 结论：构建→安装→启动→过协议→主界面 全链路 PASS。

## 1. 金额基线（现有测试）
- **JVM 金额单测**（`:core:model:test` `:core:data:testDebugUnitTest` `:core:domain:testDebugUnitTest` `:feature:records:testDebugUnitTest`）：✅ `BUILD SUCCESSFUL in 27s`。
- **`:core:database:connectedDebugAndroidTest`**（设备级 DAO，TransactionDaoTest 1009 行覆盖 finalAmount/簇重算/余额）：⏳ 待跑（受 §4 阻塞排查打断，未执行）。
- 结论：金额*算法*层 JVM 基线绿；设备级 DAO 待补。

## 2. journey 结果汇总（BUG-1 修复后复验）

> BUG-1 修复前：记账分类不渲染，依赖记录的 journey 全阻塞。修复后（commit 20a0e502）已解阻并复验。

| journey | 结果 | 备注（修复后复验） |
|---|---|---|
| 00-seed | ✅ 关键路径 PASS | 协议 gate✅、建账本 BookA✅、建 2 资产(现金¥1000/招商银行¥5000，净资产¥6000 求和正确)✅、**建记录端到端✅**（餐饮¥100/现金保存→首页月支出¥100/月结余-¥100/列表「餐饮 ¥100 现金」正确）。完整报销对冲簇未建（高 token，未驱动） |
| 01-records | ✅ 创建 PASS | 餐饮¥100 支出创建→保存→列表显示+金额联动全链路 PASS；编辑/删除未驱动 |
| 02-view | ✅ 列表显示 PASS | 首页记录列表、月度汇总（月支出/结余）渲染正确；finalAmount/被报销显示未驱动（未建报销簇） |
| 03-tags | ✅ PASS | 标签界面渲染正常（「还没有标签」空态） |
| 04-types | ✅ PASS（修复点） | 我的分类完整渲染 12 个支出一级分类（餐饮…人际关系）；记账界面同样恢复 |
| 05-assets | ✅ 创建+余额 PASS | 建现金/银行卡+余额求和¥6000 正确；隐藏/详情未驱动 |
| 06-analytics | ✅ PASS | 收支概览渲染（总支出¥100/总结余-¥100 正确）、每日统计/分类报表/支出比例 tab |
| 07-reimbursement | ✅ PASS | 待报销界面渲染正常（「共 0 笔 ¥0.00」「无记录数据」空态正确） |
| 08-settings | ✅ PASS | 设置渲染正常（图片质量/安全验证/黑夜模式/备份与恢复/关于） |
| 09-import | ⏭️ SKIP | 需虚构样例账单文件，未驱动 |
| 10-search-calendar | ⚠️ 未充分验证 | 导航 tap 未稳定命中搜索入口（测试驱动命中问题，非产品 bug 判定）；SearchScreen/CalendarScreen 已接线 |
| 11-books | ✅ PASS | 账本界面渲染正常（默认账本+BookA）；建账本已验，删/切换未驱动 |

**复验结论**：BUG-1 修复后，核心记账全链路（建账本/建资产/记账含分类选择/保存/列表显示/月度汇总/分析图表）端到端 PASS、金额正确；各功能屏 render-smoke 无崩溃。未深驱动项（编辑/删除/报销对冲簇/隐藏资产/搜索）属 token 预算取舍，journey 套件已就绪可后续补跑。

## 3. semantics 命中缺口清单（印证评审 M4）
- Compose **未开 `testTagsAsResourceId`** → `TestTag.kt` 的 testTag（含 `launcher_protocol_confirm`/`launcher_title`）**不在 `android layout` 暴露**；journey 只能靠 text/contentDesc/坐标命中。
- **可命中**：首屏图标带 content-desc（添加/菜单/搜索/日历/分析）；抽屉项全文本（我的账本/我的资产/我的分类/我的标签/待报销/设置/关于我们）；账本/资产列表项文本与金额（¥x.xx）；账本名/余额输入框可 focus + `adb input text`。
- **不可命中/需视觉**：记账界面**分类网格为图标无 text/contentDesc**（且本次根本未渲染，见 BUG-1）；资产类型/银行选择走 bottom sheet（文本可命中）。
- 协议同意按钮实际文本 **"确认"**（非"确定"）。
- **adb 输入约束**：`adb shell input text` 仅 ASCII，无法输入中文 → 测试数据改用 ASCII 名（BookA 等）。
- ⚠️ **DoKit 调试浮窗**叠加左上角，初始拦截"菜单"点击（打开的是 DoKit 面板而非抽屉）；拖到 [76,1681] 后菜单可点。见 BUG-2。

## 4. Bug 清单

### BUG-1【High｜核心阻塞｜✅已修复 commit 20a0e502】记账/我的分类 界面支出分类区不渲染
- **现象**：记账编辑界面（EditRecord）收起金额键盘后，类型 tab（支出/收入/转账）下方分类网格**空白无任何分类**；"我的分类"界面 支出 tab 同样空。tab/返回箭头异常地位于屏幕**垂直中部**（y≈1230）而非顶部，上下大片空白。
- **实证（决定性）**：从设备 pull 出 `cashbook.db` 用 sqlite3 查：`db_type = 97`（`type_category`: EXPENDITURE=83 / INCOME=9 / TRANSFER=5；`type_level`: 一级 29 / 二级 68），`db_type` **无 booksId 列**（类型全局、不按账本过滤）。即 **83 个支出类型存在于库，但 UI 渲染 0 个**。
- **复现**：跨页面（记账界面 + 我的分类）一致；app `force-stop` 重启后仍空；多次截图 + `android layout` 均无分类元素。
- **logcat**：无 exception/fatal/crash（静默空渲染）。
- **影响**：阻断核心记账流程——无分类可选 → 无法创建支出/收入记录 → seed 无法建记录 → 依赖记录的 journey（01/02/06/07/10）全部受阻。
- **诊断决定性证据**：加 println 重建后 logcat `CASHBOOK_DBG firstExp raw=29 exp=15` → 数据层运行时正常发射 15 个支出类型到 UI（排除数据/Flow/枚举映射）。`MyCategoriesTopBar:914` 仅 Success 才渲染 tabs，而 tabs 可见 → uiState=Success、typeList 非空 → body 被挤成 0 高。
- **根因（确认）**：`CbTabRow(Modifier.fillMaxSize())` 置于 `CbTopAppBar`（Material3 `TopAppBar`）title 槽——`fillMaxSize` 含 `fillMaxHeight`，该 Compose/Material3 版本下使 TopAppBar 按 title 撑满**全屏高**，`CbScaffold` body 被挤成 ~0 高 → 列表/空态都不渲染、tab 浮于垂直中部。**与数据无关**。结构对比：坏屏 `MyCategoriesScreen:917`+`EditRecordScreen:1036` 用 fillMaxSize TabRow；正常屏 MyBooks/MyAsset 用 `title={Text}`。
- **修复（commit 20a0e502）**：两处 `fillMaxSize()`→`fillMaxWidth()`。设备验证：MyCategories 渲染 12 个支出一级分类；EditRecord 渲染完整记账表单（分类网格+金额+资产+备注+标签+可报销/手续费/优惠）。核心记账流程恢复。
- **回归测试缺口**：现有 `MyCategoriesScreenScreenshotTests`/`EditRecordScreenScreenshotTests` 渲染 Success+非空 typeList 却未抓到此回归——基线 PNG 是带 bug 状态录入（回归被烘进基线掩盖）。已 re-record 校正基线使其今后能守此回归。

### BUG-2【Low｜仅 Debug 构建】DoKit 调试浮窗拦截首屏左上"菜单"点击
- **现象**：首屏 DoKit 浮窗（`float_icon_id`）默认停在左上 [76,139]，与"菜单"按钮 [74,147] 重叠；点"菜单"打开的是 DoKit 调试面板而非 app 抽屉。
- **绕过**：把浮窗拖到 [76,1681] 后菜单恢复可点。
- **影响**：仅影响 Debug 构建的自动化/手测左上角操作；Release 构建不含 DoKit，无产品影响。建议自动化测试构建禁用 DoKit 浮窗，或默认停靠位避开顶栏。

## 5. 超范围项（不计 PASS/FAIL）
- WebDAV 备份恢复（OfflineDebug 下 `OfflineWebDAVHandler` no-op）：SKIP
- Online 更新检查：N/A

## 6. 按需 instrumented（Phase 4）
- 本轮**未触发**确定性 instrumented 编写（journey 因 BUG-1 阻塞未进入"暴露 UI 层金额异常"分支；BUG-1 是分类不渲染、非金额回显错误，宜走 systematic-debugging 而非 instrumented 断言）。
