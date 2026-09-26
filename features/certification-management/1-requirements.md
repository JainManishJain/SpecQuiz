---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: requirements
status: approved
sourceArtifact: features/certification-management/requirements/prd.md
---

# Requirements (FSD) — Certification Management

## Overview
Certification Management is the authoring side of SpecQuiz. An exam author creates
a certification exam that mirrors a real one: a titled certificate with a pass
percentage, one or more weighted sections (each weight a multiple of 20, summing to
100 across the certificate), and a pool of single-answer multiple-choice questions
(four options, exactly one correct, a one-line explanation). Authors have full CRUD
over questions, the author sets how many questions an attempt serves (at least
five, drawn proportionally to section weights), and a set of realistic sample
certifications is bootstrapped on startup. The authored content is consumed by the
Certification Attempt feature. This document defines the functional behavior in
testable EARS acceptance criteria.

## Functional Requirements

### REQ-001 — Create a certification
**Traces:** PRD-001
**User story:** As an exam author, I want to create a certificate with a title and pass percentage, so that I can model a real certification exam.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-001 | event | WHEN an author submits a new certificate with a non-empty title and a valid pass percentage THE SYSTEM SHALL create the certificate and confirm it was created. |
| AC-002 | conditional | IF an author submits a certificate with an empty or missing title THEN THE SYSTEM SHALL reject the request with a validation error and SHALL NOT create the certificate. |
| AC-003 | conditional | IF an author submits a pass percentage that is not an integer between 1 and 100 THEN THE SYSTEM SHALL reject the request with a validation error. |

### REQ-002 — Manage weighted sections
**Traces:** PRD-002
**User story:** As an exam author, I want to add weighted sections to a certificate, choosing an existing section name or a new one, so that the exam mirrors the real exam's domains.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-004 | event | WHEN an author adds a section to a certificate THE SYSTEM SHALL let the author either select an existing section name or provide a new one. |
| AC-005 | conditional | IF an author assigns a section weight that is not a positive multiple of 20 (20, 40, 60, 80, 100) THEN THE SYSTEM SHALL reject the section with a validation error. |
| AC-006 | conditional | IF the sum of all section weights for a certificate is not exactly 100 THEN THE SYSTEM SHALL reject saving the certificate with a validation error identifying the current total. |
| AC-007 | conditional | IF an author saves a certificate with no sections THEN THE SYSTEM SHALL reject it with a validation error requiring at least one section. |

### REQ-003 — Author a question
**Traces:** PRD-003
**User story:** As an exam author, I want to add a question with four options, one correct answer, and a one-line explanation, so that candidates can be tested.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-008 | event | WHEN an author adds a question to a section with non-empty text, exactly four options, exactly one option marked correct, and a one-line explanation THE SYSTEM SHALL add the question to that section's pool. |
| AC-009 | conditional | IF a submitted question does not have exactly four options THEN THE SYSTEM SHALL reject it with a validation error. |
| AC-010 | conditional | IF a submitted question has zero or more than one option marked correct THEN THE SYSTEM SHALL reject it with a validation error. |
| AC-011 | conditional | IF a submitted question has empty question text or a missing explanation THEN THE SYSTEM SHALL reject it with a validation error. |

### REQ-004 — Edit a question
**Traces:** PRD-004
**User story:** As an exam author, I want to edit an existing question, so that I can keep the pool accurate.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-012 | event | WHEN an author saves edits to an existing question that still satisfy the question validity rules THE SYSTEM SHALL persist the updated question and reflect the change immediately. |
| AC-013 | conditional | IF an author edits a question into an invalid state (not four options, not exactly one correct, empty text or explanation) THEN THE SYSTEM SHALL reject the edit with a validation error and SHALL retain the previous valid version. |
| AC-014 | conditional | IF an author attempts to edit a question that does not exist THEN THE SYSTEM SHALL respond with a not-found error. |

### REQ-005 — Delete a question
**Traces:** PRD-004
**User story:** As an exam author, I want to delete a question, so that I can remove outdated content.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-015 | event | WHEN an author deletes an existing question THE SYSTEM SHALL remove it from the pool and confirm the deletion. |
| AC-016 | conditional | IF an author attempts to delete a question that does not exist THEN THE SYSTEM SHALL respond with a not-found error. |

### REQ-006 — Configure exam length
**Traces:** PRD-005
**User story:** As an exam author, I want to set how many questions an attempt serves, so that the practice exam matches the intended length.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-017 | conditional | IF an author sets the number of questions to ask below 5 THEN THE SYSTEM SHALL reject it with a validation error. |
| AC-018 | conditional | IF the configured number of questions to ask exceeds the total number of questions in the pool THEN THE SYSTEM SHALL reject saving with a validation error. |
| AC-019 | ubiquitous | THE SYSTEM SHALL select the questions served for an attempt proportionally to each section's weight. |

### REQ-007 — Enforce certificate validity on save
**Traces:** PRD-006
**User story:** As a training program owner, I want invalid certificates to be rejected, so that candidates only ever see well-formed exams.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-020 | conditional | IF a certificate has fewer than 5 questions in total across its sections THEN THE SYSTEM SHALL reject saving it with a validation error. |
| AC-021 | conditional | IF a certificate violates any single validity rule (title, pass percentage, section weights, minimum sections, minimum questions, exam length) THEN THE SYSTEM SHALL report every violated rule in the validation response. |

### REQ-008 — Set pass percentage
**Traces:** PRD-008
**User story:** As an exam author, I want to set a pass percentage, so that attempt results can be judged pass or fail.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-022 | event | WHEN an author sets a pass percentage between 1 and 100 THE SYSTEM SHALL store it against the certificate for use when scoring attempts. |

### REQ-009 — Bootstrap sample certifications on startup
**Traces:** PRD-007
**User story:** As a training program owner, I want realistic sample certifications available immediately, so that the tool is usable without manual data entry.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-023 | event | WHEN the application starts THE SYSTEM SHALL load at least two sample certifications, each with at least one section and at least five questions, with section weights summing to 100. |
| AC-024 | ubiquitous | THE SYSTEM SHALL ensure every bootstrapped certification satisfies all certificate validity rules. |

### REQ-010 — View certifications and their questions
**Traces:** PRD-001, PRD-004
**User story:** As an exam author, I want to view existing certificates and their questions, so that I can review and maintain them.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-025 | event | WHEN an author opens the Certification Management page THE SYSTEM SHALL display the list of existing certificates with their sections and question counts. |
| AC-026 | event | WHEN an author opens a specific certificate THE SYSTEM SHALL display its sections, weights, pass percentage, exam length, and the questions in each section. |

## Non-Functional Requirements
| ID | Category | Criterion (EARS) |
|----|----------|------------------|
| REQ-050 | validation | THE SYSTEM SHALL perform all structural validation server-side and SHALL NOT rely on client-side checks alone. |
| REQ-051 | usability | WHEN section weights are being entered THE SYSTEM SHALL display the running total so the author can see progress toward 100. |
| REQ-052 | usability | THE SYSTEM SHALL present a server-rendered Thymeleaf UI for all authoring actions. |
| REQ-053 | reliability | THE SYSTEM SHALL return a clear, structured error response (status, message, offending field) for every rejected authoring request. |
| REQ-054 | performance | WHEN the Certification Management page is opened THE SYSTEM SHALL render the certificate list within 2 seconds under normal local development conditions. |
| REQ-055 | maintainability | THE SYSTEM SHALL organize code by stereotype with feature-prefixed class names under the com.specquiz base package. |

## Out of Scope
- Multi-select questions or question types other than single-answer multiple choice.
- Authentication, authorization, and per-author ownership of certificates.
- Rich media in questions (images, code blocks, formatting).
- Persistence of authored data across application restarts (in-memory H2 this release).
- The exam-taking and scoring experience (covered by the Certification Attempt feature).
