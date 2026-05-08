# Debugging Memory

## Purpose

Capture recurring investigation patterns, sharp reproductions, and failure signatures that future sessions should not rediscover from scratch.

## Entry format

- Date
- Symptom
- Root cause
- Fastest reproduction path
- Durable fix
- Regression test or alert that should exist

## 2026-05-07

- Symptom: `POST /auth/social-login` returns HTTP 500 in both frontend social login flow and `auth-test.html`.
- Root cause: the `User` JPA entity uses camelCase property names without explicit snake_case `@Column(name=...)` mappings while Spring/Hibernate is configured with `PhysicalNamingStrategyStandardImpl`. Hibernate generated SQL against `users.userId`, `users.socialProvider`, `users.socialProviderUserId`, which PostgreSQL resolves to lowercase `userid`, `socialprovider`, `socialprovideruserid`. The actual dev table uses snake_case columns such as `user_id`, `social_provider`, `social_provider_user_id`, so the repository lookup fails before login or signup can proceed.
- Fastest reproduction path: trigger social login once on dev, then inspect `s14p31e102-dev-backend-1` logs for `column u1_0.userid does not exist` and compare with `psql \\d+ users` on dev DB.
- Durable fix: align entity mappings and schema naming. Prefer explicit `@Column(name = \"snake_case\")` and `@UniqueConstraint(columnNames = {...})` values that match the actual schema, then keep dev/prod on schema validation rather than relying on `ddl-auto:update` to mutate drifted tables.
- Regression test or alert that should exist: integration test for `AuthService.socialLogin` against a real Postgres schema with snake_case columns, plus startup validation or migration checks that fail if JPA mapping and DB naming diverge.
