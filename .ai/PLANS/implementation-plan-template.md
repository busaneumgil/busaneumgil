# Implementation Plan Template

Use this when `plan-eng-review` or `plan` needs a build-ready artifact instead of loose notes.

## Source Documents

- PRD:
- ERD:
- Blueprint:
- Existing implementation plan:
- Excluded stale docs:

## Work Lane

- Lane: FE | BE | Cross-functional
- Scope boundary:
- Explicit non-goals:
- Cross-lane dependency summary:

## Problem List

- What is still vague, too large, or internally inconsistent?
- Which gaps can be closed now without waiting for a product decision?
- Which gaps are true external blockers?

## Architecture And Data Flow

- Trigger and entrypoint
- State changes
- Storage reads and writes
- External services and failure boundaries
- Auth and trust boundaries

## Execution Units

For each unit record:

1. Name and goal
2. Inputs and dependencies
3. Files or surfaces likely to change
4. Build steps
5. Review focus
6. QA or validation path
7. Done criteria

## Cross-Lane Handoff

- FE -> BE requests:
- BE -> FE requests:
- Contract questions:
- Owner or next action:

## Test And Validation Matrix

- Unit or module tests
- Contract tests
- Integration or ETL checks
- Security checks
- QA scenarios
- Benchmark or latency checks
- Release gating checks

## Risk Register

For each risk record:

- Risk statement
- Why it matters
- What can be mitigated now
- What remains open
- Owner or next action

## Review Handoff

- What reviewers should inspect first
- What changed versus the previous plan
- What is intentionally deferred

## QA Handoff

- Primary user flows
- Degraded mode or failure scenarios
- Required fixtures or environments
- Expected pass or fail signals

## Open Questions

- Leave only questions that truly require external confirmation or unavailable evidence.
- Convert everything else into execution units, tests, or risks.
