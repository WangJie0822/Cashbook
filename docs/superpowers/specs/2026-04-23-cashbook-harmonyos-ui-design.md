# Cashbook 下一代 UI/UX 设计方案 · HarmonyOS NEXT × ArkUI-X

**Status:** Draft (v5 brainstorm)
**Date:** 2026-04-23
**Target Platform:** HarmonyOS NEXT（主）· Android · iOS（通过 ArkUI-X 跨平台）
**Design Language:** Neo-Modern Chinese Bento · 中国古典色 + 现代扁平 + HMOS 沉浸光感
**Scope:** 覆盖现有 Cashbook 全功能（22+ 屏幕 + 8 类弹窗），双主题（Light/Dark），不改动业务逻辑/数据层/金额存储约定

---

## 1. 背景与目标

### 1.1 背景
当前 Cashbook 是 Android Only 的 Kotlin/Jetpack Compose + Material 3 应用（种子色 `#03776A`）。现状 UI 已完整可用，但：
1. 视觉同质化（Material 3 通用感，缺乏品牌识别）
2. 单平台，无法服务 HarmonyOS / iOS 用户
3. 设计规范存在但缺少"现代气质"（大数字、Bento、徽章、实时指示器等）

### 1.2 目标
产出一套**平台无关的设计 spec + ArkUI-X 参考实现路径**，支持：
- HarmonyOS NEXT 作为主目标平台（原生沉浸光感 / BlurStyle / 一镜到底）
- Android / iOS 通过 ArkUI-X 编译同一套代码（`.ets`）
- 视觉语言：中国古典色系 + 扁平卡片 + 科技感 + 多层渐变背景
- 明暗双套 + 全功能覆盖（首页 / 账本 / 记一笔 / 统计 / 我的 / 各管理页）

### 1.3 非目标（明确排除）
- 不改动数据库 schema（复用 v10，含 RecordTable / TypeTable / AssetTable 等）
- 不改动金额约定（`Long` 分，`core/common/ext/Money.kt` 保留）
- 不迁移业务逻辑（domain / data 层不动）
- 现有 Android 版本不立即下线，作为 v1 并存一段过渡期
- 不做小窗 / 折叠屏适配（单独后续 spec）

---

## 2. 设计原则

| 原则 | 要点 |
|---|---|
| **实色为主** | Hero 深色反色大卡 / 主卡片实色 / 次卡彩色 tint · 禁 heavy glassmorphism 装饰性 blur |
| **HMOS 沉浸光感** | 仅在悬浮层（顶栏 / 底栏 / Sheet / Modal scrim）使用 `BlurStyle` 原生 API |
| **中国古典色现代化** | 青瓷 / 朱砂 / 松绿 / 秋香 / 胭脂 / 青石 · 饱和度中等偏暗 · 对古典色做现代化对比提亮 |
| **Bento 信息密度** | 不规则网格（1.4fr + 1fr + 跨行大卡）· 一屏看全概览 |
| **Editorial 文案** | 衬线 Display 字用于大号金额 · 指标胶囊化（▲2.4%） · 语义化 tagline（"余 12 天 · 日均 2,340"） |
| **两级分类原生** | 基于 `RecordTypeModel.typeLevel` (FIRST/SECOND) + `parentId` 已有结构，非虚构 |
| **图标复用** | 现有 `vector_type_*_24.xml` 150+ SVG 直接复用，不重做 |
| **触控符合 HMOS** | 最小 40×40vp（HMOS 推荐）/ 触控反馈 < 150ms / 一镜到底沉浸 |

---

## 3. 设计 Tokens

### 3.1 色彩系统

#### Light · 青瓷月光

| Token | Hex | 语义 |
|---|---|---|
| `--bg` | `#F6F5EE` | 月光白 · 页面底层 |
| `--surface` | `#FEFDF9` | 奶白 · 一级卡片（半透 0.82 + blur 4 落地）|
| `--surface-2` | `#F9F8F0` | 素白 · 二级卡片 / 键盘背景 |
| `--text` | `#1A1E24` | 玄墨 · 主文字 |
| `--text-2` | `#4F5865` | 次文字 |
| `--text-3` | `#8A8F95` | 弱文字 / 说明 |
| `--border` | `#E8E3D3` | 茶白 · 卡片边框 |
| `--divider` | `#EFECE0` | 分割线 |
| `--primary` | `#4B8F92` | 青瓷 · 主色 |
| `--primary-strong` | `#2F6C70` | 松石 · 深青瓷（按压态 / 链接） |
| `--primary-deep` | `#1F4E52` | 深松石 · Hero 渐变终点 |
| `--primary-tint` | `#E0EEEC` | 青瓷稀 · 标签/选中底 |
| `--cinnabar` | `#C94247` | 朱砂 · 支出 / 强调 |
| `--cinnabar-tint` | `#F6DDDE` | 朱砂稀 · 支出 tint |
| `--bamboo` | `#6C8959` | 松绿 · 成功 / 收入 |
| `--bamboo-tint` | `#DCE5CF` | 松绿稀 |
| `--amber` | `#D49E35` | 秋香 · 警告 / 预算 |
| `--amber-tint` | `#F4E5C3` | 秋香稀 |
| `--azurite` | `#4A6A99` | 青石 · 信息 / 转账 |
| `--azurite-tint` | `#D9E1ED` | 青石稀 |
| `--yanzhi` | `#B0474B` | 胭脂 · 危险 / 删除 |

#### Dark · 黛青光感

| Token | Hex | 语义 |
|---|---|---|
| `--bg` | `#0C1220` | 深黛 · 页面底层 |
| `--surface` | `#161E32` | 黛青 · 一级卡片（半透 0.75 + blur 4） |
| `--surface-2` | `#1F2940` | 云青 · 二级卡片 |
| `--text` | `#EDE9DA` | 素白 · 主文字 |
| `--text-2` | `#B0B6C4` | 次文字 |
| `--text-3` | `#777F90` | 弱文字 |
| `--border` | `#2F3E57` | 蓝黛 · 边框 |
| `--divider` | `#1F2A3D` | 分割线 |
| `--primary` | `#A3C8C5` | 月蓝 · 主色 |
| `--primary-deep` | `#6FA8A5` | 霜青 · 强调 |
| `--primary-tint` | `rgba(163,200,197,0.16)` | 月蓝稀 |
| `--cinnabar` | `#E87A7F` | 朱砂稀 · 支出 |
| `--cinnabar-tint` | `rgba(232,122,127,0.18)` | |
| `--bamboo` | `#AED1BB` | 青玉 · 收入 |
| `--amber` | `#E2B66D` | 秋香稀 · 警告 |
| `--azurite` | `#9BB5D8` | 蓝石 · 信息 |

### 3.2 字体系统

| Role | Font Stack | 用途 |
|---|---|---|
| **Body** | `HarmonyOS Sans SC, PingFang SC, Noto Sans SC, Inter, sans-serif` | 正文 / 标签 / 按钮 |
| **Display (Serif)** | `Fraunces, Source Han Serif SC, Noto Serif SC, serif` | 大号金额 / 页面大标题 |
| **Numeric (Tabular)** | `HarmonyOS Sans SC + font-feature-settings: 'tnum'` 或 `JetBrains Mono` | 所有金额 / 统计数字 |

**字号阶梯（dp）：** 10 · 11 · 12 · 13 · 14 · 16 · 18 · 22 · 28 · 36

**字重：** Regular 400 / Medium 500 / SemiBold 600 / Bold 700 / ExtraBold 800

**Display 样式：** `letter-spacing: -0.025em` · tabular nums · 行高 1.05-1.1（紧凑）

### 3.3 间距 · 圆角 · Elevation

- **Spacing scale (4pt rhythm):** 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48
- **Radius scale:** 6 (chip) · 10 (button) · 14 (stat card) · 16 (card) · 20 (sheet) · 999 (pill)
- **Elevation:**
  - 0 Flat: `none`
  - 1 Hairline: `0 1px 2px rgba(0,0,0,0.04)` (card)
  - 2 Raised: `0 4px 12px rgba(0,0,0,0.08)` (menu)
  - 3 Overlay: `0 12px 36px rgba(0,0,0,0.15)` (bottom sheet)
  - 4 Modal: `0 20px 40px rgba(0,0,0,0.18)` (dialog)
  - Hero Dark 卡：`0 8px 22px rgba(47,108,112,0.28)` + `inset 0 1px 0 rgba(255,255,255,0.12)`

---

## 4. 整屏背景渐变（v5 核心）

### 4.1 四层叠加规范

#### Light
```
layer 1 (左上光晕): radial-gradient(ellipse 75% 45% at 15% 0%,
                     rgba(75,143,146,0.12) 0%, transparent 52%)  /* 青瓷 */
layer 2 (右上光晕): radial-gradient(ellipse 60% 30% at 90% 22%,
                     rgba(212,158,53,0.08) 0%, transparent 48%)  /* 秋香 */
layer 3 (底部光晕): radial-gradient(ellipse 70% 35% at 50% 102%,
                     rgba(108,137,89,0.09) 0%, transparent 55%)  /* 松绿 */
layer 4 (主基底):   linear-gradient(178deg,
                     #F3F2E6 0%, #F6F5EE 38%, #F8F6EB 78%, #F9F6E7 100%)
```

#### Dark
```
layer 1: radial-gradient(ellipse 75% 45% at 15% 0%,
          rgba(163,200,197,0.16) 0%, transparent 52%)   /* 月蓝 */
layer 2: radial-gradient(ellipse 60% 30% at 90% 22%,
          rgba(155,181,216,0.10) 0%, transparent 48%)   /* 蓝石 */
layer 3: radial-gradient(ellipse 70% 35% at 50% 102%,
          rgba(174,209,187,0.09) 0%, transparent 55%)   /* 青玉 */
layer 4: linear-gradient(178deg,
          #0D1426 0%, #0C1220 38%, #0E1528 78%, #101830 100%)
```

### 4.2 ArkTS 原生实现

```arkts
@Component
struct GradientBackground {
  build() {
    Stack() {
      // Layer 4 · 主基底
      Column().width('100%').height('100%').linearGradient({
        angle: 178,
        colors: [['#F3F2E6', 0], ['#F6F5EE', 0.38],
                 ['#F8F6EB', 0.78], ['#F9F6E7', 1]]
      })
      // Layer 1 · 左上青瓷
      Column().width('100%').height('100%').radialGradient({
        center: ['15%', '0%'],
        radius: '60%',
        colors: [['rgba(75,143,146,0.12)', 0], ['rgba(75,143,146,0)', 1]]
      })
      // Layer 2 · 右上秋香 (同理)
      // Layer 3 · 底部松绿 (同理)
    }
  }
}
```

### 4.3 卡片适配
- **主卡片** 改半透明 `rgba(254,253,249,0.82)` + `backdrop-filter: blur(4px)`（ArkUI-X: `.backgroundBlurStyle(BlurStyle.COMPONENT_THIN)` 较弱值 或 overlay 半透）
- **Tint 卡** 透明度 85%
- **Hero 深色大卡保持实色**（主角不透底）
- **FAB 实色**（主 CTA 绝不透底）

---

## 5. HMOS 沉浸光感（BlurStyle 原生）

### 5.1 BlurStyle 对应表

| 使用场景 | ArkTS API | 近似 CSS |
|---|---|---|
| TopBar 滚动虚化 | `.backgroundBlurStyle(BlurStyle.COMPONENT_THIN)` | `blur(10px) saturate(160%) bg(62%)` |
| BottomTab / Sheet 背板 | `.backgroundBlurStyle(BlurStyle.COMPONENT_REGULAR)` | `blur(16px) saturate(170%) bg(55%)` |
| 菜单 / 悬浮面板 | `.backgroundBlurStyle(BlurStyle.COMPONENT_THICK)` | `blur(24px) saturate(180%) bg(45%)` |
| Modal scrim 底层 | `.backgroundBlurStyle(BlurStyle.BACKGROUND_THIN)` | `blur(6px) bg(rgba 38%)` |
| 主卡片 | **不使用磨砂**，保持实色或半透 | `rgba(0.82) + blur(4px)` 轻度 |

### 5.2 Options 约定
- `adaptiveColor: AdaptiveColor.DEFAULT`（跟随底层内容着色）
- `colorMode: ThemeColorMode.SYSTEM`（跟随系统明暗）
- `scale`: 保持默认

---

## 6. 组件规范

### 6.1 原子组件 → ArkUI-X 映射

| 设计组件 | ArkUI-X 组件 | 关键属性 |
|---|---|---|
| 页面骨架 | `Navigation` + `Stack` | `titleMode` / safeArea |
| 底部 Tab | `Tabs`（BOTTOM）| barMode Fixed · animationDuration 280 |
| FAB | `Button` in `Stack` with Circle type | elevation 6 · 渐变色 |
| 主卡片 | `Column` + borderRadius 16 + border 1 | backdropBlurStyle COMPONENT_THIN |
| Hero 反色卡 | `Column` + `LinearGradient` + `RadialGradient` | 155° 渐变 + 顶部高光 |
| 列表 | `List` + `ListItem` | stickyHeader · cachedCount 10 |
| Bento 网格 | `Grid` + `GridItem` (span) | columnsTemplate "1.4fr 1fr" |
| Chip 胶囊 | `Row` in pill shape | borderRadius 999 · padding (4,10) |
| Segment Tab | `Tabs` + 自定义 TabBar | 自画激活背景 |
| 底部 Sheet | `bindSheet` / `Panel` half | dragBar · halfHeight 60% |
| 日历 | `Grid` 7 列 + `CalendarPicker` | selectedIndex · dotIndicator |
| 数字键盘 | `Grid` 4×4 + `Button` | 自定义运算符 |
| 饼图 / 环图 | `Canvas` drawArc | accessibility description |
| Sparkline | `Path` + `PolyLine` / 自绘 `Canvas` | |
| Progress 条 | `Progress` LinearStyleOptions | color:primary |
| Progress 环 | `Progress` RingStyleOptions | strokeWidth 5 |
| Pulse dot | 自绘 `Circle` + `animateTo` | 2s infinite |
| 对话框 | `AlertDialog` / 自定义 Dialog | scrim via BACKGROUND_THIN |
| Toast | `promptAction.showToast` | duration 2500ms |

### 6.2 按钮规范

| 类型 | Light | Dark |
|---|---|---|
| **Primary** | bg `linear(135°, #4B8F92 → #2F6C70)` · text `#FFF` · shadow `0 4px 12px rgba(75,143,146,0.28)` | bg `linear(135°, #A3C8C5 → #6FA8A5)` · text `#0C1220` |
| **Secondary** | bg `transparent` · border `1px #4B8F92` · text `#2F6C70` | border `1px #A3C8C5` · text `#A3C8C5` |
| **Text** | bg `transparent` · text `#2F6C70` | text `#A3C8C5` |
| **Destructive** | bg `#C94247` · text `#FFF` | bg `#E87A7F` · text `#0C1220` |
| **Disabled** | bg `#F9F8F0` · border · text `#8A8F95` | bg `#1F2940` · text `#777F90` |
| **FAB** | bg gradient + halo + `inset 0 1px 0 rgba(255,255,255,0.25)` | 同理 dark |

### 6.3 徽章（Badge）

```
▲ up:    bg rgba(bamboo, 0.16) · text #4E6C3A · pill
▼ down:  bg rgba(cinnabar, 0.14) · text #A0353A · pill
→ neutral: bg rgba(0,0,0,0.06) · text text-2 · pill
```

### 6.4 Pulse dot
- 7×7 圆形 · 松绿或语义色 · `animation: pulse 2s infinite`
- 用于：已同步状态 / 新记录实时提示 / 今日还款提醒

---

## 7. 图标系统

### 7.1 来源
**复用现有 `core/ui/src/main/res/drawable/vector_type_*_24.xml`**（150+ 个）。ArkUI-X 通过 `$r('app.media.vector_type_XXX_24')` 引用，或转 SVG 资源到 `resources/media/`。

### 7.2 大类 → iconName 主图标映射

| 一级分类 | iconName | 分类色 tint |
|---|---|---|
| 餐饮 | `vector_type_dining_24` | 青瓷 tint |
| 购物 | `vector_type_shopping_24` | 朱砂 tint |
| 交通 | `vector_type_traffic_24` | 松绿 tint |
| 居家 | `vector_type_housing_24` | 青石 tint |
| 娱乐 | `vector_type_amusement_24` | 秋香 tint |
| 医疗 | `vector_type_medical_24` | 朱砂 tint |
| 学习 | `vector_type_study_24` | 青瓷 tint |
| 通讯 | `vector_type_communication_24` | 青石 tint |
| 人情 | `vector_type_interpersonal_24` | 胭脂 tint |
| 育儿 | `vector_type_parenting_24` | 秋香 tint |
| 汽车 | `vector_type_car_24` | 松绿 tint |
| 宠物 | `vector_type_pet_24` | 朱砂 tint |
| 美容 | `vector_type_beauty_24` | 胭脂 tint |
| 投资 | `vector_type_investment_24` | 秋香 tint |
| 工资（收入）| `vector_type_salary_24` | 松绿 tint |
| 奖金（收入）| `vector_type_bonus_24` | 松绿 tint |
| 转账 | `vector_type_account_transfer_24` | 青石 tint |
| 其他 | `vector_type_other_24` | 茶白 tint |

### 7.3 UI 基础图标（非分类）
复用 `CbIcons.kt` Material Icons 清单（现有），平台级图标保留：Search / Settings / Add / Close / Check / Back / DateRange 等。ArkUI-X 侧对应 `@ohos/icon` 或自画 SVG。

---

## 8. 分类结构（基于 `RecordTypeModel` 现有结构）

### 8.1 字段映射
```kotlin
data class RecordTypeModel(
  val id: Long,          // 主键
  val parentId: Long,    // 子类时父类 id（一级为 -1）
  val name: String,      // 类型名
  val iconName: String,  // 图标资源名（对应 vector_type_*）
  val typeLevel: TypeLevelEnum,        // FIRST(0) / SECOND(1)
  val typeCategory: RecordTypeCategoryEnum, // EXPENDITURE(0) / INCOME(1) / TRANSFER(2)
  val protected: Boolean,// 受保护（系统类不可删）
  val sort: Int,         // 排序
  val needRelated: Boolean // 是否需要关联记录（如还款）
)
```

### 8.2 查询约定
- 一级分类：`WHERE parentId = -1 AND typeCategory = ?`
- 二级分类：`WHERE parentId = <一级 id>`
- UI 展示：一级横滚 icon grid → 选中后展示二级 chip
- 默认选中：最常用的一个二级（通过 usage count 统计，可选后续优化）
- 固定类型保留：`RECORD_TYPE_BALANCE_EXPENDITURE (-1101)` / `RECORD_TYPE_BALANCE_INCOME (-1102)`

### 8.3 迁移
Schema v10 已有 `parentId` / `typeLevel` 字段，**无需 schema 变更**。默认 seed 数据应在 `CashbookDatabase.Callback.onCreate()` 中预置（非本 spec 范畴，后续 plan 中处理）。

---

## 9. 信息架构 (IA)

### 9.1 底部 Tab（4 项）+ FAB

```
┌─────────────────────────────────────────┐
│  首页  │  账本  │   ＋   │  统计  │  我的  │
│ Home   │Records │  FAB   │Stats   │Me/Set  │
│Dashboard│+Search │ (中心)  │        │        │
└─────────────────────────────────────────┘
```

- FAB 居中凸起（不占 Tab slot），点击 → 弹全屏记一笔
- 长按 FAB → 快捷记一笔（常用分类 chip 列表）

### 9.2 顶层 → 二级栈

```
首页 ─┬─ [时间筛选 ▾] → DatePicker Sheet
      ├─ [账本切换 ▾] → Books Sheet
      ├─ 分类卡 click → 分类统计
      ├─ 记录 click   → EditRecord
      └─ 资产 click   → AssetInfo

账本 ─┬─ 流水 / 日历 / 搜索（Segment Tab）
      ├─ 记录 click → EditRecord
      └─ 🔍 → SearchScreen

统计 ─┬─ 月 / 年 / 自定义
      ├─ 支出 / 收入 切换
      ├─ 按类别 / 标签 / 资产
      └─ 分类 click → TypedAnalytics

我的 ─┬─ 账本管理 → EditBook
      ├─ 分类管理 → CategoryEdit
      ├─ 标签管理 → TagEdit
      ├─ 资产管理 → AssetList → AssetInfo → EditAsset
      ├─ 隐藏资产 → InvisibleAssets
      ├─ 账单导入 → RecordImport
      ├─ 备份恢复 → BackupAndRecovery
      ├─ 主题 → ThemeSettings (新增)
      ├─ 隐私/指纹 → SecuritySettings
      └─ 关于 → AboutUs / Markdown
```

### 9.3 导航规则
- 所有二级页面使用 `Navigation` 栈，支持系统返回手势
- Back 必须保留列表滚动位置 / 筛选状态（用 `@State` + `Keepalive`）
- 底部 Tab 切换不经过动画（即时切换，符合 HMOS 规范）
- 二级页面切换用 `slide-in from right` 280ms（HMOS 默认）

---

## 10. 关键屏幕规范

### 10.1 首页 Dashboard（Bento + Hero）

**结构（从上至下）：**
1. 顶栏（磨砂 COMPONENT_THIN）：左 `[2026·04 ▾]` 时间筛选 · 右 `🔍` + `[W 日常 ▾]` 账本切换胶囊
2. **Hero Dark 卡**（青瓷深底 + 米白字）：
   - 顶部 label "净资产" + pulse dot "已同步"
   - Display 大号 `¥128,450.00`
   - 徽章 `▲ 2.4% 本月` + tagline `余 12 天 · 日均 2,340`
   - 底部渐变填充 sparkline（近 30 天净值）
3. **Bento 网格**（1.4fr 1fr）：
   - 收入卡（松绿 tint）+ mini sparkline
   - 支出卡（朱砂 tint）+ 7 日柱状
   - 预算环（秋香 tint，跨 2 列）+ 环图 73% + tagline
4. 分类本月：3 格（餐饮/购物/交通）+ 每格进度条
5. 最近记录：3 条（图标 + 名称 + 子类 caption + 金额）
6. 底栏（磨砂 COMPONENT_REGULAR）：4 Tab + FAB 凸起

**状态：**
- 滚动时 Hero 不 sticky
- TopBar 跟随滚动虚化强度（可选动效：滚动距离 0→48px 时 BlurStyle 从 NONE 渐变到 COMPONENT_THIN）

### 10.2 记一笔（两级分类 + 键盘）

**结构：**
1. 顶栏：`✕ 取消` · 标题"记一笔" · `保存`（禁用态直到金额合法）
2. Segment Tab：支出 / 收入 / 转账
3. Display 金额显示区（按主色着色 + 备注副标题）
4. **一级分类** 横滚图标 grid（一屏 8 项可见，超出左右滑动；支出约 14 个 / 收入约 3 个 / 转账 1 个 + "＋更多"）
5. **二级分类** chip 横滚（选中一级后展示该一级下所有二级 + "＋" 新建；默认选中最近使用）
6. 资产 / 日期 · 两格并排
7. 标签 chip 可多选（常用 + "＋"）
8. 九宫格键盘：数字 + 运算符（+/−/×/÷）+ "完成" 跨 2 行

**交互：**
- 金额支持表达式计算（保留现有 `CalculatorUtils` 逻辑）
- 一级切换时 **二级默认选中最近使用的**（降低点击成本）
- 备注 tap 唤起系统键盘，键盘弹起时九宫格自动隐藏，收起后恢复

### 10.3 账本（Records）

**Segment Tab：流水 / 日历 / 搜索**

#### 10.3.1 流水
- 月份选择器 `[2026·04 ▾]` + 月净结余
- 按日分组 Sticky Header：`11 月 18 日 · 周二 · 日净 -92.50`
- 记录卡：图标 + 名称 · 子类 caption · 资产 · 时间 · 金额（支出 cinnabar / 收入 bamboo）
- 滑动操作：左滑 → 编辑 / 右滑 → 删除（+ 确认 Dialog）

#### 10.3.2 日历
- 月历 7×6 grid · 每日显示净收支小字
- 选中日：主色高亮，下方显示当日明细
- 切月：上下滑动

#### 10.3.3 搜索
- 搜索框顶部 · 默认显示"最近搜索"
- 匹配：关键词高亮 · 支持按 类别 / 标签 / 资产 / 金额区间过滤

### 10.4 统计（Analytics）

**Segment Tab：周 / 月 / 年 / 自定义**

**结构：**
1. Hero Dark 卡（本期总支出 + 环图预算占比 + 徽章环比）
2. 类别分布卡：Donut Chart（≤5 色段 + 最大段居 12 点 + 标签图例）
3. 趋势卡（跨 2 列）：Line Chart 本周 vs 上周（实线 vs 虚线）
4. 双小卡 Bento：日均支出 + 最大单笔
5. 可切换指标：支出 / 收入 · 按类别 / 标签 / 资产

**图表规范：**
- 饼图：5 色段上限（餐饮/购物/交通/娱乐/其他）· 超出自动合并到"其他"
- 线图：平滑曲线 + 端点圆点 + 触摸显示 tooltip
- 柱图：纯色 + 最高点高亮为 cinnabar
- Ring：主色环 73% + 灰色底环

### 10.5 资产（Asset List）

**结构：**
1. Hero Dark 卡：总资产大号 + 净资产/负债两列 + 比例条（储蓄 43% / 第三方 20% / 投资 24% / 负债 13%）+ 徽章
2. 分组（可折叠）：
   - **资金账户**（储蓄卡/第三方）· 每项带 sparkline 趋势 + 徽章变动率
   - **信用卡** · 每项带账单日 / 还款日（临近还款日 pulse dot 提醒）
   - **投资** · 每项带持仓 + 盈亏徽章
   - **债务**（借入/借出）
3. 隐藏资产入口（底部 链接）

### 10.6 资产详情（AssetInfo）

**结构：**
1. 顶栏：‹ 返回 · 资产名 · ⋯
2. Hero 卡（反色）：账户信息（类型 / 余额 / 账单日 / 还款日）
3. 近 30 日 sparkline 卡
4. Segment Tab：明细 / 账单 / 还款
5. 下方列表 + FAB "在此记一笔"

### 10.7 管理页（统一规范）

账本 / 分类 / 标签 / 资产管理遵循：
- 顶栏：‹ + 标题 + ＋ 新建
- 列表：icon + 名称 + 子项数 + ▾/▸ 折叠指示器
- 点击一级：展开二级 chip grid
- 长按：拖动排序
- 左滑：编辑 / 隐藏 / 删除

### 10.8 账单导入

1. 选择来源（支付宝 CSV / 微信 / 京东 / 通用 CSV）
2. 已读取 N 条 · 自动识别 vs 需手动 vs 重复跳过
3. 示例预览（可修改分类）
4. 导入按钮（主 CTA）

### 10.9 备份与恢复

1. WebDAV 区：服务商信息 + 上次备份时间 + 备份/恢复按钮
2. 本地备份列表：文件名 + 大小
3. 自动备份设置（频率 chip）
4. 导出 CSV

### 10.10 设置 / 我的

1. 头像 + 账本信息卡
2. 管理类列表：账本 / 标签 / 分类 / 资产 / 导入 / 备份
3. 偏好类列表：主题（新增）/ 隐私（指纹）/ 通知
4. 关于（版本 / 更新日志 / 开源协议）

### 10.11 其他
- Launcher / Splash：品牌色 + LOGO 动画（保留）
- Markdown 查看器（更新日志 / 隐私政策）
- 空状态 / 错误状态 / 加载骨架（需补，后续 plan）

---

## 11. 弹窗规范

### 11.1 八类弹窗（全部基于 tokens）

| # | 类型 | 用例 | 关键元素 |
|---|---|---|---|
| 1 | 删除确认 Dialog | 删除记录 / 删除分类 | 标题 + 说明 + 取消 + 删除（cinnabar） |
| 2 | 三选一警示 Dialog | 退出未保存 | 警告图标 + 继续编辑 / 保存草稿 / 放弃（text cinnabar） |
| 3 | 成功反馈 Dialog | 备份成功 | 成功图标（bamboo）+ 说明 + 好的 |
| 4 | 顶部 Toast | 已保存 · 可撤销 | 暗底白字 + icon + 撤销链接 · 3s 自动消失 · aria-live |
| 5 | 日期 Sheet | 选择日期 | drag bar + 年 tab + 月 grid + 日 grid + 完成 |
| 6 | 账本切换 Sheet | 切换账本 | drag bar + 账本列表 + ＋ 新建 |
| 7 | 分类两级 Sheet | 选分类 | 一级 icon grid + 二级 chip + 确认 |
| 8 | 右上角菜单 Popup | 资产 ⋯ | 编辑 / 记一笔 / 归档 / 删除 |

**补充弹窗（spec 增量）：**
- 冲突解决：导入重复记录 → 覆盖 / 跳过 / 两者保留
- 网络错误：WebDAV 连接失败 → 重试 / 切换服务器 / 导出本地
- 权限请求：通知 / 存储 / 指纹（首次使用时）
- 恢复进度：恢复备份中 → Progress + 取消

### 11.2 弹窗基础样式
- Dialog：18px border-radius · 18px padding · shadow Modal (`0 20px 40px rgba(0,0,0,0.18)`) + `inset 0 1px 0 rgba(255,255,255,0.9)`
- Sheet：20px top-radius · 14px padding · drag bar 32×4 · shadow Overlay + BlurStyle COMPONENT_REGULAR
- Scrim：BACKGROUND_THIN · rgba(10,18,30,0.38) 典型 38-55%
- Toast：12px radius · bg `rgba(22,30,50,0.95)` + 白字（跨 Light/Dark 样式一致 · 暗底高对比 · 便于识别临时提示）· 顶部 50px 距离 · 3s 自动消失 · aria-live polite

---

## 12. 动效 Motion

### 12.1 时长
- 微交互（press / state）：150ms
- 卡片 enter/exit：280ms
- 页面切换：280ms（enter）/ 180ms（exit）
- Sheet / Modal enter：300ms（slide + fade）
- Pulse 循环：2s infinite

### 12.2 缓动
- ArkTS `Curve.EaseOut` enter / `Curve.EaseIn` exit
- Spring：`springMotion(0.6, 1.2)` 默认
- 绝对禁用：linear UI 过渡、>500ms 无意义动效

### 12.3 HMOS 特有
- 页面切换 Shared Element：Hero 卡 scale + translate
- 触控反馈 scale 0.97 + haptic Light
- Reduced Motion：`AccessibilityManager.isReduceMotionEnabled` → 动效降级为 fade-only

### 12.4 Stagger
- List 首次渲染 stagger 30-50ms per item
- Bento 卡入场 80ms stagger（从左上到右下对角线）

---

## 13. 可访问性 (a11y)

| 项 | 要求 |
|---|---|
| 对比度 | 正文 ≥ 4.5:1 · 大字 ≥ 3:1 · 图标 ≥ 3:1 |
| 触控最小 | 40×40vp (HMOS) / 44×44pt (iOS) / 48×48dp (Android) · 统一使用 44vp |
| 动态字号 | 跟随系统 Font Scale，布局不截断 |
| Reduced Motion | 动效可降级 |
| 屏幕阅读器 | `accessibilityText` / `accessibilityDescription` 补全 |
| 色彩独立 | 金额正负用颜色 + 符号（+/-）双重表达 |
| 图标标签 | icon-only 按钮必须 `accessibilityText` |
| 错误反馈 | aria-live / role="alert" |
| 焦点 | Tab/遥控器支持（分屏/折叠/车载场景） |

---

## 14. 跨平台落地策略

### 14.1 技术栈
**ArkUI-X** · ArkTS `.ets` 一套代码三端输出：
- **HarmonyOS NEXT**（主）· DevEco Studio · API 12+
- **Android**（副）· 通过 ArkUI-X 工具链打包 APK
- **iOS**（副）· 通过 ArkUI-X 工具链打包 IPA

### 14.2 新项目结构（建议）

```
Cashbook/
├── app-android/              (现有 Kotlin Android · v1 维护态，不动)
├── app-arkui/                (新 ArkUI-X 项目 · v2)
│   ├── entry/
│   │   └── src/main/ets/
│   │       ├── pages/         (各 Screen)
│   │       ├── components/    (Card / Button / ChartCanvas)
│   │       ├── store/         (ArkTS 状态管理)
│   │       ├── data/          (@ohos.data.relationalStore + proto)
│   │       └── model/         (映射 core/model 的 ArkTS 类型)
│   ├── resources/
│   │   ├── media/             (复用 vector_type_*.svg 转入)
│   │   └── base/element/      (color.json / dimen.json / string.json)
│   └── oh-package.json5
├── core/                     (原 Kotlin 模块 · Android only 保留)
├── docs/
└── resources/
```

### 14.3 数据层迁移
- `RoomDatabase` → `@ohos.data.relationalStore`（SQLite wrapper）
- Schema 完全对齐 v10（同字段 / 同类型 / 同约束）
- DAO 手写（或用官方 orm-generator）
- Proto DataStore → `@ohos.data.preferences` + `@ohos.data.kv-store`

### 14.4 业务逻辑
- UseCase / Repository 层用 ArkTS 重写（模式相同，语言转换）
- WorkManager → `@ohos.backgroundTaskManager` + `WorkAgent`
- WebDAV 客户端 → `@ohos.net.http` + 自定义 WebDAV 协议实现
- 自定义 Lint 规则 → ArkTS Linter

### 14.5 共存/迁移策略
| 阶段 | 说明 |
|---|---|
| Stage 1 | ArkUI-X 项目搭建 + Design System token 定义 + 基础组件库 |
| Stage 2 | 首页 + 记一笔 + 账本流水 3 屏打通（MVP 可记账） |
| Stage 3 | 补齐统计 / 资产 / 管理页 / 弹窗 / 备份 |
| Stage 4 | HarmonyOS NEXT 发布 · 邀请用户测试 |
| Stage 5 | Android 版 ArkUI-X 打包 · 内测 · 与 v1 共存 |
| Stage 6 | iOS 版本 · 首次上架 |
| Stage 7 | Android v1 维护停止（仅保障稳定）· v2 成为主线 |

---

## 15. 测试策略

### 15.1 视觉回归
- **截图测试：** 使用 `@ohos.hypium` + UI 自动化 · 关键屏幕每日跑 baseline 对比
- 参考现有 Android Roborazzi 的 baseline 生成思路，但工具替换为 HMOS 原生
- 明暗双套都要跑

### 15.2 单元测试
- ArkTS 的 `describe/it` + `expect` · 目标：
  - 业务逻辑层（UseCase）覆盖 ≥ 70%
  - 状态管理 store ≥ 80%
  - 金额计算 / 格式化 ≥ 95%

### 15.3 端到端
- 跨平台 E2E 用 DevEco Testing（HMOS）+ Appium（Android/iOS）共用语义定位（`accessibilityText`）

---

## 16. 实施里程碑（建议阶段 · 非强制）

| M | 内容 | 预估屏数 | 工期 |
|---|---|---|---|
| M1 | Design System token + 基础组件库 + IA 骨架 | 0（仅 token + atom） | 1 周 |
| M2 | 首页 + 记一笔 + 账本流水（含日历）+ 搜索 | 4 屏 + 核心弹窗 | 2 周 |
| M3 | 统计 + 资产 + 资产详情 + 管理页（账本/分类/标签/资产）| 6 屏 + 弹窗 | 2 周 |
| M4 | 备份恢复 + 账单导入 + 设置/关于 + Markdown | 5 屏 | 1 周 |
| M5 | 视觉抛光 + 空/错/加载状态 + a11y + 动效 | 全局 | 1 周 |
| M6 | 截图测试 + 单元测试补齐 + 跨端验证（HMOS/Android/iOS） | 测试 | 1 周 |

**合计：** 约 8 周（2 个月）· 单人开发 · 可并行压缩 30-40%

---

## 17. 风险与开放问题

### 17.1 风险
1. **ArkUI-X 生态不成熟**：第三方库（WebDAV / OkHttp 替代 / 图表）可能需要自实现
2. **iOS 适配未完全验证**：需早期原型验证（M1 结束前完成 iOS hello-world）
3. **Room → RelationalStore 迁移工作量**：所有 DAO 手写 · 现有 Kotlin 项目 10+ 个 DAO
4. **备份兼容性**：v1 Android 的 .db 文件是否能被 v2 ArkUI-X 直接读取，需字节级对齐 SQLite 表
5. **默认 seed 分类数据**：一级 18 个（支出 14 + 收入 3 + 转账 1，见 §7.2）+ 二级 80-120 个；当前 Android 项目的 `TypeDao` / `CashbookDatabase.Callback` 可能已有 seed 逻辑，迁移到 ArkTS 时需核实并同步（若未 seed，则 spec §8.3 需在实施 plan 中补设计）

### 17.2 开放问题（待用户决策）

1. **是否同时维护 Android v1 长期？** 还是 ArkUI-X Android 包取代现有 Kotlin 版？
2. **Display 字体：** 选 `Fraunces`（现代衬线，带意大利斜体）还是 `Instrument Serif`（现代偏古典）还是思源宋体（更东方）？
3. **图标来源：** 150+ `vector_type_*.xml` 是复制到 ArkUI-X 项目 SVG 形式，还是发布成独立 icon package？
4. **Light Mode 切换：** 跟随系统 / 手动 / 定时（日出日落）三种模式都要支持？
5. **主题扩展：** v2 只做青瓷月光 + 黛青深宙，后续是否做多主题（朱墨 / 竹林 / 雪）？

---

## 18. 附录

### 18.1 参考 Mockup
位于 `.superpowers/brainstorm/59421-1776923418/content/`：
- `cashbook-harmonyos-v1.html` · 初版全屏
- `cashbook-harmonyos-v2-adjustments.html` · 色板调亮 + 两级分类 + 弹窗
- `cashbook-v3-harmonyos-native.html` · HMOS BlurStyle + 真 SVG + RecordTypeModel
- `cashbook-v4-modern-bento.html` · 现代化（Hero 反色 + Bento + Display Serif）
- `cashbook-v5-gradient-bg.html` · 多层渐变背景（当前版 · spec 对应）

### 18.2 与现有 UI 文档关系

| 文档 | 状态 |
|---|---|
| `docs/ui-design-specification.md` | v1 Android Material 3 规范 · 保留 · 标记"历史版本" |
| `docs/cross-platform-architecture.md` | v1 跨平台抽象 · 可作为 v2 架构起点 |
| `docs/cross-platform-detailed-design.md` | v1 详细设计 · v2 完成后替换或归档 |
| **本 spec (v2)** | ArkUI-X 下一代 UI · 主线文档 |

### 18.3 相关 Skill
- `/ui-ux-pro-max` · 已用于设计方向探索
- `/superpowers:writing-plans` · 下一步转入，生成实施计划
- `/superpowers:test-driven-development` · 实施期使用

---

**Changelog:**
- 2026-04-23 Draft v1（brainstorm v5 输出）· 包含 4 轮视觉迭代的综合结论
