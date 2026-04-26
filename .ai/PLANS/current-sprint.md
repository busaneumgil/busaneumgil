# Current Sprint

## Goal

Adopt the template into a real repository while preserving the end-to-end loop.

## Source Documents

- Narrative plan: this file
- Machine-readable progress: `.ai/LOCAL/PLANS/progress.json`
- Quality and readiness metrics: `.ai/LOCAL/EVALS/metrics.json`
- Reusable planning template: `.ai/PLANS/implementation-plan-template.md`

## Problem List

- The template still contains project-agnostic placeholders that a real repository must close.
- Codex and Claude host behavior must stay thin while `.ai/` remains canonical.
- The harness should be portable enough that copying only `.ai/` into another repository is sufficient to bootstrap it.
- A real project needs execution-ready planning artifacts, not just narrative task lists.
- Verification currently mixes local host state with repository correctness when ignored local files are present.
- Newly added code needs a deterministic validation gate before the harness reports readiness.

## Architecture And Data Flow

- Canonical methodology and skills live under `.ai/`.
- `.ai/scripts/` is the canonical harness command surface, and root entrypoints are installable shims.
- `.ai/scripts/sync-adapters.sh` generates `.ai/.ai/.claude/skills/`, `.ai/.ai/.agents/skills/`, and runtime adapter files.
- Guard, scoring, and dashboard scripts read canonical `.ai/` artifacts.
- Codex and Claude consume generated adapters plus root guidance files.
- Agents execute role-scoped work, skills provide reusable operating manuals, and the workflow plus scripts act as the orchestrator.
- Repeated agent mistakes should become stronger guards, evals, memory, skills, or ADRs instead of staying as one-off prompt fixes.

## Execution Units

### HARNESS-1 Customize project identity

- Inputs: `.ai/PROJECT.md`, README, public repo metadata
- Build steps: replace template identity and project wedge placeholders
- Review focus: identity consistency across docs
- QA path: `.ai/scripts/verify.sh`
- Done criteria: default identity placeholders are removed

### HARNESS-2 Customize smoke and runbooks

- Inputs: `.ai/RUNBOOKS/`, `.ai/scripts/smoke.sh`
- Build steps: replace TODO command slots with project-specific commands
- Review focus: command realism and operator clarity
- QA path: `.ai/scripts/smoke.sh`, runbook spot-check
- Done criteria: setup, release, rollback, and smoke commands are executable or intentionally stubbed with rationale

### HARNESS-3 Wire host-specific guard behavior

- Inputs: `.ai/ADAPTERS/`, guard scripts, runtime needs
- Build steps: connect only the necessary host-specific hooks and preflight paths
- Review focus: safety, determinism, and minimalism
- QA path: `.ai/scripts/pipeline-check.sh`, hook simulations
- Done criteria: chosen host guard flow is documented and tested

### HARNESS-4 Record real metrics

- Inputs: `.ai/LOCAL/EVALS/metrics.json`, first real project runs
- Build steps: replace template null metrics with observed values and update score interpretation
- Review focus: metric meaning and signal quality
- QA path: `.ai/scripts/dashboard.sh`, `.ai/scripts/score.sh`
- Done criteria: quality metrics reflect real usage rather than template defaults

### HARNESS-5 Capture first durable learnings

- Inputs: retry log and early project failures
- Build steps: record at least one recurring pattern into MEMORY, SKILL, EVAL, or ADR
- Review focus: whether the recorded lesson is durable and specific
- QA path: `.ai/scripts/dashboard.sh`, artifact inspection
- Done criteria: the harness improved itself with at least one real project learning

### HARNESS-6 Harden verification gates

- Inputs: `.ai/scripts/verify.sh`, guard scripts, local-only host files, `.ai/GUARDS.md`
- Build steps: add changed-code validation, allow ignored local-only Claude settings, and fail only when local-only files are tracked or unignored
- Review focus: strong validation without blocking normal local permissions files
- QA path: `.ai/scripts/check-code-validation.sh`, `.ai/scripts/verify.sh`
- Done criteria: new code validation runs from verify, and ignored `.ai/.ai/.claude/settings.local.json` no longer breaks verification

## Test And Validation Matrix

- Structure validation: `.ai/scripts/verify.sh`
- Adapter sync validation: `.ai/scripts/sync-adapters.sh`
- Pipeline validation: `.ai/scripts/pipeline-check.sh`
- Guard validation: dangerous command, TDD guard, and circuit breaker script checks
- Code validation: shell syntax, structured data parsing, conflict marker scan, and changed language syntax checks
- Runtime validation: Codex preflight and Claude review handoff scripts
- Smoke validation: `.ai/scripts/smoke.sh` after project commands are customized

## Risk Register

- Risk: teams may treat generated adapters as editable sources.
  Mitigation now: keep `.ai/ADAPTERS/` explicit and verify sync.
  Open remainder: real project adoption discipline still depends on usage.
- Risk: host runtimes may differ in hook capability.
  Mitigation now: keep actual enforcement in scripts and instructions, use runtime hooks only as thin adapters.
  Open remainder: stronger runtime integration depends on stable host support.
- Risk: planning may stop at analysis without producing execution-ready artifacts.
  Mitigation now: strengthen `plan-eng-review`, add template, add readiness check.
  Open remainder: real project teams must actually run the readiness check.
- Risk: stricter validation may block local-only host settings that should remain outside version control.
  Mitigation now: verify tracked and ignored state instead of file existence.
  Open remainder: host runtimes may introduce additional local-only files that need explicit policy.

## Review Handoff

- Reviewers should inspect whether canonical versus generated boundaries are preserved.
- Reviewers should confirm the execution units, done criteria, and risk handling match the requested workflow.
- Review should challenge any plan artifact that only lists gaps instead of converting them into work, tests, or explicit blockers.
- Reviewers should verify that guard failures are structural and actionable rather than caused by legitimate local-only host state.

## QA Handoff

- QA should verify the guard scripts, dashboard visibility, and score outputs against the documented workflow.
- QA should fail the stage if project-specific smoke and runbook commands remain placeholders without rationale.
- QA should confirm that planning artifacts are actionable enough for build, review, and release.
- QA should run code validation directly and through `.ai/scripts/verify.sh`.

## Open Questions

- Which host runtime features are stable enough to justify stronger automatic enforcement?
- Which additional machine-readable plan fields are worth formalizing after real project adoption?
