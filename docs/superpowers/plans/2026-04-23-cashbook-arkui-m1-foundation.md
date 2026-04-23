# Cashbook ArkUI-X · M1 Foundation 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Cashbook 下一代 UI 的 ArkUI-X 项目基础（脚手架 + Design Token + 背景渐变 + 图标资源迁移 + 原子组件库 + 测试框架），产出可运行的 HelloWorld 与原子组件预览页（ComponentCatalog）。

**Architecture:** ArkUI-X 新项目 `app-arkui/` 与现有 Android Kotlin 项目（根目录）并存；Design Token 用 `resources/base/element/*.json` 定义（`color.json` / `float.json`），Dark 用 `resources/dark/element/color.json` 重写；原子组件放 `app-arkui/entry/src/main/ets/components/` 每个组件一个 `.ets` 文件 + 对应 `test/components/` 测试。所有组件通过 `ComponentCatalog` 页面手动 + 截图双重验证。

**Tech Stack:**
- ArkTS + ArkUI-X（DevEco Studio 5.0+，API 12+）
- 测试框架：`@ohos/hypium` + `@ohos/hamock`
- 构建：`hvigorw` (DevEco 内置)
- 图标：150+ Android Vector Drawable → SVG（Python 转换脚本）
- 无第三方 UI 依赖

**Spec 参考：** `docs/superpowers/specs/2026-04-23-cashbook-harmonyos-ui-design.md`
**像素级稿参考：** `.superpowers/brainstorm/59421-1776923418/content/cashbook-v6-pixel-perfect-part[1-5].html`

**M1 验收标准：**
- `hvigorw assembleHap` 在 HMOS simulator 成功构建并运行
- `hvigorw test` 全部绿灯
- ComponentCatalog 页面手动可以看到所有原子组件 Light/Dark 两套
- 所有 iconName 引用的 SVG 资源已迁入 `resources/base/media/`
- README 记录如何启动模拟器 + 跑测试

**M2-M6 将作为独立 plan 在 M1 完成后另立。**

---

### Task 1: 创建 ArkUI-X 项目脚手架

**Files:**
- Create: `app-arkui/` 子目录结构
- Create: `app-arkui/build-profile.json5`
- Create: `app-arkui/oh-package.json5`
- Create: `app-arkui/entry/build-profile.json5`
- Create: `app-arkui/entry/oh-package.json5`
- Create: `app-arkui/entry/src/main/module.json5`
- Create: `app-arkui/entry/src/main/ets/entryability/EntryAbility.ets`
- Create: `app-arkui/entry/src/main/ets/pages/Index.ets`
- Create: `app-arkui/entry/src/main/resources/base/profile/main_pages.json`
- Create: `app-arkui/hvigorfile.ts`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p /Users/wj/Work/Owner/StudioProjects/Cashbook/app-arkui/entry/src/main/{ets/{entryability,pages,components,utils},resources/{base/{element,profile,media},dark/element}}
mkdir -p /Users/wj/Work/Owner/StudioProjects/Cashbook/app-arkui/entry/src/ohosTest/ets/test
```

- [ ] **Step 2: 写项目根 `build-profile.json5`**

```json5
// app-arkui/build-profile.json5
{
  "app": {
    "signingConfigs": [],
    "products": [
      {
        "name": "default",
        "signingConfig": "default",
        "compatibleSdkVersion": "5.0.0(12)",
        "runtimeOS": "HarmonyOS",
        "buildOption": {
          "strictMode": { "caseSensitiveCheck": true, "useNormalizedOHMUrl": true }
        }
      }
    ],
    "buildModeSet": [{ "name": "debug" }, { "name": "release" }]
  },
  "modules": [
    { "name": "entry", "srcPath": "./entry", "targets": [{ "name": "default", "applyToProducts": ["default"] }] }
  ]
}
```

- [ ] **Step 3: 写 `oh-package.json5` 项目级**

```json5
// app-arkui/oh-package.json5
{
  "name": "cashbook-arkui",
  "version": "2.0.0",
  "description": "Cashbook Next-Gen UI · HarmonyOS NEXT + ArkUI-X",
  "license": "Apache-2.0",
  "main": "",
  "author": "",
  "modelVersion": "5.0.0",
  "dependencies": {},
  "devDependencies": {
    "@ohos/hypium": "1.0.19",
    "@ohos/hamock": "1.0.0"
  }
}
```

- [ ] **Step 4: 写 entry 模块 `module.json5`**

```json5
// app-arkui/entry/src/main/module.json5
{
  "module": {
    "name": "entry",
    "type": "entry",
    "description": "$string:module_desc",
    "mainElement": "EntryAbility",
    "deviceTypes": ["phone", "tablet", "2in1"],
    "deliveryWithInstall": true,
    "installationFree": false,
    "pages": "$profile:main_pages",
    "abilities": [
      {
        "name": "EntryAbility",
        "srcEntry": "./ets/entryability/EntryAbility.ets",
        "description": "$string:entry_desc",
        "icon": "$media:app_icon",
        "label": "$string:entry_label",
        "startWindowIcon": "$media:app_icon",
        "startWindowBackground": "$color:bg",
        "exported": true,
        "skills": [{ "entities": ["entity.system.home"], "actions": ["action.system.home"] }]
      }
    ]
  }
}
```

- [ ] **Step 5: 写 EntryAbility.ets**

```typescript
// app-arkui/entry/src/main/ets/entryability/EntryAbility.ets
import UIAbility from '@ohos.app.ability.UIAbility';
import window from '@ohos.window';

export default class EntryAbility extends UIAbility {
  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        console.error(`loadContent failed: ${err.code}, ${err.message}`);
      }
    });
  }
}
```

- [ ] **Step 6: 写最小 Index.ets（HelloWorld）**

```typescript
// app-arkui/entry/src/main/ets/pages/Index.ets
@Entry
@Component
struct Index {
  build() {
    Column() {
      Text('Cashbook v2')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor($r('sys.color.ohos_id_color_background'))
  }
}
```

- [ ] **Step 7: 写 main_pages.json**

```json
{
  "src": ["pages/Index"]
}
```

- [ ] **Step 8: 写占位 app_icon 和 base string**

```json
// app-arkui/entry/src/main/resources/base/element/string.json
{
  "string": [
    { "name": "module_desc", "value": "Cashbook entry module" },
    { "name": "entry_desc", "value": "Cashbook main ability" },
    { "name": "entry_label", "value": "Cashbook" }
  ]
}
```

放一个占位图（256×256 PNG）：
```bash
# 暂借原 Android 项目的 launcher 图标
cp /Users/wj/Work/Owner/StudioProjects/Cashbook/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png \
   /Users/wj/Work/Owner/StudioProjects/Cashbook/app-arkui/entry/src/main/resources/base/media/app_icon.png
```

- [ ] **Step 9: 构建验证**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/app-arkui
hvigorw clean --no-daemon
hvigorw assembleHap --mode module -p product=default --no-daemon
```

Expected: `BUILD SUCCESSFUL` · 在 `entry/build/default/outputs/default/` 生成 `.hap` 文件

- [ ] **Step 10: Commit**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]初始化 ArkUI-X 项目骨架"
```

---

### Task 2: Design Token · 色彩系统（Light + Dark）

**Files:**
- Create: `app-arkui/entry/src/main/resources/base/element/color.json`
- Create: `app-arkui/entry/src/main/resources/dark/element/color.json`
- Create: `app-arkui/entry/src/ohosTest/ets/test/ColorTokenTest.ets`

- [ ] **Step 1: 写 Light 色彩 token 失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/ColorTokenTest.ets
import { describe, it, expect } from '@ohos/hypium';
import resourceManager from '@ohos.resourceManager';

export default function ColorTokenTest() {
  describe('ColorTokens', () => {
    it('light_bg_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const color = await rm.getColor($r('app.color.bg').id);
      // F6F5EE in ARGB → 0xFFF6F5EE
      expect(color).assertEqual(0xFFF6F5EE);
    });

    it('light_primary_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const color = await rm.getColor($r('app.color.primary').id);
      expect(color).assertEqual(0xFF4B8F92);
    });

    it('light_cinnabar_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const color = await rm.getColor($r('app.color.cinnabar').id);
      expect(color).assertEqual(0xFFC94247);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd app-arkui && hvigorw test --no-daemon
```

Expected: FAIL `resource not found: app.color.bg`

- [ ] **Step 3: 写 Light `color.json`（完整 token 表，按 spec §3.1）**

```json
// app-arkui/entry/src/main/resources/base/element/color.json
{
  "color": [
    { "name": "bg", "value": "#F6F5EE" },
    { "name": "bg_gradient_start", "value": "#F3F2E6" },
    { "name": "bg_gradient_end", "value": "#F9F6E7" },
    { "name": "surface", "value": "#FEFDF9" },
    { "name": "surface_2", "value": "#F9F8F0" },
    { "name": "surface_translucent", "value": "#D2FEFDF9" },

    { "name": "text", "value": "#1A1E24" },
    { "name": "text_2", "value": "#4F5865" },
    { "name": "text_3", "value": "#8A8F95" },

    { "name": "border", "value": "#E8E3D3" },
    { "name": "divider", "value": "#EFECE0" },

    { "name": "primary", "value": "#4B8F92" },
    { "name": "primary_strong", "value": "#2F6C70" },
    { "name": "primary_deep", "value": "#1F4E52" },
    { "name": "primary_tint", "value": "#E0EEEC" },

    { "name": "cinnabar", "value": "#C94247" },
    { "name": "cinnabar_tint", "value": "#F6DDDE" },
    { "name": "yanzhi", "value": "#B0474B" },

    { "name": "bamboo", "value": "#6C8959" },
    { "name": "bamboo_tint", "value": "#DCE5CF" },

    { "name": "amber", "value": "#D49E35" },
    { "name": "amber_tint", "value": "#F4E5C3" },

    { "name": "azurite", "value": "#4A6A99" },
    { "name": "azurite_tint", "value": "#D9E1ED" },

    { "name": "glow_primary", "value": "#1F4B8F92" },
    { "name": "glow_amber", "value": "#14D49E35" },
    { "name": "glow_bamboo", "value": "#176C8959" }
  ]
}
```

注：前两位 hex 是 alpha。`#1F` ≈ 12% (rgb 0.12 = 30/255 ≈ 0x1E-1F)；`#14` ≈ 8%；`#17` ≈ 9%。

- [ ] **Step 4: 写 Dark `color.json`**

```json
// app-arkui/entry/src/main/resources/dark/element/color.json
{
  "color": [
    { "name": "bg", "value": "#0C1220" },
    { "name": "bg_gradient_start", "value": "#0D1426" },
    { "name": "bg_gradient_end", "value": "#101830" },
    { "name": "surface", "value": "#161E32" },
    { "name": "surface_2", "value": "#1F2940" },
    { "name": "surface_translucent", "value": "#C7161E32" },

    { "name": "text", "value": "#EDE9DA" },
    { "name": "text_2", "value": "#B0B6C4" },
    { "name": "text_3", "value": "#777F90" },

    { "name": "border", "value": "#2F3E57" },
    { "name": "divider", "value": "#1F2A3D" },

    { "name": "primary", "value": "#A3C8C5" },
    { "name": "primary_strong", "value": "#B8D4D1" },
    { "name": "primary_deep", "value": "#6FA8A5" },
    { "name": "primary_tint", "value": "#29A3C8C5" },

    { "name": "cinnabar", "value": "#E87A7F" },
    { "name": "cinnabar_tint", "value": "#2EE87A7F" },
    { "name": "yanzhi", "value": "#E8898D" },

    { "name": "bamboo", "value": "#AED1BB" },
    { "name": "bamboo_tint", "value": "#29AED1BB" },

    { "name": "amber", "value": "#E2B66D" },
    { "name": "amber_tint", "value": "#29E2B66D" },

    { "name": "azurite", "value": "#9BB5D8" },
    { "name": "azurite_tint", "value": "#299BB5D8" },

    { "name": "glow_primary", "value": "#29A3C8C5" },
    { "name": "glow_amber", "value": "#199BB5D8" },
    { "name": "glow_bamboo", "value": "#17AED1BB" }
  ]
}
```

- [ ] **Step 5: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter ColorTokenTest
```

Expected: 3/3 PASS

- [ ] **Step 6: Commit**

```bash
git add app-arkui/entry/src/main/resources/ app-arkui/entry/src/ohosTest/
git commit -m "[feat|arkui|foundation][公共]色彩 Token 定义 Light + Dark"
```

---

### Task 3: Design Token · 字号 / 间距 / 圆角

**Files:**
- Create: `app-arkui/entry/src/main/resources/base/element/float.json`
- Create: `app-arkui/entry/src/ohosTest/ets/test/DimensionTokenTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/DimensionTokenTest.ets
import { describe, it, expect } from '@ohos/hypium';
import resourceManager from '@ohos.resourceManager';

export default function DimensionTokenTest() {
  describe('DimensionTokens', () => {
    it('font_display_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const v = await rm.getNumber($r('app.float.font_display').id);
      expect(v).assertEqual(36);
    });

    it('spacing_16_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const v = await rm.getNumber($r('app.float.spacing_16').id);
      expect(v).assertEqual(16);
    });

    it('radius_card_defined', 0, async () => {
      const rm = await resourceManager.getResourceManager();
      const v = await rm.getNumber($r('app.float.radius_card').id);
      expect(v).assertEqual(16);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter DimensionTokenTest
```

Expected: FAIL `resource not found`

- [ ] **Step 3: 写 `float.json`**

```json
// app-arkui/entry/src/main/resources/base/element/float.json
{
  "float": [
    { "name": "font_10", "value": "10vp" },
    { "name": "font_11", "value": "11vp" },
    { "name": "font_12", "value": "12vp" },
    { "name": "font_13", "value": "13vp" },
    { "name": "font_14", "value": "14vp" },
    { "name": "font_16", "value": "16vp" },
    { "name": "font_18", "value": "18vp" },
    { "name": "font_22", "value": "22vp" },
    { "name": "font_28", "value": "28vp" },
    { "name": "font_display", "value": "36vp" },
    { "name": "font_display_lg", "value": "44vp" },

    { "name": "spacing_4", "value": "4vp" },
    { "name": "spacing_6", "value": "6vp" },
    { "name": "spacing_8", "value": "8vp" },
    { "name": "spacing_10", "value": "10vp" },
    { "name": "spacing_12", "value": "12vp" },
    { "name": "spacing_14", "value": "14vp" },
    { "name": "spacing_16", "value": "16vp" },
    { "name": "spacing_20", "value": "20vp" },
    { "name": "spacing_24", "value": "24vp" },
    { "name": "spacing_32", "value": "32vp" },
    { "name": "spacing_40", "value": "40vp" },
    { "name": "spacing_48", "value": "48vp" },

    { "name": "radius_chip", "value": "6vp" },
    { "name": "radius_button", "value": "10vp" },
    { "name": "radius_stat_card", "value": "14vp" },
    { "name": "radius_card", "value": "16vp" },
    { "name": "radius_sheet", "value": "20vp" },
    { "name": "radius_dialog", "value": "20vp" },
    { "name": "radius_pill", "value": "999vp" },

    { "name": "touch_min", "value": "44vp" },
    { "name": "list_row", "value": "56vp" },
    { "name": "fab_size", "value": "56vp" },
    { "name": "top_bar_content", "value": "48vp" },
    { "name": "bottom_bar", "value": "83vp" },
    { "name": "status_bar_safe", "value": "54vp" },
    { "name": "home_indicator_safe", "value": "34vp" }
  ]
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter DimensionTokenTest
```

Expected: 3/3 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]字号/间距/圆角 Token 定义"
```

---

### Task 4: 测试基础设施 + 测试运行规范

**Files:**
- Modify: `app-arkui/entry/src/ohosTest/ets/test/List.test.ets`
- Create: `app-arkui/entry/src/ohosTest/module.json5`
- Create: `app-arkui/entry/src/ohosTest/ets/TestAbility/TestAbility.ets`
- Create: `app-arkui/TESTING.md`

- [ ] **Step 1: 写测试入口 `List.test.ets`**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/List.test.ets
import ColorTokenTest from './ColorTokenTest';
import DimensionTokenTest from './DimensionTokenTest';

export default function testsuite() {
  ColorTokenTest();
  DimensionTokenTest();
}
```

- [ ] **Step 2: 写 ohosTest 模块 `module.json5`**

```json5
{
  "module": {
    "name": "entry_test",
    "type": "feature",
    "description": "Test module",
    "mainElement": "TestAbility",
    "deviceTypes": ["phone", "tablet"],
    "deliveryWithInstall": true,
    "installationFree": false,
    "pages": "$profile:test_pages",
    "abilities": [
      {
        "name": "TestAbility",
        "srcEntry": "./ets/TestAbility/TestAbility.ets",
        "description": "Test ability",
        "icon": "$media:app_icon",
        "label": "$string:entry_label",
        "exported": true
      }
    ]
  }
}
```

- [ ] **Step 3: 写 TestAbility.ets**

```typescript
// app-arkui/entry/src/ohosTest/ets/TestAbility/TestAbility.ets
import UIAbility from '@ohos.app.ability.UIAbility';
import AbilityDelegatorRegistry from '@ohos.app.ability.abilityDelegatorRegistry';
import hilog from '@ohos.hilog';
import { Hypium } from '@ohos/hypium';
import testsuite from '../test/List.test';

export default class TestAbility extends UIAbility {
  onCreate(want, launchParam) {
    hilog.info(0, 'TestAbility', 'onCreate');
    const abilityDelegator = AbilityDelegatorRegistry.getAbilityDelegator();
    const abilityDelegatorArgs = AbilityDelegatorRegistry.getArguments();
    Hypium.hypiumTest(abilityDelegator, abilityDelegatorArgs, testsuite);
  }
}
```

- [ ] **Step 4: 写 TESTING.md**

```markdown
# Cashbook ArkUI-X 测试指南

## 单元测试

```bash
# 运行全部测试
hvigorw test --no-daemon

# 运行指定测试类
hvigorw test --no-daemon -- --testFilter ColorTokenTest

# 带覆盖率
hvigorw test --no-daemon -- --coverage
```

## 测试约定

- 测试文件位于 `entry/src/ohosTest/ets/test/`，命名 `<Feature>Test.ets`
- 使用 `@ohos/hypium` 的 `describe / it / expect`
- 新 Test 文件必须在 `List.test.ets` 中 import 并注册
- 组件测试使用 `@ohos/hamock` mock resourceManager 等系统 API
- 每个 PR 必须保证测试全绿

## 截图测试（M6 规划）

使用 DevEco Testing + golden file 对比，baseline 位于 `entry/src/ohosTest/resources/baseline/`
```

- [ ] **Step 5: 运行所有测试验证**

```bash
hvigorw clean --no-daemon && hvigorw test --no-daemon
```

Expected: 全部 PASS · 日志末尾 `TestFinished result=success`

- [ ] **Step 6: Commit**

```bash
git add app-arkui/ 
git commit -m "[test|arkui|foundation][公共]测试框架骨架 + TESTING.md"
```

---

### Task 5: GradientBackground 组件（4 层渐变）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/GradientBackground.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/GradientBackgroundTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/GradientBackgroundTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { GradientBackground, GradientLayers } from '../../../main/ets/components/GradientBackground';

export default function GradientBackgroundTest() {
  describe('GradientBackground', () => {
    it('layers_count_is_4', 0, () => {
      expect(GradientLayers.length).assertEqual(4);
    });

    it('layers_include_primary_glow', 0, () => {
      const hasPrimary = GradientLayers.some(l => l.kind === 'radial' && l.semanticRole === 'primary_glow');
      expect(hasPrimary).assertTrue();
    });

    it('base_layer_is_linear', 0, () => {
      const base = GradientLayers.find(l => l.semanticRole === 'base');
      expect(base?.kind).assertEqual('linear');
      expect(base?.angle).assertEqual(178);
    });
  });
}
```

在 `List.test.ets` 中 import + 注册。

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter GradientBackgroundTest
```

Expected: FAIL `module not found`

- [ ] **Step 3: 写 `GradientBackground.ets`**

```typescript
// app-arkui/entry/src/main/ets/components/GradientBackground.ets

export interface GradientLayer {
  kind: 'linear' | 'radial';
  semanticRole: 'base' | 'primary_glow' | 'amber_glow' | 'bamboo_glow';
  // Linear
  angle?: number;
  colorStops?: Array<[string, number]>;
  // Radial
  centerX?: string;
  centerY?: string;
  radius?: string;
}

export const GradientLayers: GradientLayer[] = [
  {
    kind: 'linear',
    semanticRole: 'base',
    angle: 178,
    colorStops: [
      ['$r:app.color.bg_gradient_start', 0],
      ['$r:app.color.bg', 0.38],
      ['#F8F6EB', 0.78],
      ['$r:app.color.bg_gradient_end', 1.0]
    ]
  },
  { kind: 'radial', semanticRole: 'primary_glow', centerX: '15%', centerY: '0%', radius: '60%' },
  { kind: 'radial', semanticRole: 'amber_glow', centerX: '90%', centerY: '22%', radius: '45%' },
  { kind: 'radial', semanticRole: 'bamboo_glow', centerX: '50%', centerY: '102%', radius: '50%' }
];

@Component
export struct GradientBackground {
  build() {
    Stack() {
      // Layer 4 · Linear base
      Column()
        .width('100%').height('100%')
        .linearGradient({
          angle: 178,
          colors: [
            [$r('app.color.bg_gradient_start'), 0.0],
            [$r('app.color.bg'), 0.38],
            [$r('app.color.bg'), 0.78],
            [$r('app.color.bg_gradient_end'), 1.0]
          ]
        });

      // Layer 1 · Primary glow
      Column()
        .width('100%').height('100%')
        .radialGradient({
          center: ['15%', '0%'],
          radius: '60%',
          colors: [[$r('app.color.glow_primary'), 0.0], [$r('app.color.glow_primary'), 1.0]]
        })
        .opacity(1);

      // Layer 2 · Amber glow
      Column()
        .width('100%').height('100%')
        .radialGradient({
          center: ['90%', '22%'],
          radius: '45%',
          colors: [[$r('app.color.glow_amber'), 0.0], [$r('app.color.glow_amber'), 1.0]]
        });

      // Layer 3 · Bamboo glow
      Column()
        .width('100%').height('100%')
        .radialGradient({
          center: ['50%', '102%'],
          radius: '50%',
          colors: [[$r('app.color.glow_bamboo'), 0.0], [$r('app.color.glow_bamboo'), 1.0]]
        });
    }
    .width('100%')
    .height('100%');
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter GradientBackgroundTest
```

Expected: 3/3 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]背景多层渐变组件 GradientBackground"
```

---

### Task 6: 图标资源迁移脚本（AVD → SVG）

**Files:**
- Create: `scripts/avd-to-svg.py`
- Create: `scripts/avd-to-svg-test.py`
- Modify: `app-arkui/entry/src/main/resources/base/media/`（批量产物）

- [ ] **Step 1: 写失败测试**

```python
# scripts/avd-to-svg-test.py
import subprocess
import tempfile
import os
from pathlib import Path

SAMPLE_AVD = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <path
        android:fillColor="#515151"
        android:pathData="M12 2L2 22h20L12 2z" />
</vector>
'''

EXPECTED_SVG = '''<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="24" height="24">
  <path fill="#515151" d="M12 2L2 22h20L12 2z"/>
</svg>'''


def test_single_path_avd_to_svg():
    with tempfile.TemporaryDirectory() as tmp:
        input_path = Path(tmp) / 'input.xml'
        output_path = Path(tmp) / 'output.svg'
        input_path.write_text(SAMPLE_AVD, encoding='utf-8')

        subprocess.run(
            ['python3', 'scripts/avd-to-svg.py', str(input_path), str(output_path)],
            check=True
        )

        actual = output_path.read_text(encoding='utf-8').strip()
        assert actual == EXPECTED_SVG.strip(), \
            f"Mismatch:\nExpected:\n{EXPECTED_SVG}\n\nActual:\n{actual}"


def test_multi_path_avd_to_svg():
    multi_avd = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="1024" android:viewportHeight="1024">
    <path android:fillColor="#FF000000" android:pathData="M0 0h10v10H0z" />
    <path android:fillColor="#FF0000" android:pathData="M20 20h5v5H20z" />
</vector>'''
    with tempfile.TemporaryDirectory() as tmp:
        input_path = Path(tmp) / 'input.xml'
        output_path = Path(tmp) / 'output.svg'
        input_path.write_text(multi_avd, encoding='utf-8')

        subprocess.run(['python3', 'scripts/avd-to-svg.py', str(input_path), str(output_path)], check=True)
        content = output_path.read_text(encoding='utf-8')
        assert content.count('<path') == 2
        assert 'fill="#000000"' in content  # FF alpha stripped
        assert 'fill="#FF0000"' in content


if __name__ == '__main__':
    test_single_path_avd_to_svg()
    test_multi_path_avd_to_svg()
    print('All tests passed')
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook
python3 scripts/avd-to-svg-test.py
```

Expected: FAIL `FileNotFoundError: 'scripts/avd-to-svg.py'`

- [ ] **Step 3: 写 `scripts/avd-to-svg.py`**

```python
#!/usr/bin/env python3
"""Android Vector Drawable (AVD) → SVG 转换器.

只处理 Cashbook 项目用的子集：单色 path 填充。
从 vector_type_*_24.xml 批量转换时保留命名：vector_type_dining_24.xml → vector_type_dining_24.svg
"""
import argparse
import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

ANDROID_NS = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('', ANDROID_NS)


def _attr(elem, name, default=None):
    """读 android:foo 属性，去掉 namespace 前缀"""
    return elem.get(f'{{{ANDROID_NS}}}{name}', default)


def _normalize_color(color: str | None) -> str:
    """AVD 颜色格式 #AARRGGBB 或 #RRGGBB → SVG #RRGGBB (SVG 另外处理 alpha 用 fill-opacity)"""
    if not color:
        return '#000000'
    color = color.strip()
    if color.startswith('#') and len(color) == 9:  # #AARRGGBB
        return '#' + color[3:]
    return color


def _alpha_from_argb(color: str | None) -> float | None:
    if color and color.startswith('#') and len(color) == 9:
        aa = int(color[1:3], 16)
        if aa < 255:
            return round(aa / 255, 3)
    return None


def convert(input_path: Path, output_path: Path) -> None:
    tree = ET.parse(input_path)
    root = tree.getroot()
    vp_width = _attr(root, 'viewportWidth', '1024')
    vp_height = _attr(root, 'viewportHeight', '1024')
    # width / height for output SVG (device-pixel hint)
    out_w = _attr(root, 'width', '24dp').replace('dp', '').replace('sp', '')
    out_h = _attr(root, 'height', '24dp').replace('dp', '').replace('sp', '')

    paths = []
    for p in root.findall(f'{{{ANDROID_NS}}}path'):
        fill = _normalize_color(_attr(p, 'fillColor'))
        alpha = _alpha_from_argb(_attr(p, 'fillColor'))
        d = _attr(p, 'pathData', '')
        if not d:
            continue
        attrs = f'fill="{fill}"'
        if alpha is not None:
            attrs += f' fill-opacity="{alpha}"'
        paths.append(f'  <path {attrs} d="{d}"/>')

    svg_lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {vp_width} {vp_height}" width="{out_w}" height="{out_h}">',
        *paths,
        '</svg>',
    ]
    output_path.write_text('\n'.join(svg_lines), encoding='utf-8')


def main() -> int:
    parser = argparse.ArgumentParser(description='AVD XML → SVG converter')
    parser.add_argument('input', type=Path, help='Path to AVD XML file')
    parser.add_argument('output', type=Path, help='Output SVG file path')
    args = parser.parse_args()

    if not args.input.exists():
        print(f'Input not found: {args.input}', file=sys.stderr)
        return 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    convert(args.input, args.output)
    return 0


if __name__ == '__main__':
    sys.exit(main())
```

- [ ] **Step 4: 运行测试验证通过**

```bash
python3 scripts/avd-to-svg-test.py
```

Expected: `All tests passed`

- [ ] **Step 5: 批量迁移 vector_type_*.xml**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook
mkdir -p app-arkui/entry/src/main/resources/base/media
for f in core/ui/src/main/res/drawable/vector_type_*_24.xml; do
  name=$(basename "$f" .xml)
  python3 scripts/avd-to-svg.py "$f" "app-arkui/entry/src/main/resources/base/media/$name.svg"
done
ls app-arkui/entry/src/main/resources/base/media/ | wc -l
```

Expected: ≥ 150 个 `vector_type_*.svg` 生成

- [ ] **Step 6: 构建验证**

```bash
cd app-arkui && hvigorw assembleHap --mode module --no-daemon
```

Expected: BUILD SUCCESSFUL（验证 SVG 资源合法）

- [ ] **Step 7: Commit**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook
git add scripts/avd-to-svg.py scripts/avd-to-svg-test.py app-arkui/entry/src/main/resources/base/media/
git commit -m "[feat|arkui|foundation][公共]vector_type 图标 AVD→SVG 批量迁移脚本 + 产物"
```

---

### Task 7: PulseDot 组件

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/PulseDot.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/PulseDotTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/PulseDotTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { computePulseScale, PULSE_DURATION_MS } from '../../../main/ets/components/PulseDot';

export default function PulseDotTest() {
  describe('PulseDot', () => {
    it('pulse_duration_is_2000ms', 0, () => {
      expect(PULSE_DURATION_MS).assertEqual(2000);
    });

    it('scale_at_0_is_1', 0, () => {
      expect(computePulseScale(0)).assertEqual(1.0);
    });

    it('scale_at_0_7_peaks_at_2_5', 0, () => {
      const peak = computePulseScale(0.7);
      expect(peak).assertLargerOrEqual(2.4);
      expect(peak).assertLessOrEqual(2.6);
    });

    it('scale_at_1_back_to_1', 0, () => {
      expect(computePulseScale(1.0)).assertEqual(1.0);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter PulseDotTest
```

Expected: FAIL `module not found`

- [ ] **Step 3: 写 PulseDot.ets**

```typescript
// app-arkui/entry/src/main/ets/components/PulseDot.ets
import { Resource } from '@ohos.arkui';

export const PULSE_DURATION_MS = 2000;

/** 脉冲动画曲线：t∈[0,1]，0% 无 halo（scale=1），70% 峰值（scale=2.5），100% 淡出（scale=1） */
export function computePulseScale(t: number): number {
  if (t <= 0 || t >= 1) return 1.0;
  // piecewise: 0 → 0.7 加速放大，0.7 → 1.0 消失
  if (t <= 0.7) {
    return 1.0 + (t / 0.7) * 1.5; // 1.0 → 2.5
  }
  return 1.0; // fade out, halo opacity=0 handled separately
}

export function computePulseOpacity(t: number): number {
  if (t <= 0) return 0.6;
  if (t <= 0.7) return 0.6 * (1 - t / 0.7); // fade
  return 0;
}

@Component
export struct PulseDot {
  @Prop color: ResourceColor = $r('app.color.bamboo');
  @Prop size: number = 8;
  @State private progress: number = 0;

  aboutToAppear() {
    this.animate();
  }

  private animate(): void {
    animateTo(
      { duration: PULSE_DURATION_MS, iterations: -1, curve: Curve.EaseOut, playMode: PlayMode.Normal },
      () => { this.progress = 1; }
    );
  }

  build() {
    Stack() {
      // Halo
      Circle({ width: this.size, height: this.size })
        .fill(this.color)
        .opacity(computePulseOpacity(this.progress))
        .scale({ x: computePulseScale(this.progress), y: computePulseScale(this.progress) });
      // Core
      Circle({ width: this.size, height: this.size })
        .fill(this.color);
    }
    .width(this.size)
    .height(this.size);
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter PulseDotTest
```

Expected: 4/4 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]PulseDot 组件（实时同步指示）"
```

---

### Task 8: Badge 组件（up/down/neutral 三变体）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/Badge.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/BadgeTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/BadgeTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { BadgeVariant, badgeColors } from '../../../main/ets/components/Badge';

export default function BadgeTest() {
  describe('Badge', () => {
    it('up_uses_bamboo_tint', 0, () => {
      const colors = badgeColors(BadgeVariant.Up);
      expect(colors.background).assertContain('bamboo');
    });

    it('down_uses_cinnabar', 0, () => {
      const colors = badgeColors(BadgeVariant.Down);
      expect(colors.text).assertContain('#A0353A');
    });

    it('neutral_uses_surface', 0, () => {
      const colors = badgeColors(BadgeVariant.Neutral);
      expect(colors.background.toLowerCase()).assertContain('rgba(0,0,0,0.06)');
    });

    it('all_variants_have_same_height', 0, () => {
      const u = badgeColors(BadgeVariant.Up);
      const d = badgeColors(BadgeVariant.Down);
      const n = badgeColors(BadgeVariant.Neutral);
      expect(u.height).assertEqual(d.height);
      expect(d.height).assertEqual(n.height);
      expect(u.height).assertEqual(18);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter BadgeTest
```

Expected: FAIL

- [ ] **Step 3: 写 Badge.ets**

```typescript
// app-arkui/entry/src/main/ets/components/Badge.ets

export enum BadgeVariant {
  Up = 'up',
  Down = 'down',
  Neutral = 'neutral'
}

export interface BadgeColors {
  background: string;
  text: string;
  height: number; // vp
}

export function badgeColors(variant: BadgeVariant): BadgeColors {
  switch (variant) {
    case BadgeVariant.Up:
      return { background: 'rgba(108,137,89,0.18) /* bamboo */', text: '#4E6C3A', height: 18 };
    case BadgeVariant.Down:
      return { background: 'rgba(201,66,71,0.16) /* cinnabar */', text: '#A0353A', height: 18 };
    case BadgeVariant.Neutral:
      return { background: 'rgba(0,0,0,0.06)', text: '#4F5865', height: 18 };
  }
}

@Component
export struct Badge {
  @Prop variant: BadgeVariant = BadgeVariant.Neutral;
  @Prop text: string = '';

  build() {
    Row() {
      if (this.variant === BadgeVariant.Up) {
        Text('▲').fontSize($r('app.float.font_10')).fontColor('#4E6C3A').margin({ right: 3 });
      } else if (this.variant === BadgeVariant.Down) {
        Text('▼').fontSize($r('app.float.font_10')).fontColor('#A0353A').margin({ right: 3 });
      } else {
        Text('→').fontSize($r('app.float.font_10')).fontColor('#4F5865').margin({ right: 3 });
      }
      Text(this.text)
        .fontSize($r('app.float.font_11'))
        .fontWeight(FontWeight.Bold)
        .fontColor(this.variant === BadgeVariant.Up ? '#4E6C3A' : this.variant === BadgeVariant.Down ? '#A0353A' : '#4F5865');
    }
    .height(18)
    .padding({ left: 8, right: 8, top: 3, bottom: 3 })
    .borderRadius(999)
    .backgroundColor(
      this.variant === BadgeVariant.Up ? 'rgba(108,137,89,0.18)' :
      this.variant === BadgeVariant.Down ? 'rgba(201,66,71,0.16)' :
      'rgba(0,0,0,0.06)'
    );
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter BadgeTest
```

Expected: 4/4 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]Badge 胶囊指示器组件 up/down/neutral"
```

---

### Task 9: Chip 组件（primary/tinted/hidden 变体）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/Chip.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/ChipTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/ChipTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { ChipVariant, chipStyle } from '../../../main/ets/components/Chip';

export default function ChipTest() {
  describe('Chip', () => {
    it('default_has_border', 0, () => {
      const s = chipStyle(ChipVariant.Default);
      expect(s.borderWidth).assertEqual(1);
    });

    it('active_has_no_border', 0, () => {
      const s = chipStyle(ChipVariant.Active);
      expect(s.borderWidth).assertEqual(0);
    });

    it('hidden_uses_dashed_border', 0, () => {
      const s = chipStyle(ChipVariant.Hidden);
      expect(s.borderStyle).assertEqual(BorderStyle.Dashed);
    });

    it('all_pill_radius', 0, () => {
      expect(chipStyle(ChipVariant.Default).radius).assertEqual(999);
      expect(chipStyle(ChipVariant.Active).radius).assertEqual(999);
      expect(chipStyle(ChipVariant.Hidden).radius).assertEqual(999);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter ChipTest
```

Expected: FAIL

- [ ] **Step 3: 写 Chip.ets**

```typescript
// app-arkui/entry/src/main/ets/components/Chip.ets

export enum ChipVariant {
  Default = 'default',
  Active = 'active',
  TintedPrimary = 'tinted_primary',
  TintedCinnabar = 'tinted_cinnabar',
  TintedAmber = 'tinted_amber',
  Hidden = 'hidden'
}

export interface ChipStyle {
  backgroundColor: ResourceColor | string;
  textColor: ResourceColor | string;
  borderWidth: number;
  borderStyle: BorderStyle;
  borderColor: ResourceColor | string;
  radius: number;
  fontWeight: FontWeight;
}

export function chipStyle(variant: ChipVariant): ChipStyle {
  const base: Partial<ChipStyle> = { radius: 999, borderStyle: BorderStyle.Solid };
  switch (variant) {
    case ChipVariant.Default:
      return { ...base, backgroundColor: $r('app.color.surface'), textColor: $r('app.color.text_2'), borderWidth: 1, borderColor: $r('app.color.border'), fontWeight: FontWeight.Regular } as ChipStyle;
    case ChipVariant.Active:
      return { ...base, backgroundColor: $r('app.color.primary'), textColor: Color.White, borderWidth: 0, borderColor: Color.Transparent, fontWeight: FontWeight.Bold } as ChipStyle;
    case ChipVariant.TintedPrimary:
      return { ...base, backgroundColor: $r('app.color.primary_tint'), textColor: $r('app.color.primary_strong'), borderWidth: 1, borderColor: 'rgba(75,143,146,0.2)', fontWeight: FontWeight.Medium } as ChipStyle;
    case ChipVariant.TintedCinnabar:
      return { ...base, backgroundColor: $r('app.color.cinnabar_tint'), textColor: $r('app.color.yanzhi'), borderWidth: 1, borderColor: 'rgba(176,71,75,0.2)', fontWeight: FontWeight.Medium } as ChipStyle;
    case ChipVariant.TintedAmber:
      return { ...base, backgroundColor: $r('app.color.amber_tint'), textColor: '#7A5614', borderWidth: 1, borderColor: 'rgba(122,86,20,0.2)', fontWeight: FontWeight.Medium } as ChipStyle;
    case ChipVariant.Hidden:
      return { ...base, backgroundColor: $r('app.color.surface_2'), textColor: $r('app.color.text_3'), borderWidth: 1, borderStyle: BorderStyle.Dashed, borderColor: $r('app.color.border'), fontWeight: FontWeight.Regular } as ChipStyle;
  }
}

@Component
export struct Chip {
  @Prop label: string = '';
  @Prop variant: ChipVariant = ChipVariant.Default;
  @Prop showDot: boolean = false;

  build() {
    Row() {
      if (this.variant === ChipVariant.Active && this.showDot) {
        Text('●').fontSize(10).fontColor(Color.White).margin({ right: 4 });
      }
      Text(this.label)
        .fontSize($r('app.float.font_13'))
        .fontColor(chipStyle(this.variant).textColor)
        .fontWeight(chipStyle(this.variant).fontWeight);
    }
    .padding({ left: 12, right: 12, top: 6, bottom: 6 })
    .borderRadius(chipStyle(this.variant).radius)
    .backgroundColor(chipStyle(this.variant).backgroundColor)
    .border({ width: chipStyle(this.variant).borderWidth, style: chipStyle(this.variant).borderStyle, color: chipStyle(this.variant).borderColor });
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter ChipTest
```

Expected: 4/4 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]Chip 胶囊组件（含 tinted 和 hidden 变体）"
```

---

### Task 10: CbCard + HeroDarkCard 组件

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/CbCard.ets`
- Create: `app-arkui/entry/src/main/ets/components/HeroDarkCard.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/CardTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/CardTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { CbCardStyle, HeroDarkStyle } from '../../../main/ets/components/CbCard';

export default function CardTest() {
  describe('CbCard', () => {
    it('primary_radius_is_16', 0, () => {
      expect(CbCardStyle.radius).assertEqual(16);
    });

    it('primary_has_blur', 0, () => {
      expect(CbCardStyle.blurIntensity).assertEqual(4);
    });

    it('hero_uses_gradient', 0, () => {
      expect(HeroDarkStyle.gradientAngle).assertEqual(155);
    });

    it('hero_radius_larger', 0, () => {
      expect(HeroDarkStyle.radius).assertLarger(CbCardStyle.radius);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter CardTest
```

Expected: FAIL

- [ ] **Step 3: 写 CbCard.ets**

```typescript
// app-arkui/entry/src/main/ets/components/CbCard.ets

export const CbCardStyle = {
  radius: 16,
  paddingDefault: 12,
  borderWidth: 1,
  blurIntensity: 4,
  translucentLight: 0.82,
  translucentDark: 0.78
};

export const HeroDarkStyle = {
  radius: 20,
  padding: 16,
  gradientAngle: 155,
  gradientColorsLight: [['#2F6C70', 0], ['#1F4E52', 1]] as Array<[string, number]>,
  gradientColorsDark: [['#1F2940', 0], ['#0C1220', 1]] as Array<[string, number]>
};

@Component
export struct CbCard {
  @BuilderParam content: () => void;
  @Prop padding: number = 12;

  build() {
    Column() { this.content?.() }
      .padding(this.padding)
      .backgroundColor($r('app.color.surface_translucent'))
      .borderRadius(CbCardStyle.radius)
      .border({ width: CbCardStyle.borderWidth, color: $r('app.color.border') })
      .backgroundBlurStyle(BlurStyle.COMPONENT_THIN);
  }
}
```

- [ ] **Step 4: 写 HeroDarkCard.ets**

```typescript
// app-arkui/entry/src/main/ets/components/HeroDarkCard.ets
import { HeroDarkStyle } from './CbCard';

@Component
export struct HeroDarkCard {
  @BuilderParam content: () => void;

  build() {
    Stack() {
      // Gradient base
      Column()
        .width('100%').height('100%')
        .linearGradient({
          angle: HeroDarkStyle.gradientAngle,
          colors: HeroDarkStyle.gradientColorsLight
        });
      // Top-right highlight
      Column()
        .width('100%').height('100%')
        .radialGradient({
          center: ['92%', '-10%'],
          radius: '50%',
          colors: [['rgba(255,255,255,0.15)', 0], ['rgba(255,255,255,0)', 1]]
        });
      // Content
      Column() { this.content?.() }
        .padding(HeroDarkStyle.padding)
        .width('100%');
    }
    .width('100%')
    .borderRadius(HeroDarkStyle.radius)
    .clip(true)
    .shadow({ radius: 22, color: 'rgba(47,108,112,0.28)', offsetY: 8 });
  }
}
```

- [ ] **Step 5: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter CardTest
```

Expected: 4/4 PASS

- [ ] **Step 6: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]CbCard + HeroDarkCard 组件"
```

---

### Task 11: CbButton 组件（primary/secondary/danger/text/FAB）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/CbButton.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/ButtonTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/ButtonTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { ButtonVariant, buttonSpec } from '../../../main/ets/components/CbButton';

export default function ButtonTest() {
  describe('CbButton', () => {
    it('primary_height_is_40', 0, () => {
      expect(buttonSpec(ButtonVariant.Primary).height).assertEqual(40);
    });

    it('fab_radius_is_18', 0, () => {
      expect(buttonSpec(ButtonVariant.FAB).radius).assertEqual(18);
    });

    it('fab_size_is_56', 0, () => {
      expect(buttonSpec(ButtonVariant.FAB).height).assertEqual(56);
    });

    it('danger_bg_is_cinnabar', 0, () => {
      const spec = buttonSpec(ButtonVariant.Danger);
      expect(spec.backgroundColor).assertContain('cinnabar');
    });

    it('all_variants_meet_touch_min', 0, () => {
      for (const v of Object.values(ButtonVariant)) {
        expect(buttonSpec(v).height).assertLargerOrEqual(40);
      }
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter ButtonTest
```

Expected: FAIL

- [ ] **Step 3: 写 CbButton.ets**

```typescript
// app-arkui/entry/src/main/ets/components/CbButton.ets

export enum ButtonVariant {
  Primary = 'primary',
  Secondary = 'secondary',
  Danger = 'danger',
  TextLink = 'text',
  FAB = 'fab'
}

export interface ButtonSpec {
  height: number;
  radius: number;
  backgroundColor: string;
  textColor: string;
  fontWeight: FontWeight;
  shadow: boolean;
}

export function buttonSpec(variant: ButtonVariant): ButtonSpec {
  switch (variant) {
    case ButtonVariant.Primary:
      return { height: 40, radius: 10, backgroundColor: 'gradient:primary→primary_strong', textColor: '#fff', fontWeight: FontWeight.Bold, shadow: true };
    case ButtonVariant.Secondary:
      return { height: 40, radius: 10, backgroundColor: 'surface_2', textColor: 'text', fontWeight: FontWeight.Medium, shadow: false };
    case ButtonVariant.Danger:
      return { height: 40, radius: 10, backgroundColor: 'cinnabar', textColor: '#fff', fontWeight: FontWeight.Bold, shadow: true };
    case ButtonVariant.TextLink:
      return { height: 40, radius: 0, backgroundColor: 'transparent', textColor: 'primary_strong', fontWeight: FontWeight.Bold, shadow: false };
    case ButtonVariant.FAB:
      return { height: 56, radius: 18, backgroundColor: 'gradient:primary→primary_strong', textColor: '#fff', fontWeight: FontWeight.Regular, shadow: true };
  }
}

@Component
export struct CbButton {
  @Prop label: string = '';
  @Prop variant: ButtonVariant = ButtonVariant.Primary;
  @Prop onTap: () => void = () => {};

  build() {
    Button(this.label)
      .type(ButtonType.Normal)
      .height(buttonSpec(this.variant).height)
      .borderRadius(buttonSpec(this.variant).radius)
      .fontSize($r('app.float.font_14'))
      .fontWeight(buttonSpec(this.variant).fontWeight)
      .fontColor(
        this.variant === ButtonVariant.Primary || this.variant === ButtonVariant.Danger || this.variant === ButtonVariant.FAB ? Color.White :
        this.variant === ButtonVariant.TextLink ? $r('app.color.primary_strong') :
        $r('app.color.text')
      )
      .linearGradient(
        this.variant === ButtonVariant.Primary || this.variant === ButtonVariant.FAB ?
        { angle: 135, colors: [[$r('app.color.primary'), 0], [$r('app.color.primary_strong'), 1]] } :
        { angle: 0, colors: [[Color.Transparent, 0], [Color.Transparent, 1]] }
      )
      .backgroundColor(
        this.variant === ButtonVariant.Primary || this.variant === ButtonVariant.FAB ? Color.Transparent :
        this.variant === ButtonVariant.Secondary ? $r('app.color.surface_2') :
        this.variant === ButtonVariant.Danger ? $r('app.color.cinnabar') :
        Color.Transparent
      )
      .shadow(buttonSpec(this.variant).shadow ? { radius: 12, color: 'rgba(47,108,112,0.28)', offsetY: 4 } : undefined)
      .onClick(() => this.onTap?.());
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter ButtonTest
```

Expected: 5/5 PASS

- [ ] **Step 5: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]CbButton 按钮组件 5 变体"
```

---

### Task 12: DisplayText + 组件目录预览页 + README

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/DisplayText.ets`
- Create: `app-arkui/entry/src/main/ets/pages/ComponentCatalog.ets`
- Modify: `app-arkui/entry/src/main/resources/base/profile/main_pages.json`
- Create: `app-arkui/README.md`
- Create: `app-arkui/entry/src/ohosTest/ets/test/DisplayTextTest.ets`

- [ ] **Step 1: 写 DisplayText 失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/DisplayTextTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { DISPLAY_FONT_FAMILY, DISPLAY_TRACKING } from '../../../main/ets/components/DisplayText';

export default function DisplayTextTest() {
  describe('DisplayText', () => {
    it('font_family_is_serif_chain', 0, () => {
      expect(DISPLAY_FONT_FAMILY).assertContain('Source Han Serif');
    });

    it('tracking_is_negative', 0, () => {
      expect(DISPLAY_TRACKING).assertLess(0);
    });

    it('tracking_is_0_025em', 0, () => {
      expect(DISPLAY_TRACKING).assertEqual(-0.025);
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter DisplayTextTest
```

Expected: FAIL

- [ ] **Step 3: 写 DisplayText.ets**

```typescript
// app-arkui/entry/src/main/ets/components/DisplayText.ets

export const DISPLAY_FONT_FAMILY = 'Fraunces, Source Han Serif SC, Noto Serif SC, HarmonyOS Sans SC, serif';
export const DISPLAY_TRACKING = -0.025;

@Component
export struct DisplayText {
  @Prop text: string = '';
  @Prop size: number = 36;
  @Prop weight: FontWeight = FontWeight.Bold;
  @Prop color: ResourceColor = $r('app.color.text');

  build() {
    Text(this.text)
      .fontFamily(DISPLAY_FONT_FAMILY)
      .fontSize(this.size)
      .fontWeight(this.weight)
      .fontColor(this.color)
      .letterSpacing(this.size * DISPLAY_TRACKING);
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter DisplayTextTest
```

Expected: 3/3 PASS

- [ ] **Step 5: 写 ComponentCatalog.ets 组件预览页**

```typescript
// app-arkui/entry/src/main/ets/pages/ComponentCatalog.ets
import { GradientBackground } from '../components/GradientBackground';
import { CbCard } from '../components/CbCard';
import { HeroDarkCard } from '../components/HeroDarkCard';
import { CbButton, ButtonVariant } from '../components/CbButton';
import { Badge, BadgeVariant } from '../components/Badge';
import { Chip, ChipVariant } from '../components/Chip';
import { PulseDot } from '../components/PulseDot';
import { DisplayText } from '../components/DisplayText';

@Entry
@Component
struct ComponentCatalog {
  build() {
    Stack() {
      GradientBackground();
      Scroll() {
        Column({ space: 16 }) {
          // Header
          DisplayText({ text: 'Component Catalog', size: 24 })
            .margin({ top: 60, bottom: 16 });

          // Hero
          HeroDarkCard() {
            DisplayText({ text: '¥128,450.00', size: 28, color: '#F2EFE6' });
            Text('Hero Dark Card').fontColor('#D5E7E5').margin({ top: 8 });
          }

          // Card
          CbCard() {
            Text('Primary CbCard').fontSize(14);
          }

          // Buttons
          CbCard() {
            Column({ space: 8 }) {
              Row({ space: 8 }) {
                CbButton({ label: 'Primary', variant: ButtonVariant.Primary });
                CbButton({ label: 'Secondary', variant: ButtonVariant.Secondary });
              }
              Row({ space: 8 }) {
                CbButton({ label: 'Danger', variant: ButtonVariant.Danger });
                CbButton({ label: 'Text link', variant: ButtonVariant.TextLink });
              }
              CbButton({ label: '＋', variant: ButtonVariant.FAB });
            }
          }

          // Badges
          CbCard() {
            Row({ space: 6 }) {
              Badge({ variant: BadgeVariant.Up, text: '2.4%' });
              Badge({ variant: BadgeVariant.Down, text: '11.3%' });
              Badge({ variant: BadgeVariant.Neutral, text: '0%' });
            }
          }

          // Chips
          CbCard() {
            Flex({ wrap: FlexWrap.Wrap }) {
              Chip({ label: 'Default', variant: ChipVariant.Default });
              Chip({ label: 'Active', variant: ChipVariant.Active, showDot: true });
              Chip({ label: '#Tinted', variant: ChipVariant.TintedPrimary });
              Chip({ label: '#Hidden', variant: ChipVariant.Hidden });
            }
          }

          // PulseDot
          CbCard() {
            Row({ space: 10 }) {
              PulseDot({ color: $r('app.color.bamboo'), size: 10 });
              Text('已同步').fontSize(12);
            }
          }
        }
        .padding({ left: 16, right: 16, top: 0, bottom: 40 });
      }
    }
    .width('100%')
    .height('100%');
  }
}
```

- [ ] **Step 6: 注册页面**

```json
// app-arkui/entry/src/main/resources/base/profile/main_pages.json
{
  "src": ["pages/Index", "pages/ComponentCatalog"]
}
```

Modify `pages/Index.ets` 加一个跳转按钮：

```typescript
import router from '@ohos.router';

@Entry
@Component
struct Index {
  build() {
    Column({ space: 16 }) {
      Text('Cashbook v2').fontSize(24).fontWeight(FontWeight.Bold);
      Button('Component Catalog').onClick(() => router.pushUrl({ url: 'pages/ComponentCatalog' }));
    }
    .width('100%').height('100%').justifyContent(FlexAlign.Center);
  }
}
```

- [ ] **Step 7: 写 README**

```markdown
# Cashbook ArkUI-X (v2 Next-Gen UI)

Cashbook 下一代 UI 的 ArkUI-X 实现。目标：HarmonyOS NEXT（主）+ Android + iOS（次）一套代码。

## 开发环境

- DevEco Studio 5.0+
- HarmonyOS SDK API 12+
- Node.js 18+（用于脚本）
- Python 3.9+（用于资源迁移脚本）

## 目录结构

```
app-arkui/
├── entry/                     # 主模块
│   ├── src/main/
│   │   ├── ets/
│   │   │   ├── entryability/  # EntryAbility
│   │   │   ├── pages/         # Index, ComponentCatalog
│   │   │   ├── components/    # 原子组件
│   │   │   └── utils/
│   │   └── resources/
│   │       ├── base/element/  # color.json, float.json 等 Design Token
│   │       ├── base/media/    # vector_type_*.svg 图标
│   │       └── dark/element/  # Dark token override
│   └── src/ohosTest/          # 测试
├── oh-package.json5
└── build-profile.json5
```

## 常用命令

```bash
# 构建（默认）
hvigorw assembleHap --mode module -p product=default --no-daemon

# 清理
hvigorw clean --no-daemon

# 全部测试
hvigorw test --no-daemon

# 指定测试类
hvigorw test --no-daemon -- --testFilter ColorTokenTest

# 导入模拟器
hdc install -r entry/build/default/outputs/default/entry-default-signed.hap
```

## 测试

详见 [TESTING.md](./TESTING.md)

## 组件目录

启动后，Index 页面点击 "Component Catalog" 即可预览全部原子组件（GradientBackground / CbCard / HeroDarkCard / CbButton / Badge / Chip / PulseDot / DisplayText）的明暗两套效果。

## 资源迁移

Android 项目 `core/ui/src/main/res/drawable/vector_type_*_24.xml` 150+ 图标已通过 `scripts/avd-to-svg.py` 批量转为 SVG，位于 `entry/src/main/resources/base/media/`。直接通过 `$r('app.media.vector_type_dining_24')` 引用。

## 原 Android 项目

本 ArkUI-X 项目是 Cashbook v2。原 Android Kotlin 项目（仓库根目录）进入维护态，作为 v1 保留，不下线。

## 设计 Spec

`docs/superpowers/specs/2026-04-23-cashbook-harmonyos-ui-design.md`
```

- [ ] **Step 8: 构建并在模拟器运行**

```bash
cd app-arkui && hvigorw clean --no-daemon && hvigorw assembleHap --mode module --no-daemon
# 启动模拟器后
hdc install -r entry/build/default/outputs/default/entry-default-signed.hap
hdc shell aa start -a EntryAbility -b com.cashbook.v2
```

Expected: Index 页面显示，点击按钮跳到 ComponentCatalog，所有原子组件正确渲染。

- [ ] **Step 9: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]DisplayText + ComponentCatalog 预览页 + README"
```

---

### Task 13: Sparkline 组件（Canvas 折线）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/Sparkline.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/SparklineTest.ets`

- [ ] **Step 1: 写失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/SparklineTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { normalizeSparkData, sparkPath } from '../../../main/ets/components/Sparkline';

export default function SparklineTest() {
  describe('Sparkline', () => {
    it('normalize_maps_min_to_1_max_to_0', 0, () => {
      const data = [1, 5, 3];
      const n = normalizeSparkData(data, 100, 20);
      expect(n[0].y).assertEqual(20);  // min → y=height
      expect(n[1].y).assertEqual(0);   // max → y=0
    });

    it('normalize_spaces_x_evenly', 0, () => {
      const data = [1, 2, 3];
      const n = normalizeSparkData(data, 100, 20);
      expect(n[0].x).assertEqual(0);
      expect(n[1].x).assertEqual(50);
      expect(n[2].x).assertEqual(100);
    });

    it('single_point_stays_centered', 0, () => {
      const data = [5];
      const n = normalizeSparkData(data, 100, 20);
      expect(n.length).assertEqual(1);
      expect(n[0].y).assertEqual(10);  // center
    });

    it('path_starts_with_M', 0, () => {
      const data = [1, 2, 3];
      const n = normalizeSparkData(data, 100, 20);
      const d = sparkPath(n);
      expect(d.startsWith('M')).assertTrue();
    });
  });
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
hvigorw test --no-daemon -- --testFilter SparklineTest
```

Expected: FAIL

- [ ] **Step 3: 写 Sparkline.ets**

```typescript
// app-arkui/entry/src/main/ets/components/Sparkline.ets

export interface SparkPoint { x: number; y: number; }

export function normalizeSparkData(data: number[], width: number, height: number): SparkPoint[] {
  if (data.length === 0) return [];
  if (data.length === 1) return [{ x: width / 2, y: height / 2 }];

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const stepX = width / (data.length - 1);

  return data.map((v, i) => ({
    x: i * stepX,
    y: height - ((v - min) / range) * height
  }));
}

export function sparkPath(points: SparkPoint[]): string {
  if (points.length === 0) return '';
  const [first, ...rest] = points;
  return `M ${first.x} ${first.y} ` + rest.map(p => `L ${p.x} ${p.y}`).join(' ');
}

@Component
export struct Sparkline {
  @Prop data: number[] = [];
  @Prop width: number = 100;
  @Prop height: number = 14;
  @Prop strokeColor: ResourceColor = $r('app.color.primary');
  @Prop strokeWidth: number = 1.5;

  build() {
    Canvas(this.ctx)
      .width(this.width)
      .height(this.height)
      .onReady(() => this.draw());
  }

  private ctx = new CanvasRenderingContext2D(new RenderingContextSettings(true));

  private draw(): void {
    const points = normalizeSparkData(this.data, this.width, this.height);
    if (points.length < 2) return;

    this.ctx.strokeStyle = this.strokeColor as string;
    this.ctx.lineWidth = this.strokeWidth;
    this.ctx.beginPath();
    this.ctx.moveTo(points[0].x, points[0].y);
    for (let i = 1; i < points.length; i++) {
      this.ctx.lineTo(points[i].x, points[i].y);
    }
    this.ctx.stroke();
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
hvigorw test --no-daemon -- --testFilter SparklineTest
```

Expected: 4/4 PASS

- [ ] **Step 5: 把 Sparkline 加到 ComponentCatalog**

在 `ComponentCatalog.ets` 末尾的 Column 里加：

```typescript
// Sparkline
CbCard() {
  Column({ space: 6 }) {
    Text('Sparkline (data: [3, 5, 2, 8, 4, 6, 9])').fontSize(11);
    Sparkline({ data: [3, 5, 2, 8, 4, 6, 9], width: 280, height: 40, strokeColor: $r('app.color.primary') });
  }
}
```

加 import。

- [ ] **Step 6: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]Sparkline 组件（Canvas 折线图）"
```

---

### Task 14: RingProgress + SegmentTab + Toast（压轴）

**Files:**
- Create: `app-arkui/entry/src/main/ets/components/RingProgress.ets`
- Create: `app-arkui/entry/src/main/ets/components/SegmentTab.ets`
- Create: `app-arkui/entry/src/main/ets/utils/ToastHelper.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/RingProgressTest.ets`
- Create: `app-arkui/entry/src/ohosTest/ets/test/SegmentTabTest.ets`

- [ ] **Step 1: 写 RingProgress 失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/RingProgressTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { computeRingArc } from '../../../main/ets/components/RingProgress';

export default function RingProgressTest() {
  describe('RingProgress', () => {
    it('0_progress_empty_arc', 0, () => {
      expect(computeRingArc(0)).assertEqual(0);
    });

    it('100_progress_full_arc', 0, () => {
      const full = computeRingArc(1.0);
      expect(full).assertLargerOrEqual(2 * Math.PI - 0.01);
    });

    it('50_progress_half_arc', 0, () => {
      const half = computeRingArc(0.5);
      expect(half).assertLargerOrEqual(Math.PI - 0.01);
      expect(half).assertLessOrEqual(Math.PI + 0.01);
    });

    it('clamp_over_1_to_full', 0, () => {
      expect(computeRingArc(1.5)).assertEqual(computeRingArc(1.0));
    });
  });
}
```

- [ ] **Step 2: 写 SegmentTab 失败测试**

```typescript
// app-arkui/entry/src/ohosTest/ets/test/SegmentTabTest.ets
import { describe, it, expect } from '@ohos/hypium';
import { SegmentTabStyle } from '../../../main/ets/components/SegmentTab';

export default function SegmentTabTest() {
  describe('SegmentTab', () => {
    it('container_radius_is_10', 0, () => {
      expect(SegmentTabStyle.containerRadius).assertEqual(10);
    });
    it('item_radius_is_7', 0, () => {
      expect(SegmentTabStyle.itemRadius).assertEqual(7);
    });
    it('active_background_is_surface', 0, () => {
      expect(SegmentTabStyle.activeBackground).assertContain('surface');
    });
  });
}
```

- [ ] **Step 3: 运行所有测试验证失败**

```bash
hvigorw test --no-daemon
```

Expected: 2 个测试类 FAIL

- [ ] **Step 4: 写 RingProgress.ets**

```typescript
// app-arkui/entry/src/main/ets/components/RingProgress.ets

const TWO_PI = 2 * Math.PI;

export function computeRingArc(progress: number): number {
  const clamped = Math.max(0, Math.min(1, progress));
  return clamped * TWO_PI;
}

@Component
export struct RingProgress {
  @Prop size: number = 44;
  @Prop strokeWidth: number = 5;
  @Prop progress: number = 0; // 0.0 ~ 1.0
  @Prop trackColor: ResourceColor = 'rgba(0,0,0,0.1)';
  @Prop progressColor: ResourceColor = $r('app.color.primary');

  build() {
    Canvas(this.ctx)
      .width(this.size)
      .height(this.size)
      .onReady(() => this.draw());
  }

  private ctx = new CanvasRenderingContext2D(new RenderingContextSettings(true));

  private draw(): void {
    const cx = this.size / 2;
    const cy = this.size / 2;
    const r = (this.size - this.strokeWidth) / 2;

    // Track
    this.ctx.beginPath();
    this.ctx.strokeStyle = this.trackColor as string;
    this.ctx.lineWidth = this.strokeWidth;
    this.ctx.arc(cx, cy, r, 0, TWO_PI);
    this.ctx.stroke();

    // Progress (start at -PI/2 = top)
    const arcLen = computeRingArc(this.progress);
    if (arcLen > 0) {
      this.ctx.beginPath();
      this.ctx.strokeStyle = this.progressColor as string;
      this.ctx.lineWidth = this.strokeWidth;
      this.ctx.lineCap = 'round';
      this.ctx.arc(cx, cy, r, -Math.PI / 2, -Math.PI / 2 + arcLen);
      this.ctx.stroke();
    }
  }
}
```

- [ ] **Step 5: 写 SegmentTab.ets**

```typescript
// app-arkui/entry/src/main/ets/components/SegmentTab.ets

export const SegmentTabStyle = {
  containerRadius: 10,
  itemRadius: 7,
  containerPadding: 3,
  itemPadding: 6,
  activeBackground: '$r:app.color.surface',
  height: 46
};

@Component
export struct SegmentTab {
  @Prop items: string[] = [];
  @Link activeIndex: number;

  build() {
    Row() {
      ForEach(this.items, (item: string, idx: number) => {
        Text(item)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
          .fontSize($r('app.float.font_12'))
          .fontWeight(this.activeIndex === idx ? FontWeight.Bold : FontWeight.Medium)
          .fontColor(this.activeIndex === idx ? $r('app.color.primary_strong') : $r('app.color.text_2'))
          .opacity(this.activeIndex === idx ? 1 : 0.55)
          .padding(SegmentTabStyle.itemPadding)
          .borderRadius(SegmentTabStyle.itemRadius)
          .backgroundColor(this.activeIndex === idx ? $r('app.color.surface') : Color.Transparent)
          .onClick(() => { this.activeIndex = idx; });
      });
    }
    .padding(SegmentTabStyle.containerPadding)
    .borderRadius(SegmentTabStyle.containerRadius)
    .backgroundColor($r('app.color.surface_2'))
    .width('100%');
  }
}
```

- [ ] **Step 6: 写 ToastHelper.ets**

```typescript
// app-arkui/entry/src/main/ets/utils/ToastHelper.ets
import promptAction from '@ohos.promptAction';

export enum ToastKind { Success = 'success', Error = 'error', Info = 'info' }

export function showToast(message: string, kind: ToastKind = ToastKind.Info, duration: number = 2500): void {
  // HMOS NEXT 原生 Toast 暂不支持自定义样式，先用 showToast，M5 阶段改自定义浮层组件
  promptAction.showToast({
    message: (kind === ToastKind.Success ? '✓ ' : kind === ToastKind.Error ? '✕ ' : '') + message,
    duration,
    bottom: '80%'  // top-ish
  });
}
```

- [ ] **Step 7: 把新组件加到 ComponentCatalog.ets**

```typescript
// 加 import
import { RingProgress } from '../components/RingProgress';
import { SegmentTab } from '../components/SegmentTab';
import { showToast, ToastKind } from '../utils/ToastHelper';

// 在 Scroll 里加 state
@State catalogTabIndex: number = 0;

// 在 Column 里加
// Ring
CbCard() {
  Row({ space: 16 }) {
    RingProgress({ size: 60, progress: 0.25, progressColor: $r('app.color.primary') });
    RingProgress({ size: 60, progress: 0.73, progressColor: $r('app.color.amber') });
    RingProgress({ size: 60, progress: 1.0, progressColor: $r('app.color.bamboo') });
  }
}

// SegmentTab
CbCard() {
  Column({ space: 8 }) {
    SegmentTab({ items: ['支出', '收入', '转账'], activeIndex: $catalogTabIndex });
    Text(`Active: ${this.catalogTabIndex}`).fontSize(11);
  }
}

// Toast trigger
CbCard() {
  Row({ space: 8 }) {
    CbButton({ label: 'Success', variant: ButtonVariant.Primary, onTap: () => showToast('保存成功', ToastKind.Success) });
    CbButton({ label: 'Error', variant: ButtonVariant.Danger, onTap: () => showToast('失败了', ToastKind.Error) });
  }
}
```

- [ ] **Step 8: 运行所有测试**

```bash
hvigorw test --no-daemon
```

Expected: 全部 PASS（9 个测试类，约 28 个测试）

- [ ] **Step 9: 构建并运行 · 手动验证 Catalog**

```bash
cd app-arkui && hvigorw clean --no-daemon && hvigorw assembleHap --mode module --no-daemon
hdc install -r entry/build/default/outputs/default/entry-default-signed.hap
hdc shell aa start -a EntryAbility -b com.cashbook.v2
```

Expected: 进入 App → Index 页 → 点击 "Component Catalog" → 所有组件（GradientBackground + CbCard + HeroDarkCard + Buttons + Badges + Chips + PulseDot + DisplayText + Sparkline + Ring × 3 + SegmentTab + Toast 按钮）正确渲染，SegmentTab 可切换，Toast 按钮可弹出。

- [ ] **Step 10: Commit**

```bash
git add app-arkui/
git commit -m "[feat|arkui|foundation][公共]RingProgress + SegmentTab + ToastHelper 组件"
```

---

## M1 验收 Checklist

完成全部 14 个 Task 后：

- [ ] `hvigorw clean && hvigorw assembleHap --mode module --no-daemon` 成功
- [ ] `hvigorw test --no-daemon` 全部 PASS（约 9 test suites / 28+ cases）
- [ ] ComponentCatalog 页面手动检查：
  - [ ] GradientBackground 4 层渐变可见（青瓷/秋香/松绿光晕）
  - [ ] HeroDarkCard 深青瓷渐变 + 右上角高光
  - [ ] CbCard 半透 + BlurStyle.COMPONENT_THIN
  - [ ] 5 种 Button 正确显示
  - [ ] 3 种 Badge（up bamboo / down cinnabar / neutral gray）
  - [ ] 6 种 Chip 变体
  - [ ] PulseDot 脉冲动画流畅
  - [ ] DisplayText Fraunces serif（降级到 Source Han Serif / Noto Serif）
  - [ ] Sparkline 折线正确
  - [ ] RingProgress 3 个进度环（25% / 73% / 100%）
  - [ ] SegmentTab 可切换 Tab
  - [ ] Toast Success/Error 按钮可触发
- [ ] `app-arkui/entry/src/main/resources/base/media/` 包含 150+ vector_type_*.svg
- [ ] README + TESTING.md 完整
- [ ] 所有 commit 信息遵循 `[type|module|feature][scope]description` 格式

---

## M2-M6 Roadmap（本 plan 不展开）

完成 M1 后，M2-M6 各自独立 brainstorm → spec → plan → implementation 循环：

| Milestone | 交付物 | 关键 Task 提示 |
|---|---|---|
| **M2 · MVP 3 屏** | 首页 + 记一笔 + 账本流水 (可记账可看)  | Navigation 壳 · BottomTab · 首页 Bento 拼装 · 记一笔（两级分类 + 计算键盘 · CalculatorUtils 迁移）· 账本流水（Sticky Header + Swipe action）· Room → relationalStore DAO 基础 3 个表（TypeTable / RecordTable / AssetTable） |
| **M3 · 统计 + 资产 + 管理** | 统计页 + 资产列表 + 资产详情 + 6 管理页 | Donut/Line/Bar chart 组件 · Asset Hero 比例条 · 账本/分类/标签/设置管理页 |
| **M4 · 数据管理** | 备份恢复 + 账单导入 + 设置/关于 | WebDAV client（OkHttp → @ohos.net.http）· CSV 解析 · Schema v10 兼容验证 · Markdown renderer |
| **M5 · 抛光** | 空/错/加载状态 + a11y + 动效 + 弹窗完整化 | Shimmer · EmptyState · ErrorState · 10 种弹窗完整实现 · 动效 tokens · Reduced Motion 适配 · Dynamic Type · accessibilityText 补齐 |
| **M6 · 测试 + 跨端** | 截图测试 + 单元测试覆盖 + HMOS/Android/iOS 验证 | DevEco Testing 截图基线 · 业务逻辑覆盖 70%+ · Android APK 生成验证 · iOS IPA 生成验证 · 跨端差异记录 |
