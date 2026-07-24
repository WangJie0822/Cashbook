#!/usr/bin/env bash
# 截图基线新鲜度检查（spec docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md §4）：
# 对每个截图模块，若 PR 改了 <module>/src/main/**（*.kt 或 res/**）
# 但同 PR 未动 <module>/src/test/screenshots/**，判为疑似基线未重录。
# 用法: screenshot_freshness.sh <merge_base_sha> <head_sha>
# exit 0 = 干净；exit 1 = 有 MISS（调用方 job 据此 fail）。
set -euo pipefail

BASE="$1"
HEAD="$2"

MODULES=(feature/tags feature/records feature/assets feature/books feature/types feature/settings feature/budget core/design core/ui)

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
