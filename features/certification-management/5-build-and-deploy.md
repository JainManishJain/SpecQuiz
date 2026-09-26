---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: build-and-deploy
status: draft
sourceArtifact: features/certification-management/4-review.md
build_status: SUCCESS
---

# Build & Deploy — Certification Management

## Precondition Gate
- Review verdict (`4-review.md`): **PASS**
- Proceed: **yes**

The Phase 4 review gate passed after remediation (all findings F-001..F-005
resolved; one non-blocking minor F-008 deferred). Phase 5 is authorized.

## Build
| Item | Value |
|------|-------|
| Command | `./gradlew clean build` |
| Version / tag | 0.0.1-SNAPSHOT |
| Artifact | `build/libs/specquiz-0.0.1-SNAPSHOT.jar` (executable Spring Boot jar) |
| Also produced | `build/libs/specquiz-0.0.1-SNAPSHOT-plain.jar` (plain classes jar, not runnable) |
| Tests | 41 run, 41 passed, 0 failed |
| build_status | **SUCCESS** |

## Deployment Plan
This release targets local / development deployment (in-memory H2, sample data
bootstrapped on startup). Steps that differ for staging/production are noted.

| Step | Action | Notes |
|------|--------|-------|
| 1 | Build the artifact | `./gradlew clean build` |
| 2 | Run the application | `java -jar build/libs/specquiz-0.0.1-SNAPSHOT.jar` (or `./gradlew bootRun`) |
| 3 | Health check | `curl http://localhost:8080/api/ping` returns status `UP` |
| 4 | Verify bootstrap (REQ-009) | Open `http://localhost:8080/certifications`; two sample certifications ("AWS Certified Cloud Practitioner", "AWS Certified AI Practitioner") are listed, each with sections and >=5 questions |
| 5 | Smoke-test authoring | Create a certificate, add sections (weights total 100), add a question, click "Validate certificate" |

Configuration / secrets required (by name only): **none** for the dev H2 profile.

**Staging/production differences (not in scope for this release):**
- Replace in-memory H2 with a persistent datasource (authored data is currently lost on restart).
- Set `spring.jpa.hibernate.ddl-auto` to `validate` with managed migrations instead of `update`.
- Consider disabling `app.bootstrap.enabled` so sample data is not seeded into a real environment.
- Add authentication/authorization (explicitly out of scope in the FSD).

## Rollback Plan
| Step | Action |
|------|--------|
| 1 | Stop the running application (Ctrl+C, or stop the `java -jar` process / service) |
| 2 | Redeploy the previous artifact/tag (e.g. the prior `specquiz-<version>.jar`) |
| 3 | Verify health: `curl http://localhost:8080/api/ping` returns `UP` |

Because the dev database is in-memory, a restart discards authored data; there is no
data migration to reverse. For a persistent environment, take a database backup
before deploy and restore it on rollback.

## Release Notes
**Shipped:** Certification Management — the authoring side of SpecQuiz.
Authors can create certification exams with a title and pass percentage (REQ-001,
REQ-008), define weighted sections that are multiples of 20 and total 100
(REQ-002), author single-answer questions with four options and a one-line
explanation (REQ-003), edit and delete questions (REQ-004, REQ-005), configure the
exam length with proportional-to-weight selection (REQ-006), enforce full validity
on demand (REQ-007), and view all certifications and their questions (REQ-010). Two
realistic sample certifications are bootstrapped on startup (REQ-009). The UI is
server-rendered with Thymeleaf; question CRUD is exposed over a REST API.

**Requirements delivered:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006,
REQ-007, REQ-008, REQ-009, REQ-010 (plus NFRs REQ-050..055).

**Known limitations:**
- Authored data does not persist across restarts (in-memory H2 this release).
- Single-answer questions only; no multi-select or rich media.
- No authentication/authorization.
- AC-004 (choose existing/new section name) is realized in the UI but not covered by a dedicated automated test.
- F-008 (deferred): two error handlers lack direct tests; the constraint-violation handler is currently unreachable.
- The exam-taking experience is delivered separately by the Certification Attempt feature (FEAT-002).
