#!/usr/bin/env bash
# 截图相对比对（spec docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md §3）：
# base_dir 与 head_dir 各含若干 <module>/src/test/screenshots/**/*.png，
# 逐字节比对，changed/added/removed 三类清单写 GITHUB_STEP_SUMMARY（缺省 stdout），
# changed 帧新旧对拷贝到 out_dir（__base.png/__head.png 后缀）。恒 exit 0（C' 永不 fail）。
# 用法: screenshot_diff.sh <base_dir> <head_dir> <out_dir>
set -euo pipefail

BASE_DIR="$1"
HEAD_DIR="$2"
OUT_DIR="$3"
mkdir -p "$OUT_DIR"
WORK="$(mktemp -d)"

list_shots() { (cd "$1" && find . -path '*/src/test/screenshots/*' -name '*.png' | LC_ALL=C sort); }

list_shots "$BASE_DIR" > "$WORK/base.list"
list_shots "$HEAD_DIR" > "$WORK/head.list"

comm -23 "$WORK/base.list" "$WORK/head.list" > "$WORK/removed.list"
comm -13 "$WORK/base.list" "$WORK/head.list" > "$WORK/added.list"
comm -12 "$WORK/base.list" "$WORK/head.list" > "$WORK/common.list"

: > "$WORK/changed.list"
while IFS= read -r f; do
  if ! cmp -s "$BASE_DIR/$f" "$HEAD_DIR/$f"; then
    echo "$f" >> "$WORK/changed.list"
    d="$OUT_DIR/$(dirname "$f")"
    mkdir -p "$d"
    b="$(basename "$f" .png)"
    cp "$BASE_DIR/$f" "$d/${b}__base.png"
    cp "$HEAD_DIR/$f" "$d/${b}__head.png"
  fi
done < "$WORK/common.list"

SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/stdout}"
{
  echo "## Screenshot visual diff (merge-base vs PR HEAD) — informational"
  echo ""
  echo "| kind | count |"
  echo "|---|---|"
  echo "| changed | $(wc -l < "$WORK/changed.list") |"
  echo "| added | $(wc -l < "$WORK/added.list") |"
  echo "| removed | $(wc -l < "$WORK/removed.list") |"
  for kind in changed added removed; do
    n="$(wc -l < "$WORK/$kind.list")"
    if [ "$n" -gt 0 ]; then
      echo ""
      echo "<details><summary>$kind ($n)</summary>"
      echo ""
      sed 's/^\.\///; s/^/- /' "$WORK/$kind.list" | head -300
      if [ "$n" -gt 300 ]; then echo "- ...(truncated, $n total)"; fi
      echo ""
      echo "</details>"
    fi
  done
} >> "$SUMMARY"

# 给后续 step 的信号：是否有 changed（供 artifact 上传 if 判断）
echo "changed_count=$(wc -l < "$WORK/changed.list")" >> "${GITHUB_OUTPUT:-/dev/null}"
exit 0
