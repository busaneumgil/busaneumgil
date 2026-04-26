---
name: fix-bug
description: Reproduce a bug, identify the actual failure mechanism, land the smallest safe fix, and add regression coverage.
---

# fix-bug

## purpose

Resolve defects without treating the first apparent symptom as the root cause.

## when to use

- For confirmed bugs, regressions, and production issues
- When a failure needs a bounded fix and regression test

## inputs

- Bug report or observed failure
- Relevant sprint, debugging memory, and incident context
- Existing tests and logs

## procedure

1. Reproduce the bug or define the strongest available reproduction path.
2. Confirm the root cause instead of patching the nearest symptom.
3. Before mutating shell state, run `.ai/scripts/check-dangerous-command.sh "<command>"`. Before editing implementation files, run `.ai/scripts/check-tdd-guard.sh --mode pre <candidate paths>`.
4. Apply the smallest safe fix that addresses the actual failure.
5. Add regression coverage.
6. If the same fix path fails repeatedly, run `.ai/scripts/record-retry.sh <signature>` and `.ai/scripts/check-circuit-breaker.sh <signature>` before retrying again.
7. If the bug exposed a reusable lesson, update `.ai/MEMORY/debugging.md` or `.ai/MEMORY/incidents.md`.

## outputs

- Reproduction note
- Root cause summary
- Bug fix
- Regression test

## escalation rules

- Escalate if the bug is not reproducible and evidence is weak.
- Escalate if the fix would materially change product scope or architecture.

## handoff rules

- Hand off to `review` for risk checking.
- Hand off to `qa` when the bug affects a real user flow.
