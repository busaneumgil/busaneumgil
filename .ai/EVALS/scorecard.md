# Scorecard

## Purpose

Use this file with `.ai/scripts/score.sh` to judge whether a cloned repository has moved from template state to project-ready state.

## Readiness lenses

- Identity clarity
- Workflow integrity
- Canonical versus generated discipline
- Smoke and release command readiness
- Memory and evaluation hygiene
- Structured progress visibility
- Retry and failure discipline
- Backend design analysis completeness
- Backend failure scenario coverage
- Backend verification and metrics evidence
- Dashboard visibility

## Interpretation

- High score: the repository behaves like a real project with durable AI workflow artifacts.
- Medium score: the structure exists, but project-specific commands or memory are still thin.
- Low score: the repository still looks like an untouched template.

## Readiness Note - 2026-05-10 Route API flow QA

- Route API official dev QA is not release-ready yet.
- Evidence: `.ai/LOCAL/EVALS/smoke/2026-05-10-140253-route-api-flow/`
- WALK-only flow passed 8/8 across user types and SAFE/FAST tracks.
- Transit flows failed 24/24 on S1 dev backend because `POST /routes/{routeId}/transit-refresh` returns `404 C4040`.
- Current branch local backend validated representative BUS and SUBWAY refresh against S1 dev DB/Redis, so the main blocker is S1 dev deployment/version drift, not the isolated refresh implementation.
