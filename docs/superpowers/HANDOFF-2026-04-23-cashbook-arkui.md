# Cashbook ArkUI-X 会话交接文档

**最后更新：** 2026-04-24
**当前阶段：** brainstorm → spec → plan 已完成 · 等待 M1 Foundation 执行
**下一步：** 选定执行模式（Subagent-Driven / Inline），从 Task 1 开始

---

## 1 · 产物清单

### 已提交到 git 的现有工作（不需要回看）
仓库根目录是 Android v1 Kotlin 项目，不动。

### 本轮新增（**尚未 git commit**）

| 文件 | 用途 |
|---|---|
| `docs/superpowers/specs/2026-04-23-cashbook-harmonyos-ui-design.md` | 设计 spec（18 章 · 约 900 行 · approved） |
| `docs/superpowers/plans/2026-04-23-cashbook-arkui-m1-foundation.md` | M1 Foundation 实施计划（14 Task 严格 TDD） |
| `docs/superpowers/HANDOFF-2026-04-23-cashbook-arkui.md` | 本文件 |

### 视觉稿（gitignored，在 `.superpowers/brainstorm/...`）

28 屏像素级 mockup 在 `.superpowers/brainstorm/59421-1776923418/content/`：
- `cashbook-v6-pixel-perfect.html` · 首页 L/D + 记一笔（Part 1）
- `cashbook-v6-pixel-perfect-part2.html` · 账本流水 + 统计 + 资产
- `cashbook-v6-pixel-perfect-part3.html` · 日历 + 4 核心弹窗
- `cashbook-v6-pixel-perfect-part4.html` · 6 管理页
- `cashbook-v6-pixel-perfect-part5.html` · 关于 + 6 扩展弹窗 + 3 状态屏

历史视觉迭代（v1-v5，可参考但可忽略）：
- `style-directions.html` · 初始 3 方向
- `cn-classical-directions.html` · 中国古典色 3 变种
- `cashbook-harmonyos-v1.html` · HMOS 首版
- `cashbook-harmonyos-v2-adjustments.html` · v2 调整
- `cashbook-v3-harmonyos-native.html` · v3 HMOS 原生光感
- `cashbook-v4-modern-bento.html` · v4 现代化 Bento
- `cashbook-v5-gradient-bg.html` · v5 多层渐变背景

---

## 2 · 关键决策快照

### 技术栈
- **目标平台**：HarmonyOS NEXT 主 + Android + iOS 副（一套 ArkUI-X 代码）
- **语言**：ArkTS（`.ets`）
- **测试**：`@ohos/hypium`
- **构建**：DevEco Studio 5.0+ / hvigor
- **API Level**：12+

### 项目结构
- 原 Android Kotlin 项目：仓库根目录 · **维护态保留不动**
- 新 ArkUI-X 项目：`app-arkui/` · **v2 主线**
- 共享：`docs/`、`scripts/avd-to-svg.py`、`resources/`（AVD → SVG 产物）

### 视觉语言：Neo-Modern Chinese Bento
- 中国古典色（青瓷 / 朱砂 / 松绿 / 秋香 / 胭脂 / 青石 / 黛青）
- HMOS 原生 BlurStyle 沉浸光感（COMPONENT_THIN/REGULAR 在顶/底栏 + Sheet/Modal）
- 多层背景渐变（linear 主 + 3 个 radial 色温光晕 · 8~16% alpha）
- Bento 不规则网格（1.4fr + 1fr + 跨行大卡）
- Fraunces / 思源宋体 Display Serif for 大号金额
- Pulse dot · 徽章式指标 · Editorial tagline

### 数据层约定（不变）
- Schema v10 兼容（`RecordTable` / `TypeTable` / `AssetTable`）
- 金额 `Long` 分（复用 `core/common/ext/Money.kt` 的约定）
- `RecordTypeModel` 真两级分类：`parentId = -1` 为一级 · `typeLevel FIRST/SECOND` · `typeCategory EXPENDITURE/INCOME/TRANSFER`
- 固定类型保留：`RECORD_TYPE_BALANCE_EXPENDITURE (-1101)` / `RECORD_TYPE_BALANCE_INCOME (-1102)`

### 图标
- 复用 `core/ui/src/main/res/drawable/vector_type_*_24.xml` 150+ 个
- 迁移脚本 `scripts/avd-to-svg.py`（本计划 Task 6 产出）
- ArkUI-X 引用：`$r('app.media.vector_type_dining_24')`

---

## 3 · M1 Foundation 执行计划

详见 `docs/superpowers/plans/2026-04-23-cashbook-arkui-m1-foundation.md`

**14 Task 顺序（严格依赖）：**
1. ArkUI-X 项目脚手架
2. 色彩 Token（Light + Dark）
3. 字号 / 间距 / 圆角 Token
4. 测试基础设施
5. GradientBackground 组件
6. AVD → SVG 迁移脚本 + 批量产物
7. PulseDot
8. Badge
9. Chip
10. CbCard + HeroDarkCard
11. CbButton（5 变体）
12. DisplayText + ComponentCatalog 预览页 + README
13. Sparkline
14. RingProgress + SegmentTab + ToastHelper

**M1 验收：** `hvigorw test` 全绿 · ComponentCatalog 手动 QA 所有组件明暗双套 · 150+ SVG 就位 · README 完整

**M1 完成后：** M2-M6 各自启动独立 brainstorm → spec → plan 循环，不合并执行。

---

## 4 · 未决问题

### 执行模式（用户需选）
- [ ] **Subagent-Driven**（推荐）· 每 Task 派发新 subagent + 两阶段评审 · 适合长周期
- [ ] **Inline Execution**· 当前 session 批量 + checkpoint · 适合冲刺

### Spec §17.2 开放问题（可留到 M2 之前决策）
1. 是否同时长期维护 Android v1？（当前假设：v1 维护态保留直至 v2 发布半年后再下线）
2. Display 字体选哪个：
   - A. Fraunces（现代衬线，带意大利斜体）
   - B. Instrument Serif（现代偏古典）
   - C. 思源宋体（更东方）
3. 图标分发：
   - A. 直接复制 SVG 到 `app-arkui/entry/src/main/resources/base/media/`（当前 plan 采用）
   - B. 发布成独立 HAR package
4. Theme 切换：系统跟随 / 手动 / 日出日落定时（三选 / 全支持）
5. 多主题扩展（v2 只做青瓷月光 + 黛青深宙，后续是否做朱墨 / 竹林 / 雪等）

### 其他
- 签名配置：ArkUI-X 需新配置（`signing.versions.toml` 仅用于 Android v1）
- CI/CD：是否同步建立 `hvigorw` 的 GitHub Actions（M1 后期决定）
- 默认 seed 分类数据（一级 18 + 二级 80-120）：需核实现有 Android `TypeDao` seed 逻辑（M2 阶段处理）

---

## 5 · 视觉伴侣（Visual Companion）

**当前 session 已启动的 server 将在会话结束时失联（30min 超时自动停）。**

新会话需要查看 mockup 时重启：
```bash
/Users/wj/.claude/plugins/cache/claude-plugins-official/superpowers/5.0.7/skills/brainstorming/scripts/start-server.sh --project-dir /Users/wj/Work/Owner/StudioProjects/Cashbook
```

会生成新 session 目录（如 `12345-1776xxxxxxx/content`），需要把旧的 28 屏 HTML 复制过去：
```bash
cp -p /Users/wj/Work/Owner/StudioProjects/Cashbook/.superpowers/brainstorm/59421-1776923418/content/*.html \
      <新 session>/content/
```

---

## 6 · 新会话启动指令模板

```
继续 Cashbook ArkUI-X 迁移。

【产物】
- spec: docs/superpowers/specs/2026-04-23-cashbook-harmonyos-ui-design.md
- plan: docs/superpowers/plans/2026-04-23-cashbook-arkui-m1-foundation.md
- handoff: docs/superpowers/HANDOFF-2026-04-23-cashbook-arkui.md

【要求】
1. 先读 handoff 文档了解上下文
2. 执行模式：[Subagent-Driven | Inline] （二选一）
3. 从 M1 Foundation Task [1] 开始
4. 严格 TDD（fail → impl → pass → commit）
5. 每完成一个 Task 后：等待我的 review 反馈再进下一个

【约束】
- 遵循项目 CLAUDE.md（事实优先 / 实证优先 / 变更影响评审 / 完整链路验证）
- Commit 格式：[feat|module|feature][公共]说明
- 不 commit spec 和 plan（已待 review）
- 所有测试必须实际运行确认通过，不能假设
```

---

## 7 · 本轮迭代要点（为什么值得记住）

1. **5 轮视觉迭代的收敛路径：**
   - v1 全量 → v2 色板调亮 + 首页左上角时间筛选 + 两级分类 + 弹窗
   - v2 → v3 HMOS 原生 BlurStyle 替代 CSS 模拟 + 真 SVG + RecordTypeModel 真结构
   - v3 → v4 "太朴素" → 现代化 Hero 反色 + Bento + Display Serif + 徽章
   - v4 → v5 整屏多层渐变背景
   - v5 → v6 像素级（28 屏 390×844）

2. **spec 范围拆分结论：** 原"8 周 6 milestone"不适合一个 plan。决策：M1 单独成 plan，M2-M6 各自独立 brainstorm → plan 循环。

3. **数据层完全不动：** schema v10 / `RecordTypeModel` / 金额 Long 分 / WebDAV / Proto 全部保留。ArkUI-X 只重写 UI + Repository 适配层。

4. **150+ 图标复用：** 关键决策。不重绘图标，AVD → SVG 自动批量迁移（Task 6）。

5. **组件命名前缀：** Android v1 用 `Cb*`（CbCard, CbButton 等）。ArkUI-X v2 保持同样前缀，方便跨项目对齐讨论。
