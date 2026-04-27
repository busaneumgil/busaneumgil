---
name: ship
description: Verify readiness gates, confirm docs and tests are aligned, and prepare the change to land safely.
---

# ship

## purpose

Provide a disciplined release gate before code is merged or deployed.

## when to use

- After review and QA
- When the team believes a change is ready to release

## inputs

- `.ai/LOCAL/PLANS/current-sprint.md`
- `.ai/DOCS.md` and source documents relevant to the shipped behavior
- `.ai/EVALS/done-criteria.md`
- `.ai/EVALS/smoke-checklist.md`
- `.ai/RUNBOOKS/release.md`
- `.ai/RUNBOOKS/rollback.md`

## procedure

1. Load `.ai/DOCS.md` and confirm the shipped behavior is checked against the relevant product, FE, API, ERD, infra, and convention docs.
2. Confirm the requested scope is complete enough to ship.
3. Verify review, QA, benchmark, security, and documentation outcomes are visible.
4. Confirm release and rollback readiness.
5. Record release status, source-document conflicts, stale assumptions, and accepted risks in the sprint artifact.
6. Update release documentation if behavior or operations changed.

## outputs

- Ship readiness decision
- Accepted-risk list
- Release artifact updates

## escalation rules

- Escalate if rollback is missing or implausible.
- Escalate if unresolved high-severity review or QA findings remain.

## handoff rules

- Hand off to `deploy-check` or actual release execution once readiness is green.
- Hand off to `document-release` when docs lag behind shipped behavior.
