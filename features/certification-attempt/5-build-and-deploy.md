---
featureId: FEAT-002
slug: certification-attempt
title: Certification Attempt
phase: build-and-deploy
status: draft
sourceArtifact: features/certification-attempt/4-review.md
build_status: SUCCESS
---

# Build & Deploy — Certification Attempt

## Precondition Gate
- Review verdict (`4-review.md`): **PASS**
- Proceed: **yes**

The Phase 4 review gate passed after remediation of the exam-form scoring blocker.
Phase 5 is authorized.

## Build
| Item | Value |
|------|-------|
| Command | `./gradlew clean build` |
| Version / tag | 0.0.1-SNAPSHOT |
| Artifact | `build/libs/specquiz-0.0.1-SNAPSHOT.jar` (executable Spring Boot jar) |
| Tests | 57 run, 57 passed, 0 failed (both features combined) |
| build_status | **SUCCESS** |

This feature adds no new dependencies, configuration keys, or database tables — it
is a stateless flow layered on the existing certificate model, so it deploys within
the same artifact as Certification Management.

## Deployment Plan
Local / development deployment (in-memory H2, sample data bootstrapped on startup).

| Step | Action | Notes |
|------|--------|-------|
| 1 | Build the artifact | `./gradlew clean build` |
| 2 | Run the application | `java -jar build/libs/specquiz-0.0.1-SNAPSHOT.jar` (or `./gradlew bootRun`) |
| 3 | Health check | `curl http://localhost:8080/api/ping` returns status `UP` |
| 4 | Smoke-test the attempt flow | Open `http://localhost:8080/attempts/new`, enter a name, pick a bootstrapped certification, start, answer, submit, and confirm the result page shows score, PASS/FAIL, and per-question feedback |

Configuration / secrets required (by name only): **none** for the dev H2 profile.

**Staging/production differences (not in scope this release):**
- Persist the certificate data (in-memory H2 discards it on restart); attempts
  themselves are intentionally not persisted.
- Add authentication if attempts should be tied to real users.

## Rollback Plan
| Step | Action |
|------|--------|
| 1 | Stop the running application |
| 2 | Redeploy the previous artifact/tag |
| 3 | Verify health: `curl http://localhost:8080/api/ping` returns `UP` |

Certification Attempt is stateless and read-only against the certificate data, so a
rollback has no data-migration implications.

## Release Notes
**Shipped:** Certification Attempt — the exam-taking side of SpecQuiz. A candidate
enters their name, selects a certification, and starts an attempt (REQ-001); the
system serves the configured number of questions drawn proportionally to section
weight on a single page with radio options and a submit control (REQ-002); on
submission it scores server-side, treating unanswered and now-missing questions as
incorrect, and reports PASS/FAIL against the certificate's pass percentage
(REQ-003); the result highlights each question correct/incorrect with the correct
option and its one-line explanation (REQ-004).

**Requirements delivered:** REQ-001, REQ-002, REQ-003, REQ-004 (plus NFRs REQ-050..053).

**Known limitations:**
- Attempts are not persisted (scored and shown once; no history).
- No timing, pausing, or resuming.
- Single-answer questions only; no authentication.
