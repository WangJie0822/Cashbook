#!/usr/bin/env bash
# 截图基线新鲜度检查（spec docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md §4）：
# 对每个截图模块，若 PR 改了 <module>/src/main/**（*.kt 或 res/**）
# 但同 PR 未动 <module>/src/test/screenshots/**，判为疑似基线未重录。
# 用法: screenshot_freshness.sh <merge_base_sha> <head_sha>
# exit 0 = 干净；exit 1 = 有 MISS（调用方 job 据此 fail）。
# 边界（有意设计，勿误读为覆盖缺口修复点）：
# - 只按「模块自身」src/main 判定，不覆盖跨模块传递渲染影响（改 core/design 影响
#   feature/* 渲染的场景由非阻塞 screenshot_diff 报告兜底）；
# - 触发面 = *.kt 与 res/**（AndroidManifest/assets 变更不触发，按 spec §4 取舍）。
set -euo pipefail

BASE="$1"
HEAD="$2"

# 硬编码而非按已入库 PNG 动态派生：动态派生会漏「有截图测试代码但 0 张已入库基线」
# 的模块（其 shot_cnt 恒 0 应判 MISS）。下方自检断言防清单与实际漂移。
MODULES=(feature/tags feature/records feature/assets feature/books feature/types feature/settings feature/budget core/design core/ui)

# 清单漂移自检：所有「已有入库基线」的模块必须都在 MODULES 中（多出的 0 基线模块允许）
DERIVED="$(git ls-files '*/src/test/screenshots/*.png' | sed 's|/src/test/screenshots/.*||' | sort -u)"
for d in $DERIVED; do
  found=0
  for m in "${MODULES[@]}"; do [ "$d" = "$m" ] && { found=1; break; }; done
  if [ "$found" -eq 0 ]; then
    echo "ERROR: module '$d' has committed screenshot baselines but is missing from MODULES in $0 — add it."
    exit 1
  fi
done

CHANGED="$(git diff --name-only "$BASE" "$HEAD")"

fail=0
for m in "${MODULES[@]}"; do
  src_hit="$(printf '%s\n' "$CHANGED" | grep -E "^$m/src/main/(.*\.kt$|res/)" | head -3 || true)"
  shot_cnt="$(printf '%s\n' "$CHANGED" | grep -c "^$m/src/test/screenshots/" || true)"
  if [ -n "$src_hit" ] && [ "$shot_cnt" -eq 0 ]; then
    gradle_path=":$(echo "$m" | tr '/' ':')"
    echo "MISS $m — UI source changed but no baseline update. Re-record locally: ./gradlew ${gradle_path}:recordRoborazziDebug (or add PR label 'screenshot-freshness-skip' if render-neutral)"
    printf '%s\n' "$src_hit" | sed 's/^/    changed: /'
    fail=1
  fi
done

if [ "$fail" -eq 0 ]; then
  echo "OK: all screenshot modules fresh (or untouched)."
fi
exit "$fail"
