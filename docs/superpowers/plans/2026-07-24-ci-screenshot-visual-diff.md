# CI 截图护栏（C' 视觉 diff 报告 + L 新鲜度 lint）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给 CI 加两层截图护栏——L（阻塞）：改 UI 源未重录同模块基线即红；C'（非阻塞）：同 runner 双 record 输出「本 PR 改了哪些帧」报告；并删除 Build.yaml 空转截图链。

**Architecture:** 新增独立 workflow `.github/workflows/Screenshot.yaml`（两个 read-only job：`screenshot_freshness` 阻塞 lint、`screenshot_diff` 非阻塞报告），逻辑落在两个可本地测试的 bash 脚本；C' 上线前必须通过 Phase 0 gating（ubuntu record×2 逐字节一致，临时 workflow 实测，不通过则 C' 作废只留 L）。

**Tech Stack:** GitHub Actions（复用现有 checkout/setup-java/setup-gradle/upload-artifact）、bash + git + cmp、Gradle `recordRoborazziDebug`。

**Spec:** `docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md`

## Global Constraints

- 触发一律 `pull_request`，**禁止 `pull_request_target`**（spec §6.1）。
- 新增 job 一律 `permissions: contents: read`（spec §6.2）。
- SHA 一律经 `env:` 注入（`github.event.pull_request.base.sha`/`head.sha`），**禁止 `${{ }}` 直接插值 `run:`**（spec §6.3）。
- 不新增第三方 action；不改 `ScreenshotHelper.kt`、`changeThreshold`、任何入库基线 PNG（spec §2）。
- `.sh` 脚本必须 **LF 行尾**（CRLF 在 ubuntu bash 下报 `\r: command not found`；写完用 `file`/`cat -A` 核验，必要时 `sed -i 's/\r$//'`）。
- workflow 文件变更 PR 需 gh CLI `workflow` scope：实施前 `gh auth refresh -h github.com -s workflow`（项目 CLAUDE.md 契约）。
- 本地在 worktree 内实施（EnterWorktree 或 `git worktree add`），分支名 `worktree-ci-screenshot-guardrail`。
- 9 个截图模块清单（L 与 C' 共用，两脚本内各自维护同一份数组）：`feature/tags feature/records feature/assets feature/books feature/types feature/settings feature/budget core/design core/ui`。

---

### Task 1: L 新鲜度归类脚本 `screenshot_freshness.sh`（本地真实历史 commit 验证）

**Files:**
- Create: `.github/scripts/screenshot_freshness.sh`

**Interfaces:**
- Produces: `bash .github/scripts/screenshot_freshness.sh <merge_base_sha> <head_sha>`——命中「改 UI 未重录」时打印 `MISS <module> ...` 行 + exit 1；全干净 exit 0。Task 3 的 `screenshot_freshness` job 调用它。

- [ ] **Step 1: 写脚本**

```bash
#!/usr/bin/env bash
# 截图基线新鲜度检查（spec §4）：
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
```

- [ ] **Step 2: 本地用真实历史 commit 对验证（三个用例，Git Bash）**

用例 ①（纯基线 commit → 干净）：
```bash
cd <worktree> && bash .github/scripts/screenshot_freshness.sh 51e76163^ 51e76163; echo "exit=$?"
```
Expected: `OK: ...`，`exit=0`（该 commit 只改 531 张 PNG，无 src/main 改动）。

用例 ②（改 UI 源 + 无基线 → MISS）：`20a0e502`（BUG-1 修复，改 `feature/records`/`feature/types` 的 .kt、无截图变更）：
```bash
bash .github/scripts/screenshot_freshness.sh 20a0e502^ 20a0e502; echo "exit=$?"
```
Expected: 输出含 `MISS feature/records`（及/或 feature/types），`exit=1`。

用例 ③（UI 源 + 基线同 commit → 干净）：`f1a7d634`（周期弹窗，records .kt + 截图同笔）：
```bash
bash .github/scripts/screenshot_freshness.sh f1a7d634^ f1a7d634; echo "exit=$?"
```
Expected: 对 feature/records 无 MISS（若该 commit 未动其他模块 UI 则整体 `exit=0`；若历史事实与预期不符，以 `git show --name-only` 核对后换等价 commit，勿硬凑）。

- [ ] **Step 3: LF 核验 + commit**

```bash
cat -A .github/scripts/screenshot_freshness.sh | grep -c '\r' # 期望 0；非 0 则 sed -i 's/\r$//'
git add .github/scripts/screenshot_freshness.sh
git commit -m "[ci|截图verify][公共]L基线新鲜度lint脚本：改src/main未动同模块screenshots判MISS，真实历史commit三用例验证"
```

---

### Task 2: C' 比对脚本 `screenshot_diff.sh`（本地 fixture 验证）

**Files:**
- Create: `.github/scripts/screenshot_diff.sh`

**Interfaces:**
- Produces: `bash .github/scripts/screenshot_diff.sh <base_dir> <head_dir> <out_dir>`——两目录内 `*/src/test/screenshots/**/*.png` 逐字节比对；三类归集（changed/added/removed）写 `$GITHUB_STEP_SUMMARY`（无该 env 时 stdout）；changed 的新旧对拷 `<out_dir>`；**恒 exit 0**（C' 永不 fail，spec §3.1）。Task 5 的 `screenshot_diff` job 调用它。

- [ ] **Step 1: 写脚本**

```bash
#!/usr/bin/env bash
# 截图相对比对（spec §3）：base_dir 与 head_dir 各含若干 <module>/src/test/screenshots/**/*.png
# 输出 changed/added/removed 三类清单到 GITHUB_STEP_SUMMARY（缺省 stdout），
# changed 帧新旧对拷贝到 out_dir（__base.png/__head.png 后缀）。恒 exit 0。
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
      [ "$n" -gt 300 ] && echo "- ...(truncated, $n total)"
      echo ""
      echo "</details>"
    fi
  done
} >> "$SUMMARY"

# 给后续 step 的信号：是否有 changed（供 artifact 上传 if 判断）
echo "changed_count=$(wc -l < "$WORK/changed.list")" >> "${GITHUB_OUTPUT:-/dev/null}"
exit 0
```

- [ ] **Step 2: 本地 fixture 验证（Git Bash）**

```bash
T=$(mktemp -d)
mkdir -p "$T"/{a,b}/feature/tags/src/test/screenshots "$T/out"
printf 'AAA' > "$T/a/feature/tags/src/test/screenshots/same.png"
printf 'AAA' > "$T/b/feature/tags/src/test/screenshots/same.png"
printf 'AAA' > "$T/a/feature/tags/src/test/screenshots/chg.png"
printf 'BBB' > "$T/b/feature/tags/src/test/screenshots/chg.png"
printf 'X'   > "$T/a/feature/tags/src/test/screenshots/gone.png"
printf 'Y'   > "$T/b/feature/tags/src/test/screenshots/new.png"
bash .github/scripts/screenshot_diff.sh "$T/a" "$T/b" "$T/out"; echo "exit=$?"
ls -R "$T/out"
```
Expected: 表格 `changed 1 / added 1 / removed 1`；清单含 `chg.png`/`new.png`/`gone.png`；`$T/out/.../chg__base.png` 与 `chg__head.png` 存在；`exit=0`。再跑一次 a vs a：三类全 0、out 目录为空、exit=0。

- [ ] **Step 3: LF 核验 + commit**

```bash
cat -A .github/scripts/screenshot_diff.sh | grep -c '\r' # 期望 0
git add .github/scripts/screenshot_diff.sh
git commit -m "[ci|截图verify][公共]C'相对比对脚本：changed/added/removed三类归集+summary+changed对拷出，fixture验证恒exit0"
```

---

### Task 3: workflow 落地第一批——Screenshot.yaml（先只含 L job）+ Phase 0 临时 workflow + Build.yaml 删空转链，开 PR

**Files:**
- Create: `.github/workflows/Screenshot.yaml`
- Create: `.github/workflows/Phase0ScreenshotDeterminism.yaml`
- Modify: `.github/workflows/Build.yaml:78-103`（删除四个空转 step）

**Interfaces:**
- Consumes: Task 1 脚本。
- Produces: PR（分支 `worktree-ci-screenshot-guardrail`）——Task 4 在其上做实证；`Screenshot.yaml` 的 job 骨架——Task 5 往里加 `screenshot_diff` job。

- [ ] **Step 1: 写 `.github/workflows/Screenshot.yaml`**

```yaml
name: Screenshot

on:
  pull_request:
    # labeled/unlabeled：打/摘 screenshot-freshness-skip 后无需空 push 即可重判
    types: [opened, synchronize, reopened, labeled, unlabeled]

concurrency:
  group: screenshot-${{ github.ref }}
  cancel-in-progress: true

jobs:
  screenshot_freshness:
    name: "Screenshot baseline freshness (blocking, label-skippable)"
    if: ${{ !contains(github.event.pull_request.labels.*.name, 'screenshot-freshness-skip') }}
    runs-on: ubuntu-latest
    permissions:
      contents: read
    timeout-minutes: 10
    steps:
      - name: Checkout
        uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Check screenshot baseline freshness
        env:
          BASE_SHA: ${{ github.event.pull_request.base.sha }}
          HEAD_SHA: ${{ github.event.pull_request.head.sha }}
        run: |
          MB="$(git merge-base "$BASE_SHA" "$HEAD_SHA")"
          echo "merge-base: $MB"
          bash .github/scripts/screenshot_freshness.sh "$MB" "$HEAD_SHA" | tee -a "$GITHUB_STEP_SUMMARY"
```

注意：`run:` 内只用 env 变量（`$BASE_SHA`/`$HEAD_SHA`），无 `${{ }}` 插值（Global Constraints）。`tee -a` 管道会吃掉脚本 exit code——bash 默认 `pipefail` 未开，**须确认 step 失败语义**：Actions 的 `run:` 默认 shell 是 `bash -e {0}`，管道末端 tee 成功则 step 绿。改写为不吞 exit code 的形式（实施时用这个最终版）：

```yaml
        run: |
          MB="$(git merge-base "$BASE_SHA" "$HEAD_SHA")"
          echo "merge-base: $MB"
          set -o pipefail
          bash .github/scripts/screenshot_freshness.sh "$MB" "$HEAD_SHA" | tee -a "$GITHUB_STEP_SUMMARY"
```

- [ ] **Step 2: 写 `.github/workflows/Phase0ScreenshotDeterminism.yaml`（临时，Task 6 删）**

```yaml
# TEMPORARY (spec §3.0 Phase 0 gating): ubuntu record×2 byte-identity + duration.
# Delete this file once the gating verdict is recorded (plan Task 6).
name: Phase0ScreenshotDeterminism

on:
  pull_request:
    paths:
      - '.github/workflows/Phase0ScreenshotDeterminism.yaml'

concurrency:
  group: phase0-${{ github.ref }}
  cancel-in-progress: true

jobs:
  determinism:
    name: "ubuntu recordRoborazziDebug x2 byte identity"
    runs-on: ubuntu-latest
    permissions:
      contents: read
    timeout-minutes: 150
    steps:
      - name: Checkout
        uses: actions/checkout@v7

      - name: Copy CI gradle.properties
        run: mkdir -p ~/.gradle ; cp .github/ci-gradle.properties ~/.gradle/gradle.properties

      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: 'zulu'
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          validate-wrappers: true
          gradle-home-cache-cleanup: true

      - name: Record round 1
        run: |
          t0=$(date +%s)
          ./gradlew recordRoborazziDebug --rerun-tasks
          t1=$(date +%s)
          echo "RECORD1_SECONDS=$((t1-t0))" >> "$GITHUB_ENV"
          mkdir -p "$RUNNER_TEMP/shots-1"
          tar cf - $(git ls-files '*/src/test/screenshots/*.png' | sed 's|/src/test/screenshots/.*||' | sort -u | sed 's|$|/src/test/screenshots|') | (cd "$RUNNER_TEMP/shots-1" && tar xf -)
          git checkout -- .
          git clean -fd -- '*/src/test/screenshots/'

      - name: Record round 2
        run: |
          t0=$(date +%s)
          ./gradlew recordRoborazziDebug --rerun-tasks
          t1=$(date +%s)
          echo "RECORD2_SECONDS=$((t1-t0))" >> "$GITHUB_ENV"

      - name: Compare round1 vs round2 (byte identity)
        run: |
          bash .github/scripts/screenshot_diff.sh "$RUNNER_TEMP/shots-1" . "$RUNNER_TEMP/diff-out"
          echo "record1=${RECORD1_SECONDS}s record2=${RECORD2_SECONDS}s" | tee -a "$GITHUB_STEP_SUMMARY"
          # gating 判据：changed 必须为 0（added/removed 也应为 0，同一代码两录）
          CHANGED=$(find "$RUNNER_TEMP/diff-out" -name '*__base.png' | wc -l)
          echo "changed_pairs=$CHANGED" | tee -a "$GITHUB_STEP_SUMMARY"
          if [ "$CHANGED" -ne 0 ]; then
            echo "::error::ubuntu record x2 NOT byte-identical — C' gating FAILED (spec §3.0)"
            exit 1
          fi

      - name: Upload non-identical pairs (gating failure evidence)
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: phase0-nonidentical
          path: ${{ runner.temp }}/diff-out
          retention-days: 14
```

- [ ] **Step 3: Build.yaml 删空转链**

删除 `Build.yaml:78-103` 四个 step（`Run all local screenshot tests (Roborazzi)` / `Prevent pushing new screenshots if this is a fork` / `Generate new screenshots if verification failed and it's a PR` / `Push new screenshots if available`），保留 `:105-108` `Run local tests`（其上「Run local tests after screenshot tests to avoid wrong UP-TO-DATE」注释一并删除——前置截图 step 已不存在）。`test_and_apk` 其余 step 与 `permissions: contents: write`（Dependency Guard 仍需）不动。

- [ ] **Step 4: workflow 语法自检 + commit**

```bash
py -c "import yaml,glob; [yaml.safe_load(open(f,encoding='utf-8')) for f in glob.glob('.github/workflows/*.yaml')]; print('YAML OK')"
git add .github/workflows/Screenshot.yaml .github/workflows/Phase0ScreenshotDeterminism.yaml .github/workflows/Build.yaml
git commit -m "[ci|截图verify][公共]Screenshot.yaml新增L新鲜度job(阻塞可label豁免)+Phase0临时gating workflow+Build.yaml删四步空转截图链(消*/*.png宽口径auto-commit面)"
```

（若本机无 PyYAML：`py -m pip install pyyaml` 或改用 `gh workflow view` 在 push 后核验。）

- [ ] **Step 5: push 分支 + 开 PR（前置 workflow scope）**

```bash
gh auth status | grep -q workflow || gh auth refresh -h github.com -s workflow
git push -u origin worktree-ci-screenshot-guardrail
gh pr create --title "[ci] CI 截图护栏：L 基线新鲜度 lint + C' 视觉 diff 报告（Phase 0 gating）" --body "spec: docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md

- L screenshot_freshness（阻塞，label screenshot-freshness-skip 豁免）
- Phase 0 gating：ubuntu record×2 字节一致性 + 时长实测（临时 workflow，判读后删除）
- 删 Build.yaml 空转截图链
- C' screenshot_diff job 待 Phase 0 通过后追加

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

Expected: PR 创建成功；该 PR 触发 `Screenshot`（freshness job，本 PR 无 UI 改动应绿）+ `Phase0ScreenshotDeterminism`（paths 命中）+ `Build`。

---

### Task 4: PR 上的实证循环（L 变异验证 + Phase 0 判读）

**Files:**
- Modify（临时变异，验证后 revert）: `feature/tags/src/main/kotlin/cn/wj/android/cashbook/feature/tags/screen/MyTagsScreen.kt`

**Interfaces:**
- Consumes: Task 3 的 PR 与两条 workflow run。
- Produces: Phase 0 判读结论（一致 → Task 5；不一致 → 跳到 Task 6 并按「C' 作废」收尾）+ `RECORD1_SECONDS` 实测时长（决定 Task 5 的 `timeout-minutes`）。

- [ ] **Step 1: 等待并核验首轮 run**

```bash
gh pr checks --watch
gh run list --branch worktree-ci-screenshot-guardrail --limit 5
```
Expected: `Screenshot / screenshot_freshness` 绿（本 PR 只动 .github/docs）；`Phase0ScreenshotDeterminism / determinism` 完成（绿=gating 通过；红且 error 为 NOT byte-identical=gating 失败）。判定只看 run 日志实际输出，不猜。

- [ ] **Step 2: L 变异验证——红**

对 `MyTagsScreen.kt` 做一处渲染性微改（如某 `padding(top = 8.dp)` → `9.dp`），**不动任何基线**，commit + push：

```bash
git commit -am "[test|截图verify][公共]TEMP L变异验证:MyTagsScreen padding 8->9dp 不带基线(预期freshness红,下一commit revert)"
git push
gh pr checks --watch
```
Expected: `screenshot_freshness` **FAIL**，日志含 `MISS feature/tags`。

- [ ] **Step 3: L 变异验证——绿 + label 豁免**

```bash
git revert --no-edit HEAD && git push
gh pr checks --watch   # 期望 freshness 恢复绿
gh pr edit --add-label screenshot-freshness-skip
gh run list --workflow Screenshot --branch worktree-ci-screenshot-guardrail --limit 2   # labeled 触发新 run，freshness job 应 skipped
gh pr edit --remove-label screenshot-freshness-skip
```
Expected: revert 后绿；打 label 触发的 run 中 freshness job 显示 skipped；摘 label 后恢复正常。（label 不存在时先 `gh label create screenshot-freshness-skip --description "跳过截图基线新鲜度检查(渲染无关改动)" --color EDEDED`。）

- [ ] **Step 4: Phase 0 判读并记录**

从 run summary 抄录 `changed_pairs` 与 `record1/record2` 秒数，写入 spec §8（追加「Phase 0 实测结果」小节，含 run URL）。判读：
- `changed_pairs=0` → **gating 通过**，取 `RECORD1_SECONDS` 估算 Task 5 timeout（`2×record + 编译 + 余量`，如单次 20min → timeout 60min）→ 进 Task 5。
- 非 0 → **C' 作废**：跳过 Task 5，Task 6 中同时删 `Phase0ScreenshotDeterminism.yaml`、在 spec §3.0 记录否决证据（artifact 中不一致样本）、CLAUDE.md 同步只提 L。

```bash
git add docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md
git commit -m "[docs|spec|截图verify][公共]Phase0 gating实测结果回填(ubuntu record×2一致性+时长+run URL)"
```

---

### Task 5: C' `screenshot_diff` job 上线（仅 Phase 0 通过时）

**Files:**
- Modify: `.github/workflows/Screenshot.yaml`（追加 job）

**Interfaces:**
- Consumes: Task 2 脚本、Task 4 的时长实测值。
- Produces: PR 上可见的 screenshot diff summary + artifact。

- [ ] **Step 1: Screenshot.yaml 追加 job**

```yaml
  screenshot_diff:
    name: "Screenshot visual diff (informational, never blocks)"
    runs-on: ubuntu-latest
    permissions:
      contents: read
    timeout-minutes: 90   # ← 按 Task 4 实测调整：约 2×单次record + 2×编译 + 10min 余量
    steps:
      - name: Checkout
        uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Copy CI gradle.properties
        run: mkdir -p ~/.gradle ; cp .github/ci-gradle.properties ~/.gradle/gradle.properties

      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: 'zulu'
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          validate-wrappers: true
          gradle-home-cache-cleanup: true

      - name: Record at merge-base
        env:
          BASE_SHA: ${{ github.event.pull_request.base.sha }}
          HEAD_SHA: ${{ github.event.pull_request.head.sha }}
        run: |
          MB="$(git merge-base "$BASE_SHA" "$HEAD_SHA")"
          echo "merge-base: $MB"
          git checkout --detach "$MB"
          ./gradlew recordRoborazziDebug --rerun-tasks
          mkdir -p "$RUNNER_TEMP/base-shots"
          tar cf - $(git ls-files '*/src/test/screenshots/*.png' | sed 's|/src/test/screenshots/.*||' | sort -u | sed 's|$|/src/test/screenshots|') | (cd "$RUNNER_TEMP/base-shots" && tar xf -)
          git checkout -- .
          git clean -fd -- '*/src/test/screenshots/'

      - name: Record at PR HEAD
        env:
          HEAD_SHA: ${{ github.event.pull_request.head.sha }}
        run: |
          git checkout --detach "$HEAD_SHA"
          ./gradlew recordRoborazziDebug --rerun-tasks

      - name: Compare and summarize
        id: diff
        run: bash .github/scripts/screenshot_diff.sh "$RUNNER_TEMP/base-shots" . "$RUNNER_TEMP/diff-out"

      - name: Upload changed pairs
        if: steps.diff.outputs.changed_count != '0'
        uses: actions/upload-artifact@v7
        with:
          name: screenshot-diff
          path: ${{ runner.temp }}/diff-out
          retention-days: 14
```

实现要点（与 spec §3.1 对齐）：`run:` 内零 `${{ }}` 未受信插值（仅 `runner.temp`/`steps.*.outputs` 等系统值出现在非 run 字段）；merge-base record 的产物按「模块/src/test/screenshots」相对结构拷 `$RUNNER_TEMP/base-shots`（tar 保结构）；还原用 `git checkout -- .`（record 改写的是 git 跟踪文件）+ `git clean -fd -- '*/src/test/screenshots/'`（清 merge-base 侧新增帧残留，限定子树、不动 build/ 以复用编译产物）；head 侧产物即工作区，脚本 find 限定 screenshots 子树天然排除其他 png。

- [ ] **Step 2: push 并在本 PR 实测两态**

```bash
git add .github/workflows/Screenshot.yaml
git commit -m "[ci|截图verify][公共]C'视觉diff报告job上线：同runner双record相对比对,changed对上传artifact,永不fail"
git push
gh pr checks --watch
```
Expected（本 PR 无 UI 改动）: `screenshot_diff` 绿，summary 三类全 0，无 artifact。

再推一个临时 UI 变异 commit（同 Task 4 Step 2 的 padding 改法，这次**验 C' 报告**）：
```bash
git commit -am "[test|截图verify][公共]TEMP C'验证:MyTagsScreen padding变异(预期diff报告出feature/tags changed,下一commit revert)"
git push
gh pr checks --watch
```
Expected: `screenshot_diff` 仍绿（永不 fail）；summary `changed ≥ 4`（MyTags 相关帧）且清单集中 `feature/tags/`；artifact `screenshot-diff` 含 `__base/__head` 对；`screenshot_freshness` 红（无基线——顺带二次确认 L）。下载 artifact 抽 1 对目检确认像素确实不同：
```bash
gh run download <run-id> -n screenshot-diff -D /tmp/sd && ls -R /tmp/sd | head
```
然后 revert：
```bash
git revert --no-edit HEAD && git push
```

---

### Task 6: 收尾——删临时 workflow + CLAUDE.md 同步 + 合并

**Files:**
- Delete: `.github/workflows/Phase0ScreenshotDeterminism.yaml`
- Modify: `CLAUDE.md`（「Roborazzi 截图基线由本地 record 维护」条款）
- Modify: `docs/superpowers/specs/2026-07-24-ci-screenshot-visual-diff-design.md`（勘误/实测回填，若 Task 4 未完成）

**Interfaces:**
- Consumes: Task 4 判读结论、Task 5 实测结果。

- [ ] **Step 1: 删临时 workflow**

```bash
git rm .github/workflows/Phase0ScreenshotDeterminism.yaml
git commit -m "[ci|截图verify][公共]删Phase0临时gating workflow(判读已回填spec)"
```

- [ ] **Step 2: CLAUDE.md 条款同步**

在「Roborazzi 截图基线由本地 record 维护，CI 当前不 verify/不 record feature/core 截图」条款中，将「**但截图 verify 仍未上 CI**，待独立 spec 加固后引入」段更新为实际落地态（按 Phase 0 结果二选一，保持一行内）：

- 通过版：`CI 截图护栏已上线（2026-07-24 spec）：L screenshot_freshness 阻塞 lint（改 <module>/src/main/** 未动同模块 src/test/screenshots/** 即红，PR label screenshot-freshness-skip 豁免）+ C' screenshot_diff 非阻塞视觉 diff 报告（同 runner 对 merge-base/PR HEAD 双 record 字节比对，changed/added/removed 进 summary+artifact，永不 fail；dependabot PR 的大面积 changed 是依赖漂移定性材料非噪声）。CI 仍不做基线 verify——本机 record 即权威 reference 不变。`
- 作废版：同上仅保留 L 描述 + `C' 相对比对因 Phase 0 gating 失败（ubuntu record×2 不一致，证据见 spec §3.0）作废`。

```bash
git add CLAUDE.md
git commit -m "[docs|截图verify][公共]CLAUDE.md同步CI截图护栏落地态(L lint+C'报告/或C'作废判读)"
```

- [ ] **Step 3: 全量核验 + 合并**

```bash
gh pr checks   # 全绿（Build / Screenshot 两 workflow；androidTest 照旧）
gh pr view --json labels -q '.labels[].name'   # 确认无残留 screenshot-freshness-skip
gh pr merge --merge --admin
```
Expected: merge 成功。合并后 `gh run list --branch main --limit 3` 确认 push:main 触发的 Build 绿（Screenshot.yaml 是 pull_request-only，main 上不跑，符合设计）。

- [ ] **Step 4: 本地收尾**

```bash
git -C D:/Work/Workspace/Owner/Cashbook checkout main
git -C D:/Work/Workspace/Owner/Cashbook pull --ff-only
# worktree 清理（撞 MAX_PATH 用 \\?\ 前缀删物理目录）
git -C D:/Work/Workspace/Owner/Cashbook worktree remove --force <worktree路径> || powershell -NoProfile -Command 'Remove-Item -LiteralPath "\\?\<worktree绝对路径>" -Recurse -Force'
git -C D:/Work/Workspace/Owner/Cashbook worktree prune
git -C D:/Work/Workspace/Owner/Cashbook branch -D worktree-ci-screenshot-guardrail
```

之后按流程走 superpowers:finishing-a-development-branch 收束（节点 2 评审在合并前：本 plan 的 diff 以 workflow+脚本为主，若合并前 `git diff` 逻辑面 <50 行可两维快审，否则跑满；由 controller 在 Task 6 Step 3 合并前触发）。

---

## 风险与回退

- **Phase 0 失败**：C' 作废，L 独立存活（Task 4 Step 4 已定分支），无残骸（Phase 0 workflow 本就计划删除）。
- **C' 时长超预算**：spec §9 备选——`screenshot_diff` job 加 `if: contains(github.event.pull_request.labels.*.name, 'screenshot-diff')` 改按需触发（label 驱动），一行改动。
- **L 误报率高**（纯逻辑 .kt 改动频繁命中）：先用 label 豁免顶住；若持续，收窄触发面到 `src/main/kotlin/**/(screen|view|component|dialog)/**` 类 UI 包（需另行核对 9 模块包结构后再定，本期不做）。
- **整体回退**：`git revert` 三个 workflow/脚本 commit 即可，无基线/无共享代码污染（spec §7 对照：这正是 C'+L 相对 A/B 的回退优势）。
