# 搜索筛选设计评审与处理

日期：2026-10-30。对象：同目录 `2026-10-29-search-filter-design.md` 的首次草案。阶段为设计，不表示产品代码已实现或通过 Android 运行验证。

本轮按计划中的接口、查询和统计变更范围，执行 feasibility（含 brainstorming builder check）、reverse、impact、security 四维评审。四路独立回报，同一轮汇总；未调用跨模型或 Workflow，未追加 re-review。报告生成后，controller 自行回源核对并批量订正文档。

## 计数与完整性

| 维度 | Critical | High | Medium | Low | 原始 finding 数 |
| --- | --- | --- | --- | --- | --- |
| feasibility / builder | 0 | 0 | 2 | 0 | 2 |
| reverse | 0 | 0 | 2 | 2 | 4 |
| impact | 0 | 0 | 1 | 1 | 2 |
| security | 0 | 0 | 1 | 1 | 2 |
| 原始合计 | 0 | 0 | 6 | 4 | 10 |
| 按同一问题合并后 | 0 | 0 | 3 | 1 | 4 |

下文包含全部十项原始 finding 的归属、证据、影响及处理，无丢弃项。合并时保留各维度归属，取原始较高严重度。全部为设计约束问题，没有把静态检查当成新功能运行成功。

## D1 · Medium · 标签作用域的错误前提

归属：feasibility F1（Medium）、reverse R1（Medium）、impact I1（Medium）、security S2（Low）。impact 原报告以标题标识，本文为其分配 I1 方便映射。

首次草案在“筛选语义与模式切换”和“待批准默认”中写“切换账本清空账本专属标签”。当前公开模型无法区分这种归属：

- `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/TagModel.kt` 的 `TagModel` 只有 `id/name/invisible`。
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/TagRepository.kt` 的 `TagModel.asTable` 固定写 `booksId = -1L`，`TagTable.asModel` 不向领域层传递账本字段。
- `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TagDao.kt` 的 `queryAll` 是全表查询。
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/TagRepositoryImpl.kt` 的 `tagListData`、`getAllTagList` 直接提供该全局集合。

影响：若实施者从表字段、名称或当前结果推断账本归属，会让正常全局标签不可选或无依据清空已有约束；新增标签归属又会超出本次不改 schema 的边界。此 finding 不是已发生跨账本泄露的报告，账单查询的当前账本条件仍是独立要求。

独立核验：controller 直接读取领域模型、映射函数、DAO 和仓库实现，使用四条数据通路核验，没有仅重跑 reviewer 的过滤条件。

处理：撤去该错误前提；设计改为保留全局标签、分类及其他已应用条件，切换时只重建当前账本查询。验收增加同一个全局标签在两个账本均有记录的情形。该保留策略列为待全文批准的 Codex 默认，不伪装成用户此前说过的规则。

## D2 · Medium · 隐藏标签选择契约缺失

归属：feasibility F2（Medium）、reverse R3（Low）、impact I2（Low）。impact I2 同样为本文按原报告标题分配。

首次草案只规定“标签多选”，未说明隐藏标签是否可新选、已选标签后来隐藏时是否继续生效。

证据：`TagModel#invisible` 表示现有状态；`feature/tags/src/main/kotlin/cn/wj/android/cashbook/feature/tags/viewmodel/MyTagsViewModel.kt` 的 `switchInvisibleState` 可切换该状态；`core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetSelectableVisibleTagListUseCase.kt` 的 `invoke` 默认只显示未隐藏标签及原已选隐藏项；全量标签仓库没有相同过滤。两个现有入口不能被视为同一个产品契约。

影响：直接复用编辑选择器会令未选隐藏标签在筛选中不可达；显示全量则支持检索历史标签。将隐藏当成删除，还会让已选条件从界面消失或被错误清除。

独立核验：controller 读取上述 use case 的实际过滤谓词、标签仓库和隐藏状态更新入口，确认隐藏与删除是不同操作。

用户补充原话：2026-10-30 对“筛选器里是否允许选择已隐藏的标签，用于查询历史账单”回答“显示全部标签，并标记已隐藏状态”。

处理：设计明确全部标签均可新选，隐藏项带标记，已选后隐藏仍保留、可见、可移除且继续匹配；补充可见、隐藏、已选后隐藏、删除四种验收输入。此项是本轮 builder check 唯一需要用户补充的产品问题，答案已并入全文。

## D3 · Medium · 大集合参数预算和全局分页边界未落到约束

归属：reverse R2（Medium）、security S1（Medium）。

首次草案允许跨组多选和绑定 ID，但没有规定整条语句的总参数预算与超限时的行为。

证据：`core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SqlConstants.kt` 的 `SQL_IN_CHUNK_SIZE = 900` 是现有批量查询分块约定；`TagRepositoryImpl#getRelatedTags`、`RecordRepositoryImpl` 的多个批量关联查询使用该分块；组合查询还要同时携带分类、标签、类型、账本、日期与分页参数。

影响：【未验证·实施风险】直接将所有集合展开到同一 SQL，存在超过实际运行环境参数预算的风险；按各块分别分页再拼接会破坏全局排序、标签去重和全量汇总。没有实施新查询，本文不声称已经复现参数超限，也不把 900 说成所有设备的 SQLite 硬上限。

独立核验：controller 直接读取分块常量及多处真实调用点，结合设计中的组合输入独立枚举同一语句的参数来源。

处理：设计增加整条语句预算、实际 Room 验证及分类与标签组合跨过现有分块边界的用例；不支持的规模明确拒绝应用、保留原筛选。禁止截断条件、回落不限、各块局部分页拼接，以及默认将全量命中 ID 集载入内存。具体有界承载方式交由实施计划给出并实测，未凭空选定第三方 SQL 扩展。

## D4 · Low · 空选择的验收措辞与不限条件冲突

归属：reverse R4（Low）。

首次草案的条件关系定义空组表示不限，并允许用户显式应用不限；验收第 5 项却写“所有标签 ID 不可用、类型与分类冲突、空选择均不扩大范围”。

影响：按该验收字面写守卫，会错误禁止显式不限查询，并混淆“用户主动清空”与“原非空选择全部失效”。原关键字用例的空白短路只说明现有关键字行为，不能证明新筛选入口的显式不限行为。

独立核验：controller 对照首次草案“条件关系”“三种状态”“验收矩阵”三处文字，以及 `GetSearchRecordViewsUseCase#invoke` 的空白分支。

处理：明确分成未应用条件的初始态、用户显式应用空组为不限、非空所选 ID 全部失效不回落不限三格；冲突类型和分类保留选择并显示零结果。该项为文档一致性订正，由 controller 批量修改，不启动措辞复核循环。

## 回源后落实的实施边界

以下不是新增 finding，也未据此另开评审轮次：

- 平账类型通过既有合成 ID 映射，不能依赖普通 `db_type` 的 INNER JOIN。依据：`core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/LauncherRecordViewRelation.kt` 的 `types` 注释与 `RecordModelTransToViewsUseCase#transBatch`。设计已明确平账的明细、笔数与零金额贡献。
- 分类变化路径只更新 `typeDataVersion` 的情形需独立订阅。依据：`TypeRepositoryImpl#update`、`changeTypeToSecond`、`deleteById`。记录版本不能代替所有数据源版本。
- 保持关键字输入草稿、提交、搜索历史点击填入草稿、500 ms 历史延迟及原 DAO 排序；稳定同时间戳排序只作用新筛选通路，不顺手修改旧搜索。
- SQL 真实语义由 Room 测试覆盖，不能仅凭现有 FakeRecordRepository 的 contains 结果证明 LIKE 和账本隔离。
- 新通路遵循取消契约，不无差别复制将取消吞成普通 LoadResult.Error 的异常兜底。

## 本轮验证记录与限制

执行过的检查：源码定位引用文件存在性；设计 UTF-8 解码、脱敏路径与空白检查；`git diff --check`；主检出与 worktree 的状态检查；一轮四维静态评审与 controller 定向回源。

交互草图单独执行过 Node 脚本 smoke：多标签任意匹配与去重、关键字独立、筛选恢复、跨父分类／收支类型保留选择、分类搜索、摘要折叠与演示汇总；浏览器完成草图渲染截图。这些记录仅证明演示草图，不证明 Android 功能。

未执行 Android 构建、单元测试、Room 设备测试、产品守卫变异、真机回放或最终产品 full-review，原因是本阶段仅产出设计，产品代码尚未开始。上述项目属于获批后的实施与交付验收。

设计已登记 pending-docs；登记不等同知识库归档，尚未运行 `/summarize-session`。没有远端推送。
