# 首页列表分页优化（R6a）真机 journey 验收记录

- 日期：2026-07-08
- 分支：`worktree-perf-poc`
- 设备：Medium_Phone AVD（Android 11 / API 30，时区 GMT）
- APK：`app-Online-debug.apk`（`:app:assembleOnlineDebug` 产物）
- 方案：R6a —— Room `@Transaction`+`@Relation` 分页视图（`pagingLauncherRecordViews`）一次批量物化 type/asset/tags/images/双向 relatedRecord，消 N+1；Room 生成 `LimitOffsetPagingSource`，对全部关联表自动 invalidate（保位、不回顶）。

## 数据源

设备既有真实备份账本「默认账本」（book 1，6023 条历史记录 + 160 个报销对冲簇）；本月（2026-07）原为空。为验证首页当前月渲染与增删改保位，向 book 1 的 2026-07 注入 31 条脱敏记录（`run-as` 直改 DB，纯 `PERF*` 占位备注，含 1 个报销对冲对：被报销支出 id6245 finalAmount=0 / 报销款收入 id6246，关联行 169）。

## 验收结论

### 1. 首屏列表正确加载（真实 @Relation 链路，非孤立 PoC）✅

首页（2026-07）经新 `getRecordPagingData → @Relation POJO → toRecordViewsModel → asEntity → UI` 全链路正确渲染：

- 顶部汇总：月收入 ¥5000.00 / 月支出 ¥836.22 / 月结余 ¥4163.78（分→元正确）。
- 日期头分组 + 日汇总：`7日（昨天）支：¥185.22`、`6日（前天）支：¥163.30` …
- 记录项：`餐饮 11:30 PERF支出#28 ¥48.36` 等，金额、类型、时间正确。

### 2. 报销对冲显示正确（双向 relatedRecord → relatedNature/relatedAmount/finalAmount 净自付）✅

- 报销款收入（id6246）：`报销 11:00 PERF报销款 已关联(¥200.00) ¥200.00` —— `relatedNature` + `relatedAmount` 正确。
- 被报销支出（id6245）：`餐饮 10:00 PERF被报销 已报销(¥200.00) ¥0.00` —— finalAmount 净自付=0 正确显示。

此为 Room `@Relation` 双向 Junction（`relatedAsRecordId`/`relatedAsRelatedId`）在真库经映射层组装的端到端证据，与单条 `RecordModelTransToViewsUseCase` 逐字段等价（Task 3 单测 + Task 7 androidTest 亦已守护）。

### 3. 删除中部记录 → 列表刷新、**不回顶**、日汇总正确更新 ✅（R6a 核心收益）

- 滚到列表中部：锚点「3日」头位于 y=244、`PERF支出#12` 位于 y=435。
- 点击中部 `PERF支出#11`（¥25.07）→ 记录详情 sheet → 删除 → 确认。
- 删除后：锚点「3日」头**仍在 y=244**、`PERF支出#12`**仍在 y=435**（坐标完全不变，列表未跳回顶部/最新 7 日）；被删项消失、后续项自然上移；「3日」日汇总由 ¥97.54 → ¥72.47（−25.07）正确重算。

证据：`scratchpad/ui7.xml`（删前）vs `ui10.xml`（删后）锚点 y 坐标逐一比对不变。

### 4. 增删改保位机制与切月

新增/编辑记录返回首页走**同一** `Pager.flow` + Room 自动 invalidate 路径，与已实证的删除保位为同一机制（androidTest `RecordDaoRelationTest` 已验 `@Relation` 全表 invalidate 语义）。切月/切「全部」经 `_dateSelection` flatMapLatest 重建 Pager（预期行为，非保位场景）。

## 与「完整链路验证」的关系

- 端到端从真实入口（首页 ViewModel）+ 真实上游（Room `@Relation` 分页）+ 真实数据（备份账本 + 报销对冲簇）跑通，非孤立 PoC 手填理想输入。
- N+1 消除为架构性保证（单次 `@Relation` 批量 IN vs 逐条 type/asset/tag/image/related 查询），并由 `RecordDaoRelationTest` 真库单批查询证据支撑；本 journey 未做前后耗时基准数值对比（如需量化数字须另做 benchmark）。

## 备注

注入数据仅为本机 journey 验证（`PERF*` 脱敏占位），不入库、不随代码交付。
