---
name: qa-only
description: Run the same real-flow validation as QA but stop at reporting instead of changing code.
---

# qa-only

## purpose

Provide an independent QA report without mixing testing and implementation in the same pass.

## when to use

- When a pure test report is preferred
- When code changes are blocked or should be separated from validation

## inputs

- Current change and release candidate context
- `.ai/EVALS/smoke-checklist.md`
- Relevant review notes

## procedure

1. Select the highest-value flows for the release.
2. Execute them and collect evidence.
3. Report bugs, inconsistencies, and risk areas in `.ai/LOCAL/PLANS/current-sprint.md`.
4. Update score or readiness notes if the report changes release confidence.

## outputs

- QA report without code changes
- Bug list and risk list

## escalation rules

- Escalate if high-severity issues are found and no owner is assigned.
- Escalate if the release cannot be judged without missing credentials, environments, or fixtures.

## handoff rules

- Hand off to `fix-bug`, `start`, or `ship` depending on the report outcome.
