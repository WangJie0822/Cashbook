#!/usr/bin/env python3
# Copyright 2026 The Cashbook Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""在同一提交的独占 worktree 上逐项变异；原工作区被测文件全程只读。

先运行每组真实绿测，再要求目标用例以断言失败杀死合法变异。
编译错误、跳过、陈旧 XML、超时与缺失报告均不计作 KILLED。
每轮从字节基线恢复，子进程有硬超时，结束时核对两棵树的源码哈希。
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass


SQL = "core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterSqlBuilder.kt"
VM = "feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchViewModel.kt"
SCREEN = "feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreen.kt"
ROW = "feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterRecordItem.kt"
MODEL = "core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordFilterSummary.kt"
ROOM_CLASS = "cn.wj.android.cashbook.core.data.repository.impl.RecordFilterRepositoryRoomTest"
ROOM_TEST = "core/data/src/androidTest/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterRepositoryRoomTest.kt"


@dataclass(frozen=True)
class Edit:
    path: str
    old: str
    new: str


@dataclass(frozen=True)
class Mutation:
    name: str
    group: str
    expected: str
    edits: tuple[Edit, ...]


PUBLISH = "        _filterState.value = candidate.copy(mode = mode, queryContext = context, recordQueryFailure = if (changed || retry) null else candidate.recordQueryFailure)"
SAVE = "        SearchFilterStateCodec.write(savedStateHandle, _filterState.value)"
START = "        if (changed || retry) startQuery(context)"
SUMMARY_LINE = '"SELECT COUNT(*) AS totalCount, COALESCE(SUM($income), 0) AS income, COALESCE(SUM($expenditure), 0) AS expenditure $fromWhere"'
LIMITED_SUMMARY = SUMMARY_LINE.replace("$fromWhere", '${fromWhere.replace("FROM db_record r", "FROM (SELECT * FROM db_record ORDER BY record_time DESC, id DESC LIMIT 20) r")}')
MUTATIONS = (
    Mutation("book_and_to_or", "room", "combinedTypesCategoriesAndTags_doNotBypassBookOrOtherDimensions", (
        Edit(SQL, 'predicates.joinToString(" AND ")', 'predicates.joinToString(" OR ")'),)),
    Mutation("tag_outer_join_duplicates", "room", "tagsAny_deduplicatesAndScopesBook", (
        Edit(SQL, "FROM db_record r LEFT JOIN db_type t ON t.id = r.type_id WHERE ", "FROM db_record r LEFT JOIN db_type t ON t.id = r.type_id LEFT JOIN db_tag_with_record duplicate_tags ON duplicate_tags.record_id = r.id WHERE "),)),
    Mutation("date_end_inclusive", "room", "dateRange_includesStartAndExcludesEnd", (
        Edit(SQL, '"r.record_time < ?"', '"r.record_time <= ?"'),)),
    Mutation("dynamic_children_disconnected", "room", "wholeGroup_newChildAndMovedChild_followCurrentTree", (
        Edit(SQL, "r.type_id = p.id OR t.parent_id = p.id", "r.type_id = p.id"),)),
    Mutation("exclusion_reversed", "room", "wholeGroup_newChildAndMovedChild_followCurrentTree", (
        Edit(SQL, "r.type_id NOT IN", "r.type_id IN"),)),
    Mutation("summary_limited_to_first_page", "room", "aggregate_coversAllPagesAndSameTimestampIsStable", (
        Edit(SQL, SUMMARY_LINE, LIMITED_SUMMARY),)),
    Mutation("sql_transfer_counts_principal", "room", "transfer_onlyFeeMinusConcessionsContributes", (
        Edit(SQL, "r.charge - r.concessions", "r.amount"),)),
    Mutation("keyword_uses_filter_source", "state", "keyword_ignoresPreviouslyAppliedFilter", (
        Edit(VM, "context.mode == SearchMode.Keyword -> recordDataVersion.flatMapLatest {", "context.mode == SearchMode.Keyword -> if (filterState.value.appliedFilter != null) getFilteredRecordViewsUseCase(filterState.value.appliedFilter!!.toRequest(context.bookId, filterState.value.monthStartDay)) else recordDataVersion.flatMapLatest {"),)),
    Mutation("catalogue_subscription_once", "state", "independentTypeVersion_refreshesTreeAndQuery", (
        Edit(VM, "import kotlinx.coroutines.flow.flowOf", "import kotlinx.coroutines.flow.flowOf\nimport kotlinx.coroutines.flow.take"),
        Edit(VM, "typeRepository.allTypeListData,", "typeRepository.allTypeListData.take(1),"))),
    Mutation("retry_epoch_guard_deleted", "state", "retry_oldFailureCannotReplaceNewSuccessForSameCriteria", (
        Edit(VM, "epoch == attempt && context == filterState.value.queryContext", "context == filterState.value.queryContext"),)),
    Mutation("query_started_before_context_publication", "state", "resetThenApply_unrestrictedIsExplicit", (
        Edit(VM, "\n".join((PUBLISH, SAVE, START)), "\n".join((START, PUBLISH, SAVE))),)),
    Mutation("annotation_rollback_to_original_row", "ui", "transferAndRelatedIncome_keepMainAmountsAndExplainContributions", (
        Edit(ROW, "if (shouldShowFilterContribution(record))", "if (false)"),)),
    Mutation("model_rollback_to_legacy_transfer_amount", "model", "transfer_excludesPrincipalAndShowsFeeContribution", (
        Edit(MODEL, "RecordFilterContribution(0L, record.charges - record.concessions)", "RecordFilterContribution(0L, record.amount + record.charges - record.concessions)"),)),
    Mutation("sql_expenditure_rollback_to_gross", "room", "outsideRangeReimbursement_updatesHistoricNet", (
        Edit(SQL, "THEN r.final_amount WHEN t.type_category", "THEN r.amount + r.charge - r.concessions WHEN t.type_category"),)),
    Mutation("sql_income_rollback_to_gross", "room", "outsideRangeReimbursement_updatesHistoricNet", (
        Edit(SQL, "THEN r.final_amount ELSE 0 END", "THEN r.amount - r.charge ELSE 0 END"),)),
    Mutation("income_annotation_disconnected", "ui", "transferAndRelatedIncome_keepMainAmountsAndExplainContributions", (
        Edit(ROW, "if (shouldShowFilterContribution(record))", "if (shouldShowFilterContribution(record) && record.typeCategory != RecordTypeCategoryEnum.INCOME)"),)),
    Mutation("keyword_filter_button_reenabled", "ui", "keyword_disablesFilterEntryAndExplainsHowToRestoreIt", (
        Edit(SCREEN, "enabled = filterState.metadataReady && !keywordMode", "enabled = filterState.metadataReady"),)),
    Mutation("keyword_filter_hint_removed", "ui", "keyword_disablesFilterEntryAndExplainsHowToRestoreIt", (
        Edit(SCREEN, "if (keywordMode) stateDescription = clearKeywordHint", "if (false) stateDescription = clearKeywordHint"),)),
    Mutation("keyword_drawer_guard_removed", "state", "keyword_blocksDrawerButDraftAndClearKeepFilteringAvailable", (
        Edit(VM, "state.metadataReady && state.mode != SearchMode.Keyword", "state.metadataReady"),)),
    Mutation("restored_scroll_uses_revision_counter", "ui", "newViewModel_delayedBookRestoresAppliedCriteriaAndScroll", (
        Edit(SCREEN, "filterState.queryContext.copy(criteriaRevision = 0L).toString()", "filterState.queryContext.toString()"),)),
)

GROUPS = {
    "room": (":core:data:connectedDebugAndroidTest", [], "core/data/build/outputs/androidTest-results/connected/debug/TEST-*.xml"),
    "state": (":feature:records:testDebugUnitTest", ["--tests", "*SearchFilterStateTest"], "feature/records/build/test-results/testDebugUnitTest/TEST-*SearchFilterStateTest.xml"),
    "ui": (":feature:records:testDebugUnitTest", ["--tests", "*SearchFilterScreenTest"], "feature/records/build/test-results/testDebugUnitTest/TEST-*SearchFilterScreenTest.xml"),
    "model": (":core:model:test", ["--tests", "*RecordFilterSummaryTest"], "core/model/build/test-results/test/TEST-*RecordFilterSummaryTest.xml"),
}


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(["git", "-C", str(root), *args], timeout=30, text=True, encoding="utf-8").strip()


def read_reports(paths: list[Path], since_ns: int) -> dict[str, dict]:
    if not paths:
        raise ValueError("No JUnit XML reports")
    cases: dict[str, dict] = {}
    for path in paths:
        if path.stat().st_mtime_ns < since_ns:
            raise ValueError(f"Stale JUnit XML: {path.name}")
        root = ET.parse(path).getroot()
        for suite in root.iter("testsuite"):
            nodes = suite.findall("testcase")
            if not nodes:
                continue
            counts = {"failure": 0, "error": 0, "skipped": 0}
            for case in nodes:
                key = f"{case.attrib.get('classname', '')}.{case.attrib['name']}"
                kind = next((tag for tag in counts if case.find(tag) is not None), "passed")
                if kind != "passed":
                    counts[kind] += 1
                detail = case.find(kind) if kind != "passed" else None
                value = {"name": case.attrib["name"], "kind": kind,
                         "type": detail.attrib.get("type", "") if detail is not None else "",
                         "message": detail.attrib.get("message", "") if detail is not None else "",
                         "trace": (detail.text or "")[:12000] if detail is not None else ""}
                if key in cases:
                    raise ValueError(f"Duplicate JUnit testcase: {key}")
                cases[key] = value
            expected = {"tests": len(nodes), "failures": counts["failure"], "errors": counts["error"], "skipped": counts["skipped"]}
            for attribute, actual in expected.items():
                if int(suite.attrib.get(attribute, 0)) != actual:
                    raise ValueError(f"JUnit declared count mismatch: {path.name}: {attribute}")
    if not cases:
        raise ValueError("JUnit XML contains zero cases")
    return cases


def assertion_failure(case: dict, root: Path | None = None) -> bool:
    text = "\n".join((case["type"], case["message"], case["trace"]))
    if case["kind"] != "failure":
        return False
    if any(token in text for token in ("AssertionError", "ComparisonFailure")):
        case["assertion_evidence"] = "explicit_exception_type"
        return True
    # Android UTP 的 Truth 失败原文不含异常类名。限定为实测的 expected/but was
    # 形态，并回读第一帧对应的真实 assertThat 语句；不能将任意 failure 当成断言。
    if root is None or case["type"]:
        return False
    payload, separator, frames = case["trace"].partition("\nat ")
    if not separator or not re.search(r"(?m)^expected\s*:[^\n]+\nbut was\s*:[^\n]+", payload):
        return False
    first_frame = frames.splitlines()[0]
    site = re.fullmatch(re.escape(ROOM_CLASS) + r"\$" + re.escape(case["name"]) + r"\$\d+\.invokeSuspend\(RecordFilterRepositoryRoomTest.kt:(\d+)\)", first_frame)
    if site is None:
        return False
    lines = (root / ROOM_TEST).read_text(encoding="utf-8").splitlines()
    line_number = int(site.group(1))
    if not 1 <= line_number <= len(lines):
        return False
    statement = lines[line_number - 1].strip()
    if not statement.startswith("assertThat(") or not re.search(r"\.(?:containsExactly|isEqualTo)\(", statement):
        return False
    case["assertion_evidence"] = {"kind": "android_truth_source_verified", "path": ROOM_TEST, "line": line_number, "statement": statement}
    return True


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="cashbook-mutation-parser-") as directory:
        path = Path(directory) / "TEST-fixture.xml"
        path.write_text('<testsuite tests="2" failures="1" errors="0" skipped="0"><testcase classname="C" name="pass"/><testcase classname="C" name="guard"><failure type="java.lang.AssertionError" message="expected 1, got 2">AssertionError</failure></testcase></testsuite>', encoding="utf-8")
        cases = read_reports([path], 0)
        if len(cases) != 2 or not assertion_failure(cases["C.guard"]) or assertion_failure(cases["C.pass"]):
            raise ValueError("Parser positive/negative control failed")
        path.write_text(path.read_text(encoding="utf-8").replace('failures="1"', 'failures="0"'), encoding="utf-8")
        try:
            read_reports([path], 0)
        except ValueError:
            pass
        else:
            raise ValueError("Parser accepted a contradictory report header")
        try:
            read_reports([], 0)
        except ValueError:
            pass
        else:
            raise ValueError("Parser accepted missing reports")
        room_source = Path(directory) / ROOM_TEST
        room_source.parent.mkdir(parents=True)
        room_source.write_text('assertThat(actual).containsExactly(2L, 1L).inOrder()\n', encoding="utf-8")
        native = {"name": "guard", "kind": "failure", "type": "", "message": "",
                  "trace": f"unexpected (1): 3\n---\nexpected : [2, 1]\nbut was : [3, 2, 1]\nat {ROOM_CLASS}$guard$1.invokeSuspend(RecordFilterRepositoryRoomTest.kt:1)\n"}
        if not assertion_failure(native, Path(directory)):
            raise ValueError("Actual Android Truth shape not recognized")
        for changed in ({"type": "java.lang.IllegalStateException"}, {"trace": "TimeoutCancellationException"}, {"name": "otherGuard"}):
            if assertion_failure(native | changed, Path(directory)):
                raise ValueError("Android parser accepted unrelated failure")
        room_source.write_text('throw IllegalStateException("expected : fake")\n', encoding="utf-8")
        if assertion_failure(native, Path(directory)):
            raise ValueError("Android parser accepted a non-assertion source site")


def command_for(root: Path, group: str, device: str | None) -> list[str]:
    task, filters, _ = GROUPS[group]
    wrapper = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    command = [wrapper, task, "--rerun", *filters, "--no-daemon", "--console=plain", "-Proborazzi.test.verify=false"]
    if group == "room":
        if not device:
            raise ValueError("Room mutation requires an explicit --device")
        command += [f"-Pandroid.testInstrumentationRunnerArguments.class={ROOM_CLASS}"]
    return command


def run_tests(root: Path, group: str, log: Path, device: str | None, timeout: int) -> dict:
    command = command_for(root, group, device)
    env = os.environ.copy()
    if device:
        env["ANDROID_SERIAL"] = device
    started_ns = time.time_ns()
    print(f"RUN {log.stem} log={log}", flush=True)
    with log.open("wb") as output:
        process = subprocess.Popen(command, cwd=root, env=env, stdout=output, stderr=subprocess.STDOUT, start_new_session=os.name != "nt")
        try:
            code = process.wait(timeout=timeout)
        except BaseException:
            if process.poll() is None:
                if os.name == "nt":
                    subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], timeout=30, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
                else:
                    os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=30)
            raise
    contents = log.read_text(encoding="utf-8", errors="replace")
    task = GROUPS[group][0]
    status = re.search(r"^> Task " + re.escape(task) + r"(?: ([^\r\n]+))?$", contents, re.MULTILINE)
    if status is None or status.group(1) in {"UP-TO-DATE", "FROM-CACHE", "NO-SOURCE", "SKIPPED"}:
        raise ValueError(f"Target test task did not execute: {task}")
    if not re.search(r"^BUILD (SUCCESSFUL|FAILED)\b", contents, re.MULTILINE):
        raise ValueError("Missing terminal BUILD result")
    reports = sorted(root.glob(GROUPS[group][2]))
    cases = read_reports(reports, started_ns)
    saved = log.with_suffix("") / "reports"
    saved.mkdir(parents=True)
    for path in reports:
        shutil.copyfile(path, saved / path.name)
    return {"returncode": code, "command": command, "reports": [str(path.relative_to(root)) for path in reports],
            "saved_reports": [str((saved / path.name).relative_to(log.parent)) for path in reports], "cases": cases}


def write_json(path: Path, data: dict) -> None:
    pending = path.with_suffix(".tmp")
    pending.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    pending.replace(path)


def execute(args: argparse.Namespace) -> int:
    source = args.source.resolve(strict=True)
    worktree = args.worktree.resolve(strict=True)
    if source == worktree or source.is_relative_to(worktree) or worktree.is_relative_to(source):
        raise ValueError("Source and mutation worktree must be independent sibling checkouts")
    for root in (source, worktree):
        if Path(git(root, "rev-parse", "--show-toplevel")).resolve() != root:
            raise ValueError(f"Not an exact Git root: {root}")
    head = git(source, "rev-parse", "HEAD")
    if git(worktree, "rev-parse", "HEAD") != head:
        raise ValueError("Mutation worktree must start at the source HEAD")
    if git(worktree, "status", "--porcelain"):
        raise ValueError("Mutation worktree contains local changes; refusing to overwrite")
    selected = [mutation for mutation in MUTATIONS if not args.only or mutation.name in args.only]
    groups = list(dict.fromkeys(mutation.group for mutation in selected))
    paths = sorted({edit.path for mutation in MUTATIONS for edit in mutation.edits})
    originals = {path: (source / path).read_bytes() for path in paths}
    if any((worktree / path).read_bytes() != data for path, data in originals.items()):
        raise ValueError("Source bytes differ from mutation worktree")
    for mutation in selected:
        for edit in mutation.edits:
            text = originals[edit.path].decode("utf-8").replace("\r\n", "\n")
            if text.count(edit.old) != 1:
                raise ValueError(f"Mutation anchor is not unique: {mutation.name}: {edit.path}")
    output = args.output.resolve()
    if (output / "results.json").exists():
        raise ValueError("Output already contains a run; choose a fresh output directory")
    output.mkdir(parents=True, exist_ok=True)
    for path, data in originals.items():
        backup = output / "baseline-source" / path
        backup.parent.mkdir(parents=True, exist_ok=True)
        backup.write_bytes(data)
    result = {"source_head": head, "selected_mutations": [mutation.name for mutation in selected],
              "source_hashes_before": {path: sha(data) for path, data in originals.items()},
              "baselines": {}, "restored_baselines": {}, "mutations": [], "source_untouched": False, "mutation_tree_restored": False}
    try:
        for group in groups:
            baseline = run_tests(worktree, group, output / f"baseline-{group}.log", args.device, args.timeout)
            if baseline["returncode"] != 0 or any(case["kind"] != "passed" for case in baseline["cases"].values()):
                raise ValueError(f"Baseline not fully green: {group}")
            for mutation in selected:
                if mutation.group == group and not any(case["name"] == mutation.expected or case["name"].startswith(mutation.expected + "[") for case in baseline["cases"].values()):
                    raise ValueError(f"Baseline lacks the expected guard: {mutation.expected}")
            result["baselines"][group] = baseline
            write_json(output / "results.json", result)
            print(f"BASELINE {group} PASS tests={len(baseline['cases'])}", flush=True)
        for mutation in selected:
            for path, data in originals.items():
                (worktree / path).write_bytes(data)
            for edit in mutation.edits:
                path = worktree / edit.path
                data = path.read_bytes()
                text = data.decode("utf-8").replace("\r\n", "\n")
                if text.count(edit.old) != 1:
                    raise ValueError(f"Mutation anchor changed unexpectedly: {mutation.name}")
                text = text.replace(edit.old, edit.new, 1)
                if b"\r\n" in data:
                    text = text.replace("\n", "\r\n")
                path.write_bytes(text.encode("utf-8"))
            run = run_tests(worktree, mutation.group, output / f"{mutation.name}.log", args.device, args.timeout)
            if set(run["cases"]) != set(result["baselines"][mutation.group]["cases"]):
                raise ValueError(f"Executed case set changed: {mutation.name}")
            failed = {key: case for key, case in run["cases"].items() if case["kind"] != "passed"}
            if (run["returncode"] == 0) != (not failed):
                raise ValueError(f"Exit status contradicts JUnit results: {mutation.name}")
            expected = [key for key, case in failed.items() if (case["name"] == mutation.expected or case["name"].startswith(mutation.expected + "[")) and assertion_failure(case, worktree)]
            verdict = "KILLED" if run["returncode"] != 0 and expected else "SURVIVED" if not failed else "ERROR"
            result["mutations"].append({"id": mutation.name, "group": mutation.group, "expected_guard": mutation.expected,
                                        "verdict": verdict, "failed_cases": failed, "matched_assertion_guards": expected, **{key: value for key, value in run.items() if key != "cases"}})
            write_json(output / "results.json", result)
            print(f"RESULT {mutation.name} {verdict} guards={expected} failed={list(failed)}", flush=True)
            if verdict != "KILLED":
                raise ValueError(f"Mutation not killed by its intended assertion: {mutation.name}")
        for path, data in originals.items():
            (worktree / path).write_bytes(data)
        for group in groups:
            restored = run_tests(worktree, group, output / f"restored-{group}.log", args.device, args.timeout)
            if restored["returncode"] != 0 or any(case["kind"] != "passed" for case in restored["cases"].values()):
                raise ValueError(f"Restored baseline failed: {group}")
            if set(restored["cases"]) != set(result["baselines"][group]["cases"]):
                raise ValueError(f"Restored case set changed: {group}")
            result["restored_baselines"][group] = restored
            write_json(output / "results.json", result)
        result["status"] = "PASS"
    except BaseException as error:
        result["status"] = "ERROR"
        result["error"] = f"{type(error).__name__}: {error}"
        raise
    finally:
        for path, data in originals.items():
            (worktree / path).write_bytes(data)
        result["source_hashes_after"] = {path: sha((source / path).read_bytes()) for path in paths}
        result["source_untouched"] = result["source_hashes_before"] == result["source_hashes_after"]
        result["mutation_tree_restored"] = all((worktree / path).read_bytes() == data for path, data in originals.items())
        write_json(output / "results.json", result)
        print(f"SOURCE_UNTOUCHED={result['source_untouched']} MUTATION_TREE_RESTORED={result['mutation_tree_restored']}", flush=True)
    if not result["source_untouched"] or not result["mutation_tree_restored"]:
        raise ValueError("Source hash verification failed")
    print(f"PASS {len(result['mutations'])}/{len(selected)}; {output / 'results.json'}", flush=True)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path)
    parser.add_argument("--worktree", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--device")
    parser.add_argument("--timeout", type=int, default=600)
    parser.add_argument("--only", action="append", choices=[mutation.name for mutation in MUTATIONS])
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    self_test()
    if args.self_test:
        print("Parser controls PASS: explicit/Android Truth assertion, passing case, count mismatch, missing reports, unrelated failure, wrong guard and non-assertion site")
        return 0
    if args.list:
        print(json.dumps([asdict(mutation) for mutation in MUTATIONS], ensure_ascii=False, indent=2))
        return 0
    if any(value is None for value in (args.source, args.worktree, args.output)):
        parser.error("--source, --worktree and --output are required")
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    return execute(args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(f"ERROR {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(2)
