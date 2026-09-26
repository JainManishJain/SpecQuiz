---
featureId: FEAT-002
slug: certification-attempt
title: Certification Attempt
phase: review
status: approved
sourceArtifact:
  - features/certification-attempt/1-requirements.md
  - features/certification-attempt/2-design.md
  - features/certification-attempt/3-code-and-tests.md
verdict: PASS
---

# Review — Certification Attempt

## Verdict: PASS
This artifact reflects the review AFTER remediation. The initial review found a
**blocker**: the exam form rendered a hidden `answers[qid]=""` input before the
radios of the same name, so Spring's `Map<Long,Long>` binder kept the empty value
and every real submission scored zero. The remediation carries the served-question
ids in a dedicated hidden field, makes the radios the sole source of `answers[qid]`,
and scores against the authoritative served-id set (failing closed if none is
present). An independent re-review confirmed the blocker is resolved with a real
form-ordering regression test that asserts scoring against the actual result model,
the unanswered path (AC-010) holds, and the previously-untested minors are now
covered. No blocker and no unresolved major remain; the gate PASSES.

## Conformance Checklist
| Dimension | Result | Notes (evidence) |
|-----------|--------|------------------|
| requirements-coverage | pass | REQ-001..004 and AC-001..015 implemented and tested; AC-006 proportional selection tested for this feature (TC-015); AC-010 unanswered path tested through the form (TC-014). |
| design-conformance | pass | Stateless flow, reuses `selectQuestionsForAttempt` (REQ-053); server-side scoring (REQ-050); missing-question-as-incorrect (REQ-052). Endpoints match DES-001..003. |
| naming-and-layout | pass | `AttemptController`, `AttemptService`, DTOs under `com.specquiz`; conventions honored. |
| validation-and-error-handling | pass | Blank name re-renders (AC-002); missing certificate → 404 via existing handler (AC-003); missing question handled in-flow (REQ-052). |
| test-adequacy | pass | 16 tests for the feature incl. real-form-ordering regression (TC-013), single- and multi-question (TC-016), unanswered (TC-014), proportional (TC-015). |
| security-and-safety | pass | Correctness recomputed server-side from persisted options; `AttemptView` omits correct flags; scoring fails closed on empty served set; Thymeleaf `th:text` escaping. |
| build-and-test-status | pass | `./gradlew build` green; 57/57 tests pass. |

## Findings
| ID | Severity | Status | Location | Description | Resolution |
|----|----------|--------|----------|-------------|------------|
| F-001 | blocker | RESOLVED | templates/attempts/exam.html, service/AttemptService | Empty hidden `answers[qid]` input won the map binding; every real submission scored zero | Served ids carried in a dedicated hidden field; radios are the only `answers[qid]` source; scorer iterates served ids. Regression guarded by TC-013 and TC-016 |
| F-002 | major | RESOLVED | test/AttemptControllerTest | No test exercised the real browser form ordering | Added TC-013 (single) and TC-016 (5-question mixed) posting servedQuestionIds + answers, asserting the scored model |
| F-003 | minor | RESOLVED | service/AttemptService | Denominator derived from client-submitted answer keys | Scoring iterates authoritative served ids and fails closed on empty (no keySet fallback) |
| F-004 | minor | RESOLVED | service/AttemptService (selection) | AC-006 proportional selection untested for this feature | TC-015 asserts 80/20 → 4+1 via the served AttemptView |
| F-005 | info | ACCEPTED | templates/attempts/exam.html | AC-007 (radios) / AC-008 (submit) HTML structure not asserted field-by-field | Exercised via the exam-page render (TC-010); accepted |

## Acceptance Criteria Verification
| AC | Test case(s) | Status |
|----|--------------|--------|
| AC-001 | TC-001, TC-010 | pass |
| AC-002 | TC-009 | pass |
| AC-003 | TC-002, TC-011 | pass |
| AC-004 | TC-008 | pass |
| AC-005 | TC-001, TC-010 | pass |
| AC-006 | TC-015 | pass |
| AC-007 | TC-010 (render); exam.html | pass (UI-rendered) |
| AC-008 | TC-010 (render); exam.html | pass (UI-rendered) |
| AC-009 | TC-003, TC-012, TC-013, TC-016 | pass |
| AC-010 | TC-004, TC-014, TC-016 | pass |
| AC-011 | TC-003, TC-005, TC-016 | pass |
| AC-012 | TC-004 | pass |
| AC-013 | TC-012 | pass |
| AC-014 | TC-006 | pass |
| AC-015 | TC-006 | pass |

---

## Review history

### Initial review — verdict FAIL
Found one blocker (exam-form binding scored every real submission zero) plus a major
(no test reproduced the real form ordering) and two minors (denominator from client
keys; AC-006 untested for this feature). All resolved in remediation.

Independent review records:
- Initial: `semantic-review/2026-09-26-230218-pr-certification-attempt.md`
- Re-review (this PASS): `semantic-review/2026-09-26-231420-pr-certification-attempt.md`
