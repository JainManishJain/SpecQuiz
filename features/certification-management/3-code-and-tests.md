---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: code-and-tests
status: approved
sourceArtifact: features/certification-management/2-design.md
reviewVerdict: PASS
---

# Code & Test Cases — Certification Management

## Implementation Summary
Implemented the Certification Management feature as a layered Spring Boot module
with a Thymeleaf authoring UI. The Certificate → Section → Question → Option
aggregate is persisted via Spring Data JPA to H2. `CertificationValidator`
performs aggregated structural validation (all violations reported together,
AC-021) and proportional question selection (AC-019). `CertificationService`
orchestrates certificate creation, section management, question CRUD, and
exam-length/pass-percentage updates. `CertificationController` renders the
Thymeleaf pages; `QuestionController` exposes REST CRUD used by the page's inline
editing. `CertificationDataLoader` bootstraps two valid sample certifications on
startup through the service so they pass the same validation (AC-024). A global
exception handler returns a structured error body with a `violations[]` array.
All 26 tests pass and `./gradlew build` is green.

## File Manifest
| File | Stereotype | Implements | Change |
|------|-----------|------------|--------|
| `src/main/java/com/specquiz/entity/Certificate.java` | Entity | DES-001 | new |
| `src/main/java/com/specquiz/entity/Section.java` | Entity | DES-002 | new |
| `src/main/java/com/specquiz/entity/Question.java` | Entity | DES-003 | new |
| `src/main/java/com/specquiz/entity/Option.java` | Entity | DES-003 | new |
| `src/main/java/com/specquiz/repository/CertificationRepository.java` | Repository | DES-001 | new |
| `src/main/java/com/specquiz/repository/QuestionRepository.java` | Repository | DES-004 | new |
| `src/main/java/com/specquiz/dto/CertificationRequest.java` | DTO | DES-001 | new |
| `src/main/java/com/specquiz/dto/SectionRequest.java` | DTO | DES-002 | new |
| `src/main/java/com/specquiz/dto/QuestionRequest.java` | DTO | DES-003 | new |
| `src/main/java/com/specquiz/dto/CertificationResponse.java` | DTO | DES-010 | new |
| `src/main/java/com/specquiz/dto/QuestionResponse.java` | DTO | DES-003 | new |
| `src/main/java/com/specquiz/util/CertificationValidator.java` | Util | DES-006 | new |
| `src/main/java/com/specquiz/service/CertificationService.java` | Service | DES-001 | new |
| `src/main/java/com/specquiz/controller/CertificationController.java` | Controller | DES-001 | new |
| `src/main/java/com/specquiz/controller/QuestionController.java` | Controller | DES-003 | new |
| `src/main/java/com/specquiz/config/CertificationDataLoader.java` | Config | DES-007 | new |
| `src/main/java/com/specquiz/exception/CertificationNotFoundException.java` | Exception | DES-004 | new |
| `src/main/java/com/specquiz/exception/CertificationValidationException.java` | Exception | DES-006 | new |
| `src/main/java/com/specquiz/exception/GlobalExceptionHandler.java` | Exception | DES-011 | new |
| `src/main/resources/templates/certifications/list.html` | Config | DES-010 | new |
| `src/main/resources/templates/certifications/form.html` | Config | DES-001 | new |
| `src/main/resources/templates/certifications/view.html` | Config | DES-010 | new |
| `build.gradle` | Config | DES-001 | modified |
| `src/test/java/com/specquiz/util/CertificationValidatorTest.java` | Test | DES-006 | modified |
| `src/test/java/com/specquiz/service/CertificationServiceTest.java` | Test | DES-001 | modified |
| `src/test/java/com/specquiz/controller/CertificationControllerTest.java` | Test | DES-001 | new |
| `src/test/java/com/specquiz/controller/QuestionControllerTest.java` | Test | DES-003 | new |

## Test Cases
| TC | Covers (AC) | Level | Description | Expected result |
|----|-------------|-------|-------------|-----------------|
| TC-001 | AC-001 | unit | Fully valid certificate validates | No violations |
| TC-002 | AC-002 | unit | Blank title | Violation on `title` |
| TC-003 | AC-003 | unit | Pass % out of 1..100 | Violation on `passPercentage` |
| TC-004 | AC-005 | unit | Weight not multiple of 20 | Violation on `section.weight` |
| TC-005 | AC-006 | unit | Weights not summing to 100 | Violation citing current total |
| TC-006 | AC-007 | unit | No sections | Violation on `sections` |
| TC-007 | AC-009 | unit | Not exactly 4 options | Violation on `question.options` |
| TC-008 | AC-010 | unit | Not exactly 1 correct | Violation on `question.correct` |
| TC-009 | AC-011 | unit | Blank text/explanation | Violations on both |
| TC-010 | AC-020 | unit | Fewer than 5 questions total | Violation on `questions` |
| TC-011 | AC-017 | unit | questionsToAsk < 5 | Violation on `questionsToAsk` |
| TC-012 | AC-018 | unit | questionsToAsk > pool | Violation citing pool |
| TC-013 | AC-021 | unit | Multiple broken rules | All violations aggregated (>=3) |
| TC-014 | AC-019 | unit | Proportional selection 80/20 of 5 | 4 from A, 1 from B, total 5 |
| TC-015 | AC-023, AC-024 | integration | Startup bootstrap | >=2 valid certs, each >=5 questions, weights=100 |
| TC-016 | AC-001 | integration | Create valid certificate | Persisted with id |
| TC-017 | AC-008 | integration | Add valid question | Section pool grows by 1 |
| TC-018 | AC-012 | integration | Edit question validly | Text + correct option updated |
| TC-019 | AC-013 | integration | Invalid edit | Rejected; prior version retained |
| TC-020 | AC-014 | integration | Edit missing question | CertificationNotFoundException |
| TC-021 | AC-015 | integration | Delete question | Removed from pool |
| TC-022 | AC-016 | integration | Delete missing question | CertificationNotFoundException |
| TC-023 | AC-022 | integration | Set pass percentage | Stored value updated |
| TC-024 | AC-018 | integration | Exam length above pool | CertificationValidationException |
| TC-025 | AC-026 | integration | Get certificate structure | Sections + questions returned |
| TC-026 | AC-024 | integration | create() rejects invalid aggregate | CertificationValidationException (protects bootstrap) |
| TC-027 | AC-006, AC-020 | integration | validateCertificate(id) rejects invalid draft (F-002) | Violations on sections/questions |
| TC-028 | AC-006 | integration | validateCertificate(id) accepts valid | Returns certificate |
| TC-029 | AC-022, AC-003 | integration | setPassPercentage rejects 0 and 200 (F-001) | CertificationValidationException |
| TC-030 | AC-002 | integration | create() rejects duplicate title (F-003) | Violation on title |
| TC-031 | AC-025 | integration (MVC) | List page renders with model | 200, view certifications/list, model certificates |
| TC-032 | AC-026 | integration (MVC) | View page renders | 200, view certifications/view |
| TC-033 | AC-022 | integration (MVC) | Pass % out of range re-renders with error (F-001) | 200, passPercentageError present |
| TC-034 | AC-022 | integration (MVC) | Valid pass % redirects | 3xx redirect |
| TC-035 | AC-006, AC-020 | integration (MVC) | Validate invalid draft shows errors (F-002) | 200, validationErrors present |
| TC-036 | AC-006 | integration (MVC) | Validate valid cert shows success (F-002) | 200, validationOk present |
| TC-037 | AC-008 | integration (REST) | Add valid question | 201 with id + 4 options |
| TC-038 | AC-009 | integration (REST) | Wrong option count | 400 with violations array |
| TC-039 | AC-016 | integration (REST) | Delete missing question | 404 with status body |
| TC-040 | AC-019 | unit | Proportional selection redistributes shortfall (F-005) | Exactly N, took all of tiny section |

## Acceptance Criteria Coverage
| AC | Covered by |
|----|-----------|
| AC-001 | TC-001, TC-016 |
| AC-002 | TC-002 |
| AC-003 | TC-003 |
| AC-004 | TC-017 (section used); UI form (view.html) |
| AC-005 | TC-004 |
| AC-006 | TC-005, TC-027, TC-028, TC-035, TC-036 |
| AC-007 | TC-006, TC-027, TC-035 |
| AC-008 | TC-017 |
| AC-009 | TC-007 |
| AC-010 | TC-008 |
| AC-011 | TC-009 |
| AC-012 | TC-018 |
| AC-013 | TC-019 |
| AC-014 | TC-020 |
| AC-015 | TC-021 |
| AC-016 | TC-022 |
| AC-017 | TC-011 |
| AC-018 | TC-012, TC-024 |
| AC-019 | TC-014, TC-040 |
| AC-020 | TC-010, TC-027, TC-035 |
| AC-021 | TC-013 |
| AC-022 | TC-023, TC-029, TC-033, TC-034 |
| AC-023 | TC-015 |
| AC-024 | TC-015, TC-026 |
| AC-025 | TC-031 |
| AC-026 | TC-025, TC-032 |

> Note: AC-004 (choose existing/new section name) remains realized in the Thymeleaf
> UI (datalist) and exercised via the section-add service path; it is not asserted
> by a dedicated automated UI test. AC-025 now has a dedicated MockMvc test (TC-031).

## Remediation (post Phase 4 review)
Addressed review findings F-001..F-005:
- **F-001** — `setPassPercentage` now enforces 1..100 server-side; MVC endpoint re-renders with an error (TC-029, TC-033).
- **F-002** — added `validateCertificate(id)` service method and `POST /certifications/{id}/validate` so aggregate rules (AC-006/007/020) are enforced through a real authoring action (TC-027, TC-028, TC-035, TC-036).
- **F-003** — duplicate title rejected via `findByTitle` pre-check with a structured violation; `DataIntegrityViolationException` mapped to 409 as defense-in-depth (TC-030).
- **F-004** — `GlobalExceptionHandler` now handles `ConstraintViolationException`, `HttpMessageNotReadableException`, and `DataIntegrityViolationException`.
- **F-005** — `selectProportionally` redistributes any shortfall so the selection sums to N when the pool allows (TC-040).
- Added controller-layer tests (MockMvc) that previously did not exist.

## Build / Test Result
- `./gradlew build`: PASS
- Tests run: 41, passed: 41, failed: 0 (14→15 validator, 11→16 service, +6 MVC, +3 REST, 1 context load)
