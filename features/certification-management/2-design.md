---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: design
status: approved
sourceArtifact: features/certification-management/1-requirements.md
---

# Technical Design — Certification Management

## 1. Overview
This design realizes the Certification Management FSD as a layered Spring Boot
feature with a server-rendered Thymeleaf UI. A **Certificate** owns one or more
weighted **Sections**, and each Section owns a pool of single-answer **Questions**
(four **Options**, exactly one correct, plus a one-line explanation). Validation is
centralized in the service layer and enforced server-side (REQ-050): section
weights must be multiples of 20 summing to 100, a certificate needs at least one
section and at least five questions total, the configured exam length must be at
least five and no larger than the pool, and questions must have exactly four
options with exactly one correct answer. A `CommandLineRunner`-based bootstrapper
seeds at least two valid sample certifications on startup (REQ-009). The persisted
model and the proportional question-selection helper are designed so the
Certification Attempt feature (FEAT-002) can consume them directly.

Key decisions: use a single aggregate (Certificate → Section → Question → Option)
with cascading persistence; validate at save time and aggregate all violations
into one response (AC-021); keep the correct-answer flag on Options so a question
can be validated for "exactly one correct".

## 2. Architecture
Standard layered flow. Thymeleaf controllers render authoring pages and handle form
posts; a REST controller exposes question CRUD used by the page's inline editing; a
service holds all business rules and validation; repositories persist the aggregate
via Spring Data JPA to H2.

### Layers
| Layer | Responsibility |
|-------|----------------|
| View (Thymeleaf) | Server-rendered authoring pages; shows running weight total (REQ-051) |
| Controller | HTTP endpoints (MVC for pages, REST for question CRUD), request/response mapping |
| Service | Business logic, structural validation, proportional selection, transactions |
| Repository | Spring Data JPA persistence of the Certificate aggregate |
| Database | H2 in-memory (dev); seeded on startup |

### Design Decisions (ADRs)
| Decision | Rationale | Alternatives considered |
|----------|-----------|-------------------------|
| Single aggregate Certificate→Section→Question→Option with cascade | Keeps authoring transactional and validation holistic; matches how a certificate is edited as a unit | Separate aggregates per entity with manual wiring (more endpoints, weaker invariants) |
| Correct answer stored as a boolean flag on Option | Lets the service validate "exactly one correct" and lets Attempt compare selected option id | Store correct index on Question (harder to render options generically) |
| Validation aggregates all violations into one error response | Satisfies AC-021 and gives authors a complete fix list | Fail-fast on first violation (poorer UX) |
| Proportional selection helper in service now, consumed by Attempt later | Avoids duplicating the weighting rule across features | Implement selection only in Attempt (rule duplicated) |
| Bootstrap via CommandLineRunner | Simple, runs after context init, uses the same service validation | SQL import.sql (bypasses service validation, can seed invalid data) |

## 3. Components
| Class | Stereotype | Package | Responsibility | Depends on | Implements |
|-------|-----------|---------|----------------|------------|------------|
| `CertificationController` | Controller | com.specquiz.controller | Thymeleaf MVC: list/view/create certificate, add section, set exam length & pass % | `CertificationService` | DES-001, DES-002, DES-008, DES-009, DES-010 |
| `QuestionController` | Controller | com.specquiz.controller | REST CRUD for questions (add/edit/delete) used by the authoring page | `CertificationService` | DES-003, DES-004, DES-005 |
| `CertificationService` | Service | com.specquiz.service | All business rules: create cert, manage sections, question CRUD, validity checks, proportional selection | `CertificationRepository`, `QuestionRepository`, `CertificationValidator` | DES-001..DES-011 |
| `CertificationValidator` | Util | com.specquiz.util | Pure validation of a certificate aggregate; returns aggregated violations | — | DES-006, DES-011 |
| `CertificationRepository` | Repository | com.specquiz.repository | Persist/read Certificate aggregate | — | DES-001, DES-010 |
| `QuestionRepository` | Repository | com.specquiz.repository | Persist/read Question | — | DES-003, DES-004, DES-005 |
| `Certificate` | Entity | com.specquiz.entity | Aggregate root: title, passPercentage, questionsToAsk, sections | — | DES-001, DES-008, DES-009 |
| `Section` | Entity | com.specquiz.entity | Named weighted section; owns questions | — | DES-002 |
| `Question` | Entity | com.specquiz.entity | Question text, explanation, 4 options | — | DES-003 |
| `Option` | Entity | com.specquiz.entity | Option text + correct flag | — | DES-003 |
| `CertificationRequest` | DTO | com.specquiz.dto | Inbound certificate form (title, passPercentage, questionsToAsk) | — | DES-001 |
| `SectionRequest` | DTO | com.specquiz.dto | Inbound section (name, weight) | — | DES-002 |
| `QuestionRequest` | DTO | com.specquiz.dto | Inbound question (text, explanation, 4 options, correct index) | — | DES-003, DES-004 |
| `CertificationResponse` | DTO | com.specquiz.dto | Outbound certificate view (sections, counts, config) | — | DES-010 |
| `QuestionResponse` | DTO | com.specquiz.dto | Outbound question view | — | DES-003 |
| `CertificationDataLoader` | Config | com.specquiz.config | CommandLineRunner seeding sample certifications on startup | `CertificationService` | DES-007 |
| `CertificationNotFoundException` | Exception | com.specquiz.exception | Thrown when a certificate/question id is absent | — | DES-004, DES-005 |
| `CertificationValidationException` | Exception | com.specquiz.exception | Carries aggregated validation violations | — | DES-006, DES-011 |
| `GlobalExceptionHandler` | ExceptionHandler | com.specquiz.exception | Maps exceptions to structured error responses / status codes | — | DES-011 |

## 4. Data Model

### Certificate (table: `certificate`)
| Field | Column | Type | Nullable | Constraints | Notes |
|-------|--------|------|----------|-------------|-------|
| id | id | Long | no | PK, generated | |
| title | title | String | no | not blank, unique | AC-001, AC-002 |
| passPercentage | pass_percentage | Integer | no | 1..100 | AC-003, AC-022 |
| questionsToAsk | questions_to_ask | Integer | no | >= 5, <= pool size | AC-017, AC-018 |
| sections | — | List<Section> | no | one-to-many, cascade ALL | AC-004 |

- **Primary key:** id
- **Indexes:** `uk_certificate_title` on (title), unique: yes
- **Relationships:** one-to-many → Section (mappedBy `certificate`, cascade ALL, orphanRemoval true, fetch LAZY)

### Section (table: `section`)
| Field | Column | Type | Nullable | Constraints | Notes |
|-------|--------|------|----------|-------------|-------|
| id | id | Long | no | PK, generated | |
| name | name | String | no | not blank | AC-004 |
| weight | weight | Integer | no | multiple of 20, 20..100 | AC-005, AC-006 |
| certificate | certificate_id | Certificate | no | FK | |
| questions | — | List<Question> | no | one-to-many, cascade ALL | |

- **Primary key:** id
- **Indexes:** `idx_section_certificate` on (certificate_id), unique: no
- **Relationships:** many-to-one → Certificate (fetch LAZY); one-to-many → Question (mappedBy `section`, cascade ALL, orphanRemoval true, fetch LAZY)

### Question (table: `question`)
| Field | Column | Type | Nullable | Constraints | Notes |
|-------|--------|------|----------|-------------|-------|
| id | id | Long | no | PK, generated | |
| text | text | String | no | not blank | AC-008, AC-011 |
| explanation | explanation | String | no | not blank | AC-011 |
| section | section_id | Section | no | FK | |
| options | — | List<Option> | no | exactly 4, cascade ALL | AC-009, AC-010 |

- **Primary key:** id
- **Indexes:** `idx_question_section` on (section_id), unique: no
- **Relationships:** many-to-one → Section (fetch LAZY); one-to-many → Option (mappedBy `question`, cascade ALL, orphanRemoval true, fetch EAGER)

### Option (table: `answer_option`)
| Field | Column | Type | Nullable | Constraints | Notes |
|-------|--------|------|----------|-------------|-------|
| id | id | Long | no | PK, generated | |
| text | text | String | no | not blank | |
| correct | correct | boolean | no | exactly one true per question | AC-010 |
| question | question_id | Question | no | FK | |

- **Primary key:** id
- **Indexes:** `idx_option_question` on (question_id), unique: no
- **Relationships:** many-to-one → Question (fetch LAZY)

## 5. API Contracts

### DES-001 — Create certificate (page form)
- **Method / Path:** `POST /certifications`
- **Path params:** none
- **Query params:** none
- **Request body:** `CertificationRequest` (form-encoded)
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 302 | redirect to `/certifications/{id}` | Created |
  | 200 | certifications/form (with errors) | Validation failed, re-render form |
- **Satisfies:** REQ-001 (AC-001, AC-002, AC-003)

### DES-002 — Add section to certificate
- **Method / Path:** `POST /certifications/{id}/sections`
- **Path params:** id (Long)
- **Request body:** `SectionRequest` (name, weight)
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 302 | redirect to `/certifications/{id}` | Section added |
  | 200 | certifications/view (with errors) | Weight not multiple of 20, re-render |
  | 404 | ErrorResponse | Certificate not found |
- **Satisfies:** REQ-002 (AC-004, AC-005)

### DES-003 — Add question
- **Method / Path:** `POST /api/sections/{sectionId}/questions`
- **Path params:** sectionId (Long)
- **Request body:** `QuestionRequest`
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 201 | `QuestionResponse` | Created |
  | 400 | ErrorResponse | Not 4 options / not exactly 1 correct / empty text or explanation |
  | 404 | ErrorResponse | Section not found |
- **Satisfies:** REQ-003 (AC-008, AC-009, AC-010, AC-011)

### DES-004 — Edit question
- **Method / Path:** `PUT /api/questions/{id}`
- **Path params:** id (Long)
- **Request body:** `QuestionRequest`
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 200 | `QuestionResponse` | Updated |
  | 400 | ErrorResponse | Edit would make the question invalid; previous version retained |
  | 404 | ErrorResponse | Question not found |
- **Satisfies:** REQ-004 (AC-012, AC-013, AC-014)

### DES-005 — Delete question
- **Method / Path:** `DELETE /api/questions/{id}`
- **Path params:** id (Long)
- **Request body:** none
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 204 | none | Deleted |
  | 404 | ErrorResponse | Question not found |
- **Satisfies:** REQ-005 (AC-015, AC-016)

### DES-008 — Set exam length
- **Method / Path:** `POST /certifications/{id}/exam-length`
- **Path params:** id (Long)
- **Query params:**
  | Name | Type | Required | Default |
  |------|------|----------|---------|
  | questionsToAsk | Integer | true | — |
- **Request body:** none
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 302 | redirect to `/certifications/{id}` | Saved |
  | 200 | certifications/view (with errors) | < 5 or > pool size |
  | 404 | ErrorResponse | Certificate not found |
- **Satisfies:** REQ-006 (AC-017, AC-018)

### DES-009 — Set pass percentage
- **Method / Path:** `POST /certifications/{id}/pass-percentage`
- **Path params:** id (Long)
- **Query params:**
  | Name | Type | Required | Default |
  |------|------|----------|---------|
  | passPercentage | Integer | true | — |
- **Request body:** none
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 302 | redirect to `/certifications/{id}` | Saved |
  | 200 | certifications/view (with errors) | Not 1..100 |
- **Satisfies:** REQ-008 (AC-022)

### DES-010 — List and view certificates
- **Method / Path:** `GET /certifications` and `GET /certifications/{id}`
- **Path params:** id (Long, on the view route)
- **Request body:** none
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 200 | certifications/list or certifications/view | Rendered page |
  | 404 | ErrorResponse | Certificate not found (view route) |
- **Satisfies:** REQ-010 (AC-025, AC-026)

## 6. DTOs

### `CertificationRequest` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| title | String | @NotBlank | AC-001, AC-002 |
| passPercentage | Integer | @NotNull, @Min(1), @Max(100) | AC-003 |
| questionsToAsk | Integer | @NotNull, @Min(5) | AC-017; upper bound checked in service vs pool (AC-018) |

### `SectionRequest` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| name | String | @NotBlank | AC-004 |
| weight | Integer | @NotNull, @Min(20), @Max(100) | multiple-of-20 checked in service/validator (AC-005) |

### `QuestionRequest` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| text | String | @NotBlank | AC-008, AC-011 |
| explanation | String | @NotBlank | AC-011 |
| options | List<String> | @Size(min=4, max=4), each @NotBlank | AC-009 |
| correctOptionIndex | Integer | @NotNull, @Min(0), @Max(3) | maps to exactly one correct option (AC-010) |

### `CertificationResponse` (response)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | |
| title | String | |
| passPercentage | Integer | |
| questionsToAsk | Integer | |
| totalQuestions | Integer | pool size across sections |
| sections | List<SectionSummary> | name, weight, questionCount |

### `QuestionResponse` (response)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | |
| text | String | |
| explanation | String | |
| options | List<{id,text}> | correct flag omitted from authoring echo except for the author view |
| correctOptionId | Long | included in author-facing responses |

## 7. Business Logic

### DES-006 — Validate certificate aggregate (aggregated violations)
Steps:
1. Collect violations into a list (do not fail fast).
2. Title present and non-blank; else add violation (AC-002).
3. passPercentage in 1..100; else add violation (AC-003).
4. At least one section; else add violation (AC-007).
5. Each section weight is a positive multiple of 20; else add violation (AC-005).
6. Sum of section weights == 100; else add violation stating the current total (AC-006).
7. Total questions across sections >= 5; else add violation (AC-020).
8. questionsToAsk >= 5 and <= total questions; else add violation (AC-017, AC-018).
9. Each question has exactly 4 options and exactly 1 correct; else add violation (AC-009, AC-010).
10. If any violations, throw `CertificationValidationException` carrying all of them (AC-021).
- **Satisfies:** REQ-002, REQ-006, REQ-007 (AC-005, AC-006, AC-007, AC-017, AC-018, AC-020, AC-021)

### DES-011 — Validate a single question
Steps:
1. text and explanation non-blank (AC-011).
2. exactly 4 options, each non-blank (AC-009).
3. exactly one option flagged correct (AC-010).
4. On edit, if invalid, reject and leave stored version untouched (AC-013).
- **Satisfies:** REQ-003, REQ-004 (AC-009, AC-010, AC-011, AC-013)

### DES-007 — Bootstrap sample certifications
Steps:
1. On startup, build >= 2 sample certificates in memory (e.g. "AWS Certified Cloud Practitioner", "AWS Certified AI Practitioner").
2. Each has sections whose weights are multiples of 20 summing to 100 and >= 5 questions total.
3. Persist each through `CertificationService.create(...)` so the same validation applies (AC-024).
4. Skip seeding if certificates already exist (idempotent within a run).
- **Satisfies:** REQ-009 (AC-023, AC-024)

### DES-012 — Proportional question selection (for Attempt, defined here)
Steps:
1. Given questionsToAsk N and sections with weights summing to 100, allocate per section = round(N * weight/100).
2. Adjust rounding so the allocations sum to exactly N.
3. Randomly pick that many questions from each section's pool.
- **Satisfies:** REQ-006 (AC-019)

## 8. Sequence Flows

### Create certificate flow
1. Author submits the create form (`POST /certifications`).
2. Controller binds `CertificationRequest`; bean validation runs (title, passPercentage, questionsToAsk basic bounds).
3. Service creates the Certificate (sections/questions added subsequently) and persists.
4. On success, redirect to the certificate view; on validation failure, re-render the form with messages.

### Add question flow (inline REST)
1. Author submits a question via the page's inline form (`POST /api/sections/{sectionId}/questions`).
2. Controller binds `QuestionRequest`; bean validation runs (4 options, indices).
3. Service loads the section (404 if missing), runs `DES-011` question validation.
4. Service persists the question; controller returns 201 `QuestionResponse`; page updates the list.

### Save/validate certificate flow
1. Author triggers a save/validate action.
2. Service runs `DES-006` aggregate validation.
3. If violations exist, throw `CertificationValidationException`; handler returns them all (AC-021).
4. Otherwise persist and confirm.

## 9. Validation Rules
| Field | Rule | HTTP status | Satisfies |
|-------|------|-------------|-----------|
| title | non-blank | 400 | AC-002 |
| passPercentage | integer 1..100 | 400 | AC-003 |
| section.weight | positive multiple of 20 | 400 | AC-005 |
| sections | weights sum to exactly 100 | 400 | AC-006 |
| sections | at least one section | 400 | AC-007 |
| question.options | exactly 4, each non-blank | 400 | AC-009 |
| question.correct | exactly one correct | 400 | AC-010 |
| question.text/explanation | non-blank | 400 | AC-011 |
| questionsToAsk | >= 5 | 400 | AC-017 |
| questionsToAsk | <= total questions | 400 | AC-018 |
| certificate | >= 5 questions total | 400 | AC-020 |

## 10. Error Handling
**Standard error body:** `{ timestamp, status, error, message, path, violations[] }`
(`violations[]` carries field + message pairs to satisfy AC-021.)

| Condition | HTTP status | Exception |
|-----------|-------------|-----------|
| Certificate or question id not found | 404 | `CertificationNotFoundException` |
| Aggregated structural validation failure | 400 | `CertificationValidationException` |
| Bean validation failure on a DTO | 400 | `MethodArgumentNotValidException` |
| Malformed request body | 400 | `HttpMessageNotReadableException` |

## 11. Security
- All structural and content validation is performed server-side; client-side checks are convenience only (REQ-050).
- Inputs bound through DTOs with bean validation to reduce injection risk; JPA uses parameterized queries.
- No secrets or PII involved; question/explanation content is plain text (no HTML rendering of user input to avoid stored XSS — escape on output in Thymeleaf, which is default).
- Auth required: no (out of scope this release).

## 12. Configuration
| Key | Purpose | Default |
|-----|---------|---------|
| spring.thymeleaf (starter dependency) | Enable server-rendered UI (REQ-052) | added via spring-boot-starter-thymeleaf |
| spring.jpa.hibernate.ddl-auto | Create schema for the aggregate on startup | update (existing) |
| app.bootstrap.enabled | Toggle sample data seeding | true |

## 13. Observability
- Logging: log certificate creation, question CRUD (id + action) at INFO; log validation failures with the violation list at DEBUG.
- Metrics: count of certificates and questions created (optional, via Micrometer if added later).

## 14. Requirements Coverage
| Requirement / AC | Design element(s) |
|------------------|-------------------|
| REQ-001 / AC-001, AC-002, AC-003 | DES-001, DES-006 |
| REQ-002 / AC-004, AC-005, AC-006, AC-007 | DES-002, DES-006 |
| REQ-003 / AC-008, AC-009, AC-010, AC-011 | DES-003, DES-011 |
| REQ-004 / AC-012, AC-013, AC-014 | DES-004, DES-011 |
| REQ-005 / AC-015, AC-016 | DES-005 |
| REQ-006 / AC-017, AC-018, AC-019 | DES-008, DES-006, DES-012 |
| REQ-007 / AC-020, AC-021 | DES-006, DES-011 |
| REQ-008 / AC-022 | DES-009 |
| REQ-009 / AC-023, AC-024 | DES-007 |
| REQ-010 / AC-025, AC-026 | DES-010 |
