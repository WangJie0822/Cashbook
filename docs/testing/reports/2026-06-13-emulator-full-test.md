# Cashbook 模拟器全量功能测试报告（2026-06-13）

- 设备：Medium_Phone（OfflineDebug，APK = `app/build/outputs/apk/offline/debug/app-Offline-debug.apk`，包名 `cn.wj.android.cashbook`）
- 方法：android-cli journey（controller 手工驱动、LLM 评估、**非 CLI 托管可重放套件、不可 CI 化**）
- 构建：`BUILD SUCCESSFUL in 54s`（`:app:assembleOfflineDebug --offline --no-daemon`）。

> 状态：**执行中遇到核心阻塞**（记账类型 UI 不渲染，详见 §4 BUG-1），seed 无法建记录 → 依赖记录的 journey 受阻。已停下来向用户汇报。

## 0. 环境冒烟（PASS）
- 安装 OfflineDebug APK 成功，前台 `cn.wj.android.cashbook/.ui.MainActivity`。
- 首启隐私协议对话框正常（标题"用户协议和隐私政策"，按钮"取消"/"确认"）；点"确认"后进入主界面。
- 主界面渲染正常：月收入/月支出/月结余 ¥0.00（空库），添加 FAB/菜单/搜索/日历/分析入口齐全。
- 结论：构建→安装→启动→过协议→主界面 全链路 PASS。

## 1. 金额基线（现有测试）
- **JVM 金额单测**（`:core:model:test` `:core:data:testDebugUnitTest` `:core:domain:testDebugUnitTest` `:feature:records:testDebugUnitTest`）：✅ `BUILD SUCCESSFUL in 27s`。
- **`:core:database:connectedDebugAndroidTest`**（设备级 DAO，TransactionDaoTest 1009 行覆盖 finalAmount/簇重算/余额）：⏳ 待跑（受 §4 阻塞排查打断，未执行）。
- 结论：金额*算法*层 JVM 基线绿；设备级 DAO 待补。

## 2. journey 结果汇总

| journey | 结果 | 备注 |
|---|---|---|
| 00-seed | ⚠️ PARTIAL / BLOCKED | 协议 gate✅、建账本 BookA✅、建 2 资产(现金¥1000/招商银行¥5000，净资产¥6000 求和正确)✅；**建记录步骤受阻**——记账界面无分类可选（BUG-1），未能创建任何记录 |
| 01-records | ⛔ BLOCKED | 依赖记账分类，受 BUG-1 阻塞 |
| 02-view | ⛔ BLOCKED | 依赖 seed 记录（含报销对冲），无记录可看 |
| 03-tags | 未跑 | 不依赖记录，可在解阻塞前先跑（见下一步建议） |
| 04-types | ⚠️ 见 BUG-1 | "我的分类"支出分类区同样空（与记账界面同源症状） |
| 05-assets | 未跑 | 资产 CRUD 已部分验证（建/余额求和），可补完 |
| 06-analytics | 部分可跑 | 无记录则图表为空 |
| 07-reimbursement | ⛔ BLOCKED | 依赖报销对冲记录 |
| 08-settings | 未跑 | 不依赖记录，可先跑 |
| 09-import | 未跑 | 需虚构样例文件 |
| 10-search-calendar | ⛔ BLOCKED | 依赖记录 |
| 11-books | 部分可跑 | 账本建/列表已验，删/切换可补 |

## 3. semantics 命中缺口清单（印证评审 M4）
- Compose **未开 `testTagsAsResourceId`** → `TestTag.kt` 的 testTag（含 `launcher_protocol_confirm`/`launcher_title`）**不在 `android layout` 暴露**；journey 只能靠 text/contentDesc/坐标命中。
- **可命中**：首屏图标带 content-desc（添加/菜单/搜索/日历/分析）；抽屉项全文本（我的账本/我的资产/我的分类/我的标签/待报销/设置/关于我们）；账本/资产列表项文本与金额（¥x.xx）；账本名/余额输入框可 focus + `adb input text`。
- **不可命中/需视觉**：记账界面**分类网格为图标无 text/contentDesc**（且本次根本未渲染，见 BUG-1）；资产类型/银行选择走 bottom sheet（文本可命中）。
- 协议同意按钮实际文本 **"确认"**（非"确定"）。
- **adb 输入约束**：`adb shell input text` 仅 ASCII，无法输入中文 → 测试数据改用 ASCII 名（BookA 等）。
- ⚠️ **DoKit 调试浮窗**叠加左上角，初始拦截"菜单"点击（打开的是 DoKit 面板而非抽屉）；拖到 [76,1681] 后菜单可点。见 BUG-2。

## 4. Bug 清单

### BUG-1【High｜核心阻塞｜根因待代码级确认】记账/我的分类 界面支出分类区不渲染（DB 有 97 类型）
- **现象**：记账编辑界面（EditRecord）收起金额键盘后，类型 tab（支出/收入/转账）下方分类网格**空白无任何分类**；"我的分类"界面 支出 tab 同样空。tab/返回箭头异常地位于屏幕**垂直中部**（y≈1230）而非顶部，上下大片空白。
- **实证（决定性）**：从设备 pull 出 `cashbook.db` 用 sqlite3 查：`db_type = 97`（`type_category`: EXPENDITURE=83 / INCOME=9 / TRANSFER=5；`type_level`: 一级 29 / 二级 68），`db_type` **无 booksId 列**（类型全局、不按账本过滤）。即 **83 个支出类型存在于库，但 UI 渲染 0 个**。
- **复现**：跨页面（记账界面 + 我的分类）一致；app `force-stop` 重启后仍空；多次截图 + `android layout` 均无分类元素。
- **logcat**：无 exception/fatal/crash（静默空渲染）。
- **影响**：阻断核心记账流程——无分类可选 → 无法创建支出/收入记录 → seed 无法建记录 → 依赖记录的 journey（01/02/06/07/10）全部受阻。
- **根因**：未定。DB 有数据、无报错 → 疑 Compose 分类网格/列表的渲染或 inset 布局异常（tab 被挤到垂直中部是线索），或 ViewModel 类型 Flow 返回空。需 systematic-debugging 代码级排查（读 EditRecordScreen/MyCategoriesScreen 的类型加载与布局），非 journey 黑盒可定论。
- **证据文件**（本地，未入库）：`D:/Temp/editrec_body.png`、`D:/Temp/cats_final.png`、`D:/Temp/cashbook.db`（db_type 查询）。

### BUG-2【Low｜仅 Debug 构建】DoKit 调试浮窗拦截首屏左上"菜单"点击
- **现象**：首屏 DoKit 浮窗（`float_icon_id`）默认停在左上 [76,139]，与"菜单"按钮 [74,147] 重叠；点"菜单"打开的是 DoKit 调试面板而非 app 抽屉。
- **绕过**：把浮窗拖到 [76,1681] 后菜单恢复可点。
- **影响**：仅影响 Debug 构建的自动化/手测左上角操作；Release 构建不含 DoKit，无产品影响。建议自动化测试构建禁用 DoKit 浮窗，或默认停靠位避开顶栏。

## 5. 超范围项（不计 PASS/FAIL）
- WebDAV 备份恢复（OfflineDebug 下 `OfflineWebDAVHandler` no-op）：SKIP
- Online 更新检查：N/A

## 6. 按需 instrumented（Phase 4）
- 本轮**未触发**确定性 instrumented 编写（journey 因 BUG-1 阻塞未进入"暴露 UI 层金额异常"分支；BUG-1 是分类不渲染、非金额回显错误，宜走 systematic-debugging 而非 instrumented 断言）。
