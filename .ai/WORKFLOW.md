# Workflow

## Sprint loop

The default loop is:

Think -> Plan -> Build -> Review -> Test -> Ship -> Reflect

The automation-oriented execution shape is:

request or event -> classification -> doc and skill loading -> planning -> implementation -> evaluation -> risk summary -> scoring -> dashboard update

The harness control shape is:

intent -> orchestrator selects stage -> agent loads skill -> guarded tool use -> deterministic validation -> review agent or reviewer -> QA signal -> shared learning update when needed

## Team contract

This file is the shared team workflow contract, not a personal daily checklist. Team members may skip or compress stages for small tasks, but the handoff expectations stay the same: plan enough to implement safely, review against the plan, validate the change, and record durable lessons when they affect future work.

## Stage contracts

### Think

- Primary skills: `make`
- Goal: turn vague intent into a sharper problem definition, wedge, user, and non-goals
- Main outputs: updated framing in `.ai/LOCAL/PLANS/current-sprint.md` and relevant backlog or roadmap adjustments
- Handoff: planning stages inherit the clarified problem instead of the original loose request

### Plan

- Primary skills: `plan-ceo-review`, `plan-eng-review`, `plan-design-review`, `plan`
- Goal: challenge scope, architecture, interaction quality, failure modes, trust boundaries, and test strategy before implementation
- Main outputs: reusable plan sections in `.ai/LOCAL/PLANS/current-sprint.md` or a linked implementation plan artifact, explicit execution units, test and validation matrix, risk register, optional ADR drafts, backlog or roadmap deltas
- Handoff: build, review, and QA consume these artifacts directly

### Build

- Primary skills: `start`, `fix-bug`, `refactor-module`, `write-test`, `investigate`
- Goal: execute against an approved plan with clear boundaries and evidence
- Main outputs: code changes, tests, and implementation notes recorded in sprint artifacts when behavior or scope changed
- Handoff: review inherits the approved plan, not just the diff

### Review

- Primary skills: `review`, `design-review`, `security-review`
- Goal: inspect correctness, maintainability, product integrity, and risk
- Main outputs: findings, resolved risks, open questions, and review notes linked from `.ai/LOCAL/PLANS/current-sprint.md`
- Handoff: QA and release should consume unresolved risks explicitly

### Test

- Primary skills: `qa`, `qa-only`, `benchmark`
- Goal: verify real user flows, failure cases, and performance expectations
- Main outputs: bug and risk reports, smoke-check references, scorecard updates, regression notes
- Handoff: ship consumes readiness status rather than assuming tests passed means production-ready

### Ship

- Primary skills: `ship`, `canary`, `deploy-check`, `document-release`
- Goal: verify readiness gates, release safely, and keep release docs aligned
- Main outputs: release checklist status, deployment verification, rollback readiness, release notes
- Handoff: try consumes what actually happened, not what was intended

### Reflect

- Primary skills: `try`, `learn`, `dashboard`
- Goal: capture what changed in the team or project system so the next sprint is better
- Main outputs: memory updates, evaluation updates, ADR follow-ups, skill improvements, recurring pattern capture
- Handoff: future work starts with a better repository memory state

## Artifact movement

- Shared plan templates and examples live in `.ai/PLANS/`.
- Personal sprint plans and structured task state live in `.ai/LOCAL/PLANS/`.
- Shared quality gates and recurring failure knowledge live in `.ai/EVALS/`.
- Personal quality and readiness metrics live in `.ai/LOCAL/EVALS/metrics.json`.
- Reusable operational and debugging memory lives in `.ai/MEMORY/`.
- Structural decisions live in `.ai/DECISIONS/`.
- Runbooks describe deterministic setup, release, and rollback behavior under `.ai/RUNBOOKS/`.
- Guard policy lives in `.ai/GUARDS.md`.

## Guard rails

- Production edits should pass through the TDD guard before automation is allowed to treat them as ready.
- Shell execution should pass through the dangerous command guard before automation executes high-risk commands.
- Repeated equivalent failures should pass through the circuit breaker before more retries are attempted.
- Newly added or changed code should pass deterministic validation before `verify` treats the harness as ready.
- Local-only host files should be ignored and untracked, but their mere presence should not block repository verification.

## Learning paths

- One-off issue: leave local unless it is severe.
- Repeated issue: record it in memory.
- Repeated procedure: update or add a skill.
- Repeated completion ambiguity: tighten evals or workflow.
- Architecture-level tradeoff: write an ADR.

## Operating rule

If a stage creates output that another stage will need later, store it in `.ai/` instead of leaving it in transient chat context.

Planning is not done when it only lists gaps. Planning is done when executable tasks, measurable done criteria, validation work, and unresolved external blockers are separated into durable artifacts.
