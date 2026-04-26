---
name: plan-eng-review
description: Force architecture, data flow, failure modes, trust boundaries, and test strategy into the open before implementation.
---

# plan-eng-review

## purpose

Turn product intent or an existing implementation plan into an execution-ready artifact that build, review, and QA can follow without re-inventing missing details.

## when to use

- After the product wedge and scope are understood
- Before large implementation work
- When a change touches architecture, external systems, state, or security boundaries

## inputs

- Existing project documents such as PRD, ERD, blueprint, and prior implementation notes
- `.ai/LOCAL/PLANS/current-sprint.md`
- `.ai/PLANS/implementation-plan-template.md`
- `.ai/ARCHITECTURE.md`
- `.ai/EVALS/failure-patterns.md`
- Relevant ADRs or incidents if they exist

## procedure

1. Read the existing plan and supporting docs, then identify what is still vague, oversized, internally inconsistent, or missing.
2. Map the proposed flow: trigger, data movement, state changes, storage boundaries, external dependencies, and trust boundaries.
3. Break the work into small execution units with explicit dependencies, changed surfaces, build steps, review focus, QA path, and measurable done criteria.
4. Convert missing tests and validation into an explicit test and validation matrix instead of leaving them as loose suggestions.
5. Convert every risk into one of three buckets: mitigated now, execution task, or true open question that requires outside confirmation.
6. Update `.ai/LOCAL/PLANS/current-sprint.md` or a linked plan artifact under `.ai/LOCAL/PLANS/` using the implementation plan template so build, review, and QA can consume it directly.
7. Update `.ai/ARCHITECTURE.md` or draft an ADR when the system shape, source of truth, or trust boundary changed materially.
8. Run `.ai/scripts/check-plan-readiness.sh` on the updated plan artifact and iterate until it passes or until a repeated blocked failure must be escalated through the circuit breaker path.

## outputs

- Revised implementation plan artifact
- Data flow and failure mode summary
- Trust boundary notes
- Test and validation matrix
- Review and QA handoff sections
- Explicit risk register and open question list

## escalation rules

- Escalate only when the architecture depends on unvalidated external assumptions, when a product or source-of-truth decision is genuinely missing, or when repeated plan rewrites still fail readiness and the circuit breaker opens.
- Do not stop at listing tests, validations, or mitigations when they can be added to the plan artifact immediately.

## handoff rules

- Hand off to `plan-design-review` if UX states still need clarification.
- Hand off to build skills only after the execution units, test matrix, and risk register are explicit enough to execute.
- Hand off to `review` and `qa` with the updated plan artifact as the canonical brief, not with an isolated chat summary.
