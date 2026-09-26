---
featureId: FEAT-002
slug: certification-attempt
title: Certification Attempt
phase: design
status: approved
sourceArtifact: features/certification-attempt/1-requirements.md
---

# Technical Design — Certification Attempt

## 1. Overview
Certification Attempt is a stateless exam-taking flow layered on the existing
Certification Management model. There is no persisted attempt entity (persisting
attempt history is out of scope): the candidate starts an attempt, the server
selects `questionsToAsk` questions proportionally to section weight (reusing the
existing selection rule), renders them on one page, and scores the submitted
answers server-side. The result — score, PASS/FAIL versus the certificate's pass
percentage, and per-question correct/incorrect with explanations — is rendered
immediately. To keep the exam page self-contained and avoid server-side session
state, the served question ids (and their correct option ids) are carried in the
form and re-validated at scoring time against the live data.

Key decisions: reuse `CertificationValidator.selectProportionally` via a new
`CertificationService.selectQuestionsForAttempt` method (REQ-053); make the flow
stateless by embedding the served question ids in the submitted form; score
server-side (REQ-050) and treat any unanswered or now-missing question as incorrect
(AC-010, REQ-052).

## 2. Architecture
Thymeleaf MVC flow: a start page, an exam page, and a result page. An
`AttemptService` orchestrates selection and scoring, delegating question selection
to `CertificationService` (which owns the certificate aggregate and the proportional
rule). No new persistence.

### Layers
| Layer | Responsibility |
|-------|----------------|
| View (Thymeleaf) | Start form, exam page (radio options), result page |
| Controller | `AttemptController` — start/exam/submit endpoints, request/response mapping |
| Service | `AttemptService` — select questions, score submission, build result |
| Reuse | `CertificationService` — certificate lookup + proportional selection |
| Database | H2 (read-only for attempts; no attempt tables) |

### Design Decisions (ADRs)
| Decision | Rationale | Alternatives considered |
|----------|-----------|-------------------------|
| Stateless attempt (no persisted entity) | Persistence of attempts is out of scope; keeps the feature small | Persist an Attempt aggregate (unneeded complexity this release) |
| Carry served question ids in the submitted form | Avoids server session state and lets scoring re-verify against live data | HttpSession storage (statefulness, harder to test) |
| Score server-side, recompute correctness from persisted options | Satisfies REQ-050; client cannot influence the outcome | Trust client-submitted correctness (insecure) |
| Reuse selectProportionally via CertificationService | Single source of truth for the weighting rule (REQ-053) | Duplicate selection logic in AttemptService |

## 3. Components
| Class | Stereotype | Package | Responsibility | Depends on | Implements |
|-------|-----------|---------|----------------|------------|------------|
| `AttemptController` | Controller | com.specquiz.controller | Thymeleaf MVC: start form, start attempt, submit/score | `AttemptService`, `CertificationService` | DES-001, DES-002, DES-003 |
| `AttemptService` | Service | com.specquiz.service | Build an attempt (select questions) and score a submission | `CertificationService` | DES-002, DES-004, DES-005 |
| `CertificationService` (modified) | Service | com.specquiz.service | Add `selectQuestionsForAttempt(id)` reusing the validator's proportional selection | `CertificationValidator` | DES-004 |
| `StartAttemptRequest` | DTO | com.specquiz.dto | Inbound start form (candidateName, certificateId) | — | DES-001 |
| `AttemptView` | DTO | com.specquiz.dto | Data for the exam page (certificate title, candidate name, served questions with options) | — | DES-002 |
| `AttemptSubmission` | DTO | com.specquiz.dto | Inbound submitted answers (candidateName, certificateId, questionIds, selectedOptionId per question) | — | DES-003 |
| `AttemptResult` | DTO | com.specquiz.dto | Outbound result (name, score, outcome, per-question feedback) | — | DES-005 |

## 4. Data Model
No new entities or tables. Certification Attempt reads the existing
`Certificate → Section → Question → Option` aggregate (FEAT-001). Attempts are
transient and are not persisted.

## 5. API Contracts

### DES-001 — Start form
- **Method / Path:** `GET /attempts/new`
- **Request body:** none
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 200 | attempts/start | Form with candidate-name field and a certification dropdown (AC-004) |
- **Satisfies:** REQ-001 (AC-004)

### DES-002 — Start attempt (serve questions)
- **Method / Path:** `POST /attempts`
- **Request body:** `StartAttemptRequest` (candidateName, certificateId)
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 200 | attempts/exam | Exam page with the served questions (AC-005..008) |
  | 200 | attempts/start (with error) | Blank candidate name (AC-002) |
  | 404 | ErrorResponse | Certificate not found (AC-003) |
- **Satisfies:** REQ-001, REQ-002 (AC-001, AC-002, AC-003, AC-005, AC-006, AC-007, AC-008)

### DES-003 — Submit and score
- **Method / Path:** `POST /attempts/submit`
- **Request body:** `AttemptSubmission` (candidateName, certificateId, per-question selected option ids)
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 200 | attempts/result | Result page with score, outcome, per-question feedback (AC-009..015) |
  | 404 | ErrorResponse | Certificate not found |
- **Satisfies:** REQ-003, REQ-004 (AC-009..015)

## 6. DTOs

### `StartAttemptRequest` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| candidateName | String | @NotBlank | AC-002 |
| certificateId | Long | @NotNull | AC-003 |

### `AttemptView` (response)
| Field | Type | Notes |
|-------|------|-------|
| candidateName | String | echoed into the exam form |
| certificateId | Long | echoed into the exam form |
| certificateTitle | String | shown as the exam heading |
| questions | List<QuestionView> | served questions |

`QuestionView`: `{ questionId: Long, text: String, options: List<{ optionId: Long, text: String }> }`
(correct flags are NOT sent to the client; correctness is recomputed at scoring.)

### `AttemptSubmission` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| candidateName | String | @NotBlank | echoed to result |
| certificateId | Long | @NotNull | |
| answers | Map<Long,Long> | — | questionId -> selectedOptionId; a missing entry means unanswered (AC-010) |

### `AttemptResult` (response)
| Field | Type | Notes |
|-------|------|-------|
| candidateName | String | AC-013 |
| totalQuestions | int | number served |
| correctCount | int | number correct |
| scorePercentage | int | round(correct/total*100) (AC-009) |
| passPercentage | int | from the certificate |
| passed | boolean | scorePercentage >= passPercentage (AC-011, AC-012) |
| items | List<ResultItem> | per-question feedback |

`ResultItem`: `{ questionText, options:[{text, correct, selected}], correct: boolean, explanation: String }` (AC-014, AC-015)

## 7. Business Logic

### DES-004 — Select questions for an attempt
Steps:
1. Load the certificate via `CertificationService.getCertificate(id)` (404 if missing, AC-003).
2. Delegate to `CertificationService.selectQuestionsForAttempt(id)`, which calls
   `CertificationValidator.selectProportionally` with a `Random` (AC-005, AC-006, REQ-053).
3. Map the selected questions to `QuestionView` (options without correct flags).
- **Satisfies:** REQ-002 (AC-005, AC-006)

### DES-005 — Score a submission
Steps:
1. Load the certificate; build a lookup of questionId -> Question from its sections.
2. For each served questionId in the submission:
   a. Find the question; if it no longer exists, count as incorrect (REQ-052).
   b. Get the candidate's selected optionId (may be absent = unanswered, AC-010).
   c. Determine the question's correct option id from persisted data (server-side, REQ-050).
   d. Mark correct iff selected == correct option id.
   e. Build a `ResultItem` with each option flagged correct/selected, plus the explanation (AC-014, AC-015).
3. score = round(correctCount / totalServed * 100) (AC-009).
4. passed = score >= certificate.passPercentage (AC-011, AC-012).
5. Assemble `AttemptResult` with name, score, outcome, items (AC-013).
- **Satisfies:** REQ-003, REQ-004 (AC-009..015)

## 8. Sequence Flows

### Start attempt flow
1. Candidate opens `GET /attempts/new`; controller loads certificate titles for the dropdown.
2. Candidate submits name + certificateId (`POST /attempts`).
3. Bean validation checks the name (AC-002); service loads the certificate (404 if missing).
4. Service selects questions proportionally and returns an `AttemptView`.
5. Controller renders the exam page; the form embeds candidateName, certificateId, and the served question ids.

### Submit/score flow
1. Candidate selects radio options and submits (`POST /attempts/submit`).
2. Controller binds `AttemptSubmission` (question ids + selected option ids).
3. Service scores server-side, treating unanswered/missing as incorrect.
4. Controller renders the result page (score, PASS/FAIL, per-question feedback).

## 9. Validation Rules
| Field | Rule | HTTP status | Satisfies |
|-------|------|-------------|-----------|
| candidateName | non-blank | 400 (re-render 200 with error) | AC-002 |
| certificateId | exists | 404 | AC-003 |

## 10. Error Handling
Reuses the existing `GlobalExceptionHandler`. `CertificationNotFoundException`
(missing certificate) maps to 404 with the standard error body. Missing questions at
scoring time are handled in-flow as incorrect (REQ-052), not as errors.

| Condition | HTTP status | Exception |
|-----------|-------------|-----------|
| Certificate not found | 404 | `CertificationNotFoundException` |
| Blank candidate name (form) | 200 re-render with error | (binding result) |

## 11. Security
- Scoring and pass/fail are computed server-side from persisted data; the client
  never sends correctness and cannot influence the outcome (REQ-050).
- Candidate name is rendered via Thymeleaf `th:text` (escaped) — no stored XSS.
- Read-only access to existing data; no new write surface. Auth out of scope.

## 12. Configuration
No new configuration keys or dependencies. Reuses Thymeleaf and the existing model.

## 13. Observability
- Logging: log attempt start (certificate id) and submission score at INFO.

## 14. Requirements Coverage
| Requirement / AC | Design element(s) |
|------------------|-------------------|
| REQ-001 / AC-001, AC-002, AC-003, AC-004 | DES-001, DES-002 |
| REQ-002 / AC-005, AC-006, AC-007, AC-008 | DES-002, DES-004 |
| REQ-003 / AC-009, AC-010, AC-011, AC-012 | DES-003, DES-005 |
| REQ-004 / AC-013, AC-014, AC-015 | DES-003, DES-005 |
