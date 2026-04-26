---
name: start
description: Build an approved feature against the recorded plan instead of improvising from the latest prompt.
---

# start

## purpose

Execute planned feature work while keeping implementation tied to durable artifacts.

## when to use

- After the relevant planning reviews are complete
- When the work is a net-new feature or capability

## inputs

- Approved plan in `.ai/LOCAL/PLANS/current-sprint.md`
- `.ai/ARCHITECTURE.md`
- Relevant tests and runbooks

## procedure

1. Restate the approved feature scope and non-goals.
2. Before mutating shell state, run `.ai/scripts/check-dangerous-command.sh "<command>"`. Before editing implementation files, run `.ai/scripts/check-tdd-guard.sh --mode pre <candidate paths>`.
3. Implement the smallest coherent slice that satisfies the plan.
4. Add or update tests as the feature is built.
5. If the same implementation attempt fails repeatedly, run `.ai/scripts/record-retry.sh <signature>` and `.ai/scripts/check-circuit-breaker.sh <signature>` before retrying again.
6. Record any material plan deviation in `.ai/LOCAL/PLANS/current-sprint.md`.
7. Update architecture or runbooks if the change alters system behavior.

## outputs

- Feature implementation
- Tests for intended behavior
- Updated sprint artifact if the build revealed meaningful changes

## escalation rules

- Escalate if implementation requires changing the approved wedge, trust boundary, or release plan.
- Escalate if missing infrastructure or unclear ownership blocks progress.

## handoff rules

- Hand off to `review` and then `qa` once the implementation is coherent.
