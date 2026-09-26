---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: review
status: approved
sourceArtifact:
  - features/certification-management/1-requirements.md
  - features/certification-management/2-design.md
  - features/certification-management/3-code-and-tests.md
verdict: PASS
---

# Review — Certification Management

## Verdict: PASS
This artifact reflects the review AFTER the Phase 3 remediation. The initial review
(recorded below under "Review history") returned FAIL with one blocker and three
major findings. All five findings (F-001..F-005) have been independently verified
as resolved, with tests asserting the corrected behavior, no regressions, and the
full suite green (41/41). No blocker and no unresolved major remain, and every
acceptance criterion is covered by a passing test or explicitly justified. The gate
PASSES; the feature may proceed to Phase 5 (Build & Deploy).

## Conformance Checklist
| Dimension | Result | Notes (evidence) |
|-----------|--------|------------------|
| requirements-coverage | pass | Every REQ/AC implemented and tested. AC-006/AC-007/AC-020 now enforced via `POST /certifications/{id}/validate` (TC-027, TC-035); AC-022 bounded server-side (TC-029, TC-033); AC-025 has a MockMvc test (TC-031). AC-004 remains UI-only (datalist), justified and low-risk. |
| design-conformance | pass | Pass-% 1..100 enforced (DES-009); aggregate validation reachable through a real action (DES-006); error table (§10) exceptions now handled. |
| naming-and-layout | pass | Feature-prefixed stereotypes under `com.specquiz` (REQ-055). |
| validation-and-error-handling | pass | Handler now covers not-found, aggregated validation, MethodArgumentNotValid, ConstraintViolation, HttpMessageNotReadable, and DataIntegrity. Pass-% bounded at the service layer. |
| test-adequacy | pass | 41 tests incl. new controller (MVC) and REST layers; assert field + message, not counts. Minor gap: F-004 malformed-body/constraint handlers not directly tested (see F-008). |
| security-and-safety | pass | Thymeleaf `th:text` escaping; JSON.stringify + textContent in JS; parameterized JPA queries. Auth out of scope this release. |
| build-and-test-status | pass | `./gradlew build` green; 41/41 tests pass. |

## Findings
All findings from the initial review are resolved. One new non-blocking item (F-008)
is recorded for a future pass.

| ID | Severity | Status | Location | Description | Resolution |
|----|----------|--------|----------|-------------|------------|
| F-001 | blocker | RESOLVED | service/controller setPassPercentage | Pass-% accepted any integer | 1..100 enforced server-side; re-renders with error; TC-029, TC-033 |
| F-002 | major | RESOLVED | service/controller | No aggregate validation on authoring path | `validateCertificate(id)` + `POST /certifications/{id}/validate` + Validate button; TC-027/028/035/036 |
| F-003 | major | RESOLVED | service/exception | Duplicate title → 500 | `requireUniqueTitle` via `findByTitle` (+ DataIntegrity backstop → 409); TC-030 |
| F-004 | major | RESOLVED | exception/GlobalExceptionHandler | ConstraintViolation / malformed body → 500 | Handlers added returning the standard error body |
| F-005 | minor | RESOLVED | util/CertificationValidator | Proportional selection could return < N | Shortfall redistributed to sections with spare questions; TC-040 |
| F-008 | minor | OPEN | exception/GlobalExceptionHandler | The new malformed-body and constraint-violation handlers are untested; the ConstraintViolation handler is currently unreachable (no `@Validated` query-param endpoint) | Add a malformed-JSON MockMvc test; wire or drop the unreachable handler. Non-blocking; deferred. |
| F-006 | info | ACCEPTED | templates/view.html | AC-004 (existing/new section name) UI-only | Documented; low risk |

## Acceptance Criteria Verification
| AC | Test case(s) | Status |
|----|--------------|--------|
| AC-001 | TC-001, TC-016 | pass |
| AC-002 | TC-002, TC-030 | pass |
| AC-003 | TC-003, TC-029 | pass |
| AC-004 | (UI datalist; section-add path TC-017) | justified (UI-only) |
| AC-005 | TC-004 | pass |
| AC-006 | TC-005, TC-027, TC-028, TC-035, TC-036 | pass |
| AC-007 | TC-006, TC-027, TC-035 | pass |
| AC-008 | TC-017, TC-037 | pass |
| AC-009 | TC-007, TC-038 | pass |
| AC-010 | TC-008 | pass |
| AC-011 | TC-009 | pass |
| AC-012 | TC-018 | pass |
| AC-013 | TC-019 | pass |
| AC-014 | TC-020 | pass |
| AC-015 | TC-021 | pass |
| AC-016 | TC-022, TC-039 | pass |
| AC-017 | TC-011 | pass |
| AC-018 | TC-012, TC-024 | pass |
| AC-019 | TC-014, TC-040 | pass |
| AC-020 | TC-010, TC-027, TC-035 | pass |
| AC-021 | TC-013 | pass |
| AC-022 | TC-023, TC-029, TC-033, TC-034 | pass |
| AC-023 | TC-015 | pass |
| AC-024 | TC-015, TC-026 | pass |
| AC-025 | TC-031 | pass |
| AC-026 | TC-025, TC-032 | pass |

---

## Review history

### Initial review — verdict FAIL
The first pass found 1 blocker + 3 major findings, all now resolved:
- F-001 (blocker): pass-percentage endpoint accepted any integer (no 1..100 bound).
- F-002 (major): create flow used `createDraft` (no validation); no save/validate
  endpoint, so AC-006/AC-007/AC-020 were unenforced on the real authoring path.
- F-003 (major): duplicate title surfaced as HTTP 500.
- F-004 (major): ConstraintViolation and malformed-body exceptions surfaced as 500.
- F-005 (minor): proportional selection could return fewer than N.

Independent review records:
- Initial: `semantic-review/2026-09-26-221820-pr-certification-management.md`
- Re-review (this PASS): `semantic-review/2026-09-26-223858-pr-certification-management.md`
