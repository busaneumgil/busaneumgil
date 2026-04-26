#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WRITE_README=0
FORCE=0

usage() {
  cat <<'USAGE'
usage: .ai/scripts/install-root-entrypoints.sh [--write-readme] [--force]

Create root entrypoint files that point hosts and humans at the canonical .ai/ docs.
- --write-readme: also replace the root README.md with a harness pointer
- --force: overwrite existing target files
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --write-readme)
      WRITE_README=1
      ;;
    --force)
      FORCE=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

write_file() {
  local target="$1"
  local contents="$2"

  if [[ -e "$target" && "$FORCE" -ne 1 ]]; then
    echo "install-root-entrypoints: skipped existing $(basename "$target") (use --force to overwrite)"
    return
  fi

  printf '%s\n' "$contents" > "$target"
  echo "install-root-entrypoints: wrote $(basename "$target")"
}

agents_doc=$(cat <<'DOC'
# AGENTS.md

Canonical Codex instructions live in `.ai/AGENTS.md`.

## Setup

- If this repository adopted the harness by copying only `.ai/`, run `./.ai/scripts/install-root-entrypoints.sh`.
- Read and follow `.ai/AGENTS.md` before implementation work.
- Run `./.ai/scripts/sync-adapters.sh` after canonical skill changes; generated adapters live under `.ai/.agents`, `.ai/.claude`, and `.ai/.codex`.
DOC
)

claude_doc=$(cat <<'DOC'
# CLAUDE.md

Canonical Claude instructions live in `.ai/CLAUDE.md`.

## Setup

- If this repository adopted the harness by copying only `.ai/`, run `./.ai/scripts/install-root-entrypoints.sh`.
- Read and follow `.ai/CLAUDE.md` before major changes.
- Run `./.ai/scripts/sync-adapters.sh` after canonical skill changes; generated adapters live under `.ai/.agents`, `.ai/.claude`, and `.ai/.codex`.
DOC
)

readme_doc=$(cat <<'DOC'
# AI Harness 시작점

하네스의 canonical 문서는 `.ai/README.md`에 있습니다.

## 초기 설정

1. `./.ai/scripts/install-root-entrypoints.sh` 실행
2. `./.ai/scripts/bootstrap-template.sh "프로젝트 이름"` 실행
3. `./.ai/scripts/sync-adapters.sh` 실행
4. `./.ai/scripts/verify.sh` 실행
DOC
)

write_file "$ROOT_DIR/AGENTS.md" "$agents_doc"
write_file "$ROOT_DIR/CLAUDE.md" "$claude_doc"
if [[ "$WRITE_README" -eq 1 ]]; then
  write_file "$ROOT_DIR/README.md" "$readme_doc"
fi
