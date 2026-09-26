---
featureId: FEAT-002
slug: certification-attempt
title: Certification Attempt
phase: code-and-tests
status: approved
sourceArtifact: features/certification-attempt/2-design.md
reviewVerdict: PASS
---

# Code & Test Cases — Certification Attempt

## Implementation Summary
Implemented the exam-taking flow as a stateless feature over the existing
Certification Management model (no new entities or tables). `AttemptController`
renders a start page (name + certification dropdown), an exam page (all served
questions on one page with radio options and a submit button), and a result page.
`AttemptService` selects questions by reusing `CertificationService.selectQuestionsForAttempt`
(which delegates to the existing proportional-selection rule) and scores the
submission server-side: it recomputes each question's correct option from persisted
data, treats unanswered or now-missing questions as incorrect, computes the score as
the percentage correct, and marks PASS when the score meets the certificate's pass
percentage. The result page highlights each question correct/incorrect and shows the
correct option plus the one-line explanation. Build is green; 53 tests pass overall
(12 new for this feature).

## File Manifest
| File | Stereotype | Implements | Change |
|------|-----------|------------|--------|
| `src/main/java/com/specquiz/dto/StartAttemptRequest.java` | DTO | DES-001 | new |
| `src/main/java/com/specquiz/dto/AttemptView.java` | DTO | DES-002 | new |
| `src/main/java/com/specquiz/dto/AttemptSubmission.java` | DTO | DES-003 | new |
| `src/main/java/com/specquiz/dto/AttemptResult.java` | DTO | DES-005 | new |
| `src/main/java/com/specquiz/service/AttemptService.java` | Service | DES-002 | new |
| `src/main/java/com/specquiz/service/CertificationService.java` | Service | DES-004 | modified |
| `src/main/java/com/specquiz/controller/AttemptController.java` | Controller | DES-001 | new |
| `src/main/resources/templates/attempts/start.html` | Config | DES-001 | new |
| `src/main/resources/templates/attempts/exam.html` | Config | DES-002 | new |
| `src/main/resources/templates/attempts/result.html` | Config | DES-005 | new |
| `src/main/resources/templates/attempts/exam.html` | Config | DES-002 | modified (served-id fix) |
| `src/test/java/com/specquiz/service/AttemptServiceTest.java` | Test | DES-005 | new |
| `src/test/java/com/specquiz/controller/AttemptControllerTest.java` | Test | DES-001 | new |

## Test Cases
| TC | Covers (AC) | Level | Description | Expected result |
|----|-------------|-------|-------------|-----------------|
| TC-001 | AC-001, AC-005, AC-006 | integration | Start valid attempt | Serves questionsToAsk questions, 4 options each |
| TC-002 | AC-003 | integration | Start for missing certificate | CertificationNotFoundException |
| TC-003 | AC-009, AC-011 | integration | All correct | Score 100, PASS |
| TC-004 | AC-010, AC-012 | integration | Some unanswered | Unanswered = incorrect; below pass = FAIL |
| TC-005 | AC-011 | integration | Score equals pass mark | PASS (>=) |
| TC-006 | AC-014, AC-015 | integration | Result feedback | Per-question correct flag + explanation + correct option |
| TC-007 | REQ-052 | integration | Missing question at scoring | Counted incorrect, no error |
| TC-008 | AC-004 | integration (MVC) | Start page dropdown | 200, view attempts/start, certificates model |
| TC-009 | AC-002 | integration (MVC) | Blank name | Re-renders attempts/start |
| TC-010 | AC-001, AC-005, AC-007 | integration (MVC) | Valid start | 200, view attempts/exam, attempt model |
| TC-011 | AC-003 | integration (MVC) | Missing certificate | 404 |
| TC-012 | AC-009, AC-011, AC-013 | integration (MVC) | Submit | 200, view attempts/result, result model |
| TC-013 | AC-009 | integration (MVC) | Real-form submit (servedQuestionIds + answers) scores correct | correctCount 1, score 100 (regression guard for the form-binding fix) |
| TC-014 | AC-010 | integration (MVC) | Served question with no answers entry | Counted incorrect |
| TC-015 | AC-006 | integration | 80/20 certificate serves 4 + 1 | Proportional per-section selection for this feature |

## Acceptance Criteria Coverage
| AC | Covered by |
|----|-----------|
| AC-001 | TC-001, TC-010 |
| AC-002 | TC-009 |
| AC-003 | TC-002, TC-011 |
| AC-004 | TC-008 |
| AC-005 | TC-001, TC-010 |
| AC-006 | TC-015 (proportional for this feature); rule also tested by TC-014/TC-040 in Certification Management |
| AC-007 | TC-010 (exam page); exam.html renders radios |
| AC-008 | exam.html submit button; rendered on attempts/exam (TC-010) |
| AC-009 | TC-003, TC-012, TC-013 |
| AC-010 | TC-004, TC-014 |
| AC-011 | TC-003, TC-005, TC-012 |
| AC-012 | TC-004 |
| AC-013 | TC-012 (result model); result.html shows name/score/outcome |
| AC-014 | TC-006 |
| AC-015 | TC-006 |

> Note: AC-007 (radios) and AC-008 (submit button) are realized in `exam.html` and
> exercised via the exam-page render (TC-010); their HTML structure is not asserted
> field-by-field.

## Remediation (post Phase 4 review)
The initial review found a **blocker**: the exam form rendered a hidden
`answers[qid]=""` input before the radios with the same name, so Spring's map
binding kept the empty value and every real submission scored zero. Fixed by:
- Carrying the served-question ids in a dedicated hidden `servedQuestionIds` field;
  the radios are now the ONLY source of `answers[qid]` (no name collision).
- Scoring now iterates the authoritative `servedQuestionIds` (denominator no longer
  depends on what the client answered); a served id absent from `answers` is counted
  incorrect (AC-010).
- Added TC-013 (real-form ordering scores a correct answer as correct — regression
  guard), TC-014 (served-but-unanswered counts incorrect), and TC-015 (proportional
  selection for this feature).
- Re-review follow-ups: scoring now **fails closed** (no `answers.keySet()` fallback,
  so a hand-crafted post cannot shrink the denominator — REQ-050); added TC-016, a
  full 5-question mixed answered/unanswered submission that scores exactly 3/5 = 60
  (directly reproduces the multi-question form layout the original bug lived in).

## Build / Test Result
- `./gradlew build`: PASS
- Tests run: 57 total (41 Certification Management + 16 Certification Attempt), passed: 57, failed: 0
