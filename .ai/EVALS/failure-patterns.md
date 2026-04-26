# Failure Patterns

## Purpose

Capture repeatable ways AI-assisted delivery can fail so planning, review, QA, and release skills can counter them.

## Starter patterns

- Shipping the first requested feature instead of the real product wedge
- Missing trust boundaries, data ownership, or failure modes in the plan
- Passing tests while real user flows still fail
- Generic UI that technically works but weakens product clarity
- Skipping rollback preparation because the change looked small
- Editing production code without updating relevant tests
- Brute-force retrying the same failing strategy instead of changing approach
- Hiding progress or risk state inside transient chat output instead of canonical artifacts
