---
name: review
description: Review changes for correctness, maintainability, edge cases, missing tests, and production risk before the code moves forward.
---

# review

## purpose

Inspect the branch like a strong human reviewer would, with emphasis on what happy-path implementation tends to miss.

## when to use

- After implementation or bug fixing
- Before QA or shipping
- When a risky change needs a hard correctness pass

## inputs

- Current diff or changed files
- Optional runtime-generated review handoff such as `.ai/scripts/codex-review-brief.sh`
- `.ai/LOCAL/PLANS/current-sprint.md`
- Any linked implementation plan artifact under `.ai/LOCAL/PLANS/`
- Relevant tests, architecture notes, and incidents

## procedure

1. Review the intended scope and compare it with the actual change.
2. Look for correctness issues, maintainability problems, missing tests, and hidden risks.
3. Cross-check any runtime handoff summary against the actual diff instead of trusting it blindly.
4. Record findings and open questions in the sprint artifact.
5. Make sure unresolved items are visible to QA and release stages.

## outputs

- Review findings
- Risk summary
- Missing-test or maintainability notes

## escalation rules

- Escalate if the implementation diverged materially from the approved plan.
- Escalate if unresolved correctness risks remain high.

## handoff rules

- Hand off to `qa` when the change affects real flows.
- Hand off to `ship` only after high-risk findings are resolved or explicitly accepted.
