#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/.ai/scripts/harness-paths.sh"
harness_ensure_local_state
"$ROOT_DIR/.ai/scripts/update-progress.sh" >/dev/null
"$ROOT_DIR/.ai/scripts/update-metrics.sh" >/dev/null

python3 - <<'PY' "$ROOT_DIR" "$AI_PLAN_DIR" "$AI_EVAL_DIR"
import json
import re
import sys
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path

root = Path(sys.argv[1])
plan_dir = Path(sys.argv[2])
eval_dir = Path(sys.argv[3])
plan_path = plan_dir / "current-sprint.md"
progress_path = plan_dir / "progress.json"
metrics_path = eval_dir / "metrics.json"
retry_path = eval_dir / "retry-log.jsonl"


def strip_md(text):
    text = re.sub(r"`([^`]+)`", r"\1", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"\1", text)
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
    return text.strip()


def cut(text, width=58):
    text = strip_md(text)
    return text if len(text) <= width else text[: width - 1] + "…"


def bar(done, total, width=18):
    if total <= 0:
        return "░" * width
    filled = round(width * done / total)
    return "█" * filled + "░" * (width - filled)


def print_box(title, lines):
    width = 72
    print("┌" + "─" * (width - 2) + "┐")
    print("│ " + title.ljust(width - 4) + " │")
    print("├" + "─" * (width - 2) + "┤")
    for line in lines:
        print("│ " + line[: width - 4].ljust(width - 4) + " │")
    print("└" + "─" * (width - 2) + "┘")


def parse_checklist(path):
    if not path.exists():
        return []
    items = []
    pattern = re.compile(r"^\s*(?:[-*+]|\d+[.)])\s+\[([ xX])\]\s+(.+?)\s*$")
    for line in path.read_text(encoding="utf-8").splitlines():
        match = pattern.match(line)
        if not match:
            continue
        checked = match.group(1).lower() == "x"
        label = match.group(2)
        lower = label.lower()
        if "blocked" in lower or "막힘" in label or "차단" in label:
            status = "막힘"
        elif checked:
            status = "완료"
        elif "진행" in label or "in progress" in lower:
            status = "진행"
        else:
            status = "대기"
        items.append({"label": label, "checked": checked, "status": status})
    return items


def retry_hot_clusters(path):
    clusters = Counter()
    window_start = datetime.now(timezone.utc) - timedelta(hours=24)
    if not path.exists():
        return {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        try:
            item = json.loads(line)
            sig = item.get("signature", "")
            ts = item.get("timestamp", "")
            note = item.get("note", "")
            if not sig or sig == "placeholder" or note:
                continue
            dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
            if dt >= window_start:
                clusters[sig] += 1
        except Exception:
            continue
    return {sig: cnt for sig, cnt in clusters.items() if cnt >= 3}


items = parse_checklist(plan_path)
if not plan_path.exists():
    print_box(
        "부산이음길 작업 대시보드",
        [
            "계획 파일이 없습니다.",
            "먼저 /plan 으로 실행 가능한 계획을 세워주세요.",
            f"예상 위치: {plan_path.relative_to(root)}",
        ],
    )
    raise SystemExit(0)

if not items:
    print_box(
        "부산이음길 작업 대시보드",
        [
            "계획 체크리스트가 없습니다.",
            "먼저 /plan 으로 체크 가능한 작업 목록을 만들어주세요.",
            f"계획 파일: {plan_path.relative_to(root)}",
        ],
    )
    raise SystemExit(0)

done = sum(1 for item in items if item["checked"])
blocked = sum(1 for item in items if item["status"] == "막힘")
active = sum(1 for item in items if item["status"] == "진행")
waiting = len(items) - done - blocked - active
percent = round(done / len(items) * 100)
hot_clusters = retry_hot_clusters(retry_path)

metrics = {}
if metrics_path.exists():
    try:
        metrics = json.loads(metrics_path.read_text(encoding="utf-8"))
    except Exception:
        metrics = {}

progress = {}
if progress_path.exists():
    try:
        progress = json.loads(progress_path.read_text(encoding="utf-8"))
    except Exception:
        progress = {}

title = progress.get("sprint_name") or plan_path.stem
lines = [
    f"계획: {title}",
    f"진행률  {bar(done, len(items))}  {percent}%  ({done}/{len(items)} 완료)",
    f"상태    진행 {active} · 대기 {waiting} · 막힘 {blocked}",
    f"리스크  반복 실패 {len(hot_clusters)} · 하네스 상태 {metrics.get('harness_health_score', '확인 전')}",
]
print_box("부산이음길 작업 대시보드", lines)

print("")
print("다음 작업")
open_items = [item for item in items if not item["checked"]]
for index, item in enumerate(open_items[:6], start=1):
    marker = "!" if item["status"] == "막힘" else "›"
    print(f"{marker} {index}. [{item['status']}] {cut(item['label'])}")
if not open_items:
    print("✓ 모든 체크리스트가 완료되었습니다. /review 또는 /qa 로 검증을 진행하세요.")

if hot_clusters:
    print("")
    print("주의")
    for sig, cnt in sorted(hot_clusters.items(), key=lambda x: -x[1]):
        print(f"! 반복 실패 {cnt}회: {cut(sig)}")
    print("! 같은 접근을 반복하기 전에 /learn 또는 /investigate 로 원인을 정리하세요.")

print("")
print("추천 명령")
if not open_items:
    print("- /review 현재 변경사항을 검토해줘")
    print("- /qa 실제 사용자 흐름 기준으로 검증해줘")
elif blocked:
    print("- /investigate 막힌 원인을 증거 기반으로 분석해줘")
    print("- /plan 현재 계획을 다시 실행 가능하게 쪼개줘")
else:
    print("- /start 다음 체크리스트 항목을 구현해줘")
    print("- /fix-bug 실패한 항목을 재현하고 수정해줘")
PY
