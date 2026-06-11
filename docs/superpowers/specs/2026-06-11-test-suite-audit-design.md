# 测试套件全量评估 —— 审计方案设计

> 日期：2026-06-11
> 类型：审计（仅产出报告，不改代码）
> 触发：用户「全量评估当前项目的测试用例，确认所有功能是否覆盖，逻辑是否正常，反向测试是否覆盖」

## 1. 目标与边界

### 目标
对 Cashbook 全项目测试套件做一次系统性审计，从三个维度回答：
1. **功能覆盖广度**——所有功能/可测单元是否有测试
2. **逻辑正确性**——现有测试是否真正验证了正确行为（重点：假阳性桩、弱断言、金额口径混用、测试与代码漂移）
3. **反向/边界/异常覆盖**——错误路径、边界值、异常分支是否被测试

### 交付物
- 本审计方案 spec（本文件）
- **审计报告**：`docs/superpowers/specs/2026-06-11-test-suite-audit-report.md`
  - 覆盖矩阵摘要
  - 按严重度排序的 finding 列表（每条带 `file:line` 证据 + 修复建议）
  - 按模块分组视图
  - 统计汇总

### 边界（明确不做）
- **不改任何代码**（用户已确认：先报告，后分批决定补）
- 不实跑 `androidTest`（本机无设备，DAO 集成测试仅做静态源码覆盖审计）
- 不深审 Roborazzi 截图测试的渲染逻辑（截图测试验证渲染而非业务逻辑）

## 2. 测试现状快照（实证，2026-06-11）

- 测试文件总数 **157**（`find . -path "*/src/test/*" -o -path "*/src/androidTest/*" -name "*.kt"` 计数）
- 各模块 main 源文件 vs 测试文件密度：

| 模块 | main | test | 备注 |
|---|---|---|---|
| core/common | 31 | 3 | 薄弱（仅 Money/Number/String） |
| core/data | 24 | 22 | 强覆盖，含 8 个 Fake 桩 |
| core/database | 35 | 7 (androidTest) | 仅静态可审 |
| core/datastore | 8 | 1 | 薄弱 |
| core/datastore-proto | 0 | 0 | 纯 proto 生成 |
| core/domain | 25 | 27 | 强覆盖 |
| core/model | 50 | 2 | 薄弱（多为数据类，仅 RecordAmount/AnalyticsPieAmount 有测试） |
| core/network | 13 | 3 | 薄弱 |
| core/design | 46 | 27 | 多为截图测试 |
| core/ui | 10 | 3 | 多为截图测试 |
| core/testing | 13 | 0 | 测试基建本身，正常 |
| feature/assets | 16 | 10 | ViewModel + 截图 |
| feature/books | 6 | 4 | |
| feature/records | 31 | 18 | 含 analytics |
| feature/record-import | 6 | 1 | 薄弱 |
| feature/settings | 16 | 12 | 含备份恢复 |
| feature/tags | 13 | 6 | |
| feature/types | 10 | 6 | |
| sync/work | 10 | 1 | 薄弱 |
| app | 7 | 3 | |
| lint | 3 | 1 | |

## 3. 三维审计判据

每条 finding 必须落到这三维之一，并带 `file:line` 证据。

### 维度① 功能覆盖广度
- 以 main 源的可测单元为基准枚举：UseCase / Repository 方法 / DAO 查询 / 纯函数 / Worker / ViewModel
- 对照 test 实际调用，判定「有测试 / 无测试 / 有文件但未触达该方法」
- 产出：覆盖矩阵（源单元 → 状态 → 测试位置）

### 维度② 逻辑正确性（核心，最易翻车）
- **假阳性桩**：Fake DAO/Repository 是否忠实复刻真实 SQL 语义（单位、匹配条件、`LIKE` 定界符）——对照真实 DAO vs Fake 实现
  - 历史踩坑：`queryByTimeAndAmount`（元/分单位错配桩使去重永不命中）、`queryByWechatTransactionId`（`emptyList()` 桩使 EXACT 路径从未覆盖 + 裸 `contains` 偏离真实 `remark LIKE '%[微信单号:<id>]%'` 方括号定界）
- **断言强度**：是否断言关键输出值，还是只「不抛异常 / 非空」弱断言
- **金额口径混用**：`recordAmount` / `analyticsPieAmount` / `analyticsPieNetAmount` 三口径（TRANSFER 处理相反）是否选错
- **测试与代码漂移**：是否还在测已废弃/已改语义的行为（如旧吸收模型可负值）

### 维度③ 反向/边界/异常覆盖
- 空输入/空集合/null；边界值（0 金额、负数、分/元转换边界）
- 异常路径（事务回滚、IO 失败、解析失败、网络错误）
- 顺序/簇依赖（吸收者 id 升序、BFS 发现簇）
- 非法输入（格式错误账单、越界日期）

## 4. 模块分层与执行编排（方案 C：混合编排）

| Tier | 模块 | 谁审 | 深度 |
|---|---|---|---|
| **T1 金额核心** | core/model、core/data、core/domain、core/database | **Controller 亲审** | 三维深审，重点假阳性桩 + 金额口径 + 事务反向 |
| **T2 其余深审** | sync/work、core/common、core/network、core/datastore | Agent fan-out 产 finding → **Controller 逐条核验** | 三维深审 |
| **T3 广度扫描** | feature/*（7）、core/ui、core/design、app、lint | Workflow 只读 fan-out（schema 强校验） | 仅覆盖缺口 + 明显反向缺失，截图不深审渲染 |

### 编排约束
- Workflow agent 全程**只读**（无 Write/Edit），**禁跑本地构建**（纯静态审计，不触发 Gradle 资源问题）
- T1 的「假阳性桩」「金额口径」判定由 controller 亲自 Read 真实 DAO/SQL 与 Fake 实现对照核验，不交 agent
- T2/T3 的 agent 强断言（"已确认无覆盖 / 桩不忠实"）一律按 CLAUDE.md「事实优先原则」由 controller hands-on 核验后才入报告
- `core/database` androidTest 报告显式标注「未实跑，仅静态审」

## 5. Finding 结构与严重度

每条 finding：

```
维度 | 模块 | 标的(源 file:line + 测试 file:line) | 问题 | 严重度 | 修复建议
```

严重度分级：
- **Critical**：假阳性覆盖致真实 bug 可漏网（如金额错算路径有测试但桩骗过）
- **High**：关键业务路径完全无覆盖 / 弱断言掩盖逻辑错误
- **Medium**：反向/边界/异常分支缺失
- **Low**：次要工具/边角缺口

## 6. 执行阶段（产物与依赖，不标工时）

- **Phase 1 — T1 金额核心 controller 亲审**：单会话产物，逐模块 Read 源+测试对照，产 T1 finding（含假阳性桩判定）
- **Phase 2 — T2 深审**：Workflow 只读 fan-out 产候选 finding → controller 逐条核验 → 入报告
- **Phase 3 — T3 广度扫描**：Workflow 只读 fan-out 产覆盖缺口 → controller 低成本核验 → 入报告
- **Phase 4 — 综合报告**：合并去重、按严重度排序、写覆盖矩阵 + 统计、落盘报告文件

各 Phase 之间 controller 串行收口，报告增量写入。

## 7. 与既有规范的衔接

- 本任务为**纯审计**，按 CLAUDE.md「Agent Team 评审」文档领域豁免：节点 1/节点 2 评审针对的是代码方案，本任务无代码变更，不强制走 team-review/full-review
- Workflow 使用为用户显式 opt-in（prompt 含「按需使用 ultracode」）
- 报告落盘后更新 `D:\Vault\.meta\pending-docs.json`
