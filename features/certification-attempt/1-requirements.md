---
featureId: FEAT-002
slug: certification-attempt
title: Certification Attempt
phase: requirements
status: approved
sourceArtifact: null
---

# Requirements (FSD) — Certification Attempt

## Overview
Certification Attempt is the exam-taking side of SpecQuiz. A candidate enters their
name, selects a certification from a dropdown, and starts an attempt. The system
serves the configured number of questions (drawn proportionally to section weight,
reusing the Certification Management selection rule) on a single page, each with its
four options as radio buttons. The candidate answers and submits. On submission the
system scores the attempt, compares the score to the certificate's pass percentage
to decide pass/fail, and shows a result that highlights each question as correct or
incorrect alongside its one-line explanation. Unanswered questions count as
incorrect. This document defines the behavior in testable EARS acceptance criteria.

## Functional Requirements

### REQ-001 — Start an attempt
**User story:** As a candidate, I want to enter my name and pick a certification, so that I can begin a practice exam.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-001 | event | WHEN a candidate submits a non-empty name and selects an existing certification THE SYSTEM SHALL start an attempt and present its questions. |
| AC-002 | conditional | IF a candidate starts an attempt with an empty or missing name THEN THE SYSTEM SHALL reject the request with a validation error and SHALL NOT start the attempt. |
| AC-003 | conditional | IF a candidate selects a certification that does not exist THEN THE SYSTEM SHALL respond with a not-found error. |
| AC-004 | ubiquitous | THE SYSTEM SHALL offer, in the start form, a dropdown of the available certifications by title. |

### REQ-002 — Serve the exam questions
**User story:** As a candidate, I want all my exam questions on one page, so that I can answer them and submit together.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-005 | event | WHEN an attempt starts THE SYSTEM SHALL serve exactly the certificate's configured number of questions (questionsToAsk). |
| AC-006 | ubiquitous | THE SYSTEM SHALL select the served questions proportionally to each section's weight. |
| AC-007 | ubiquitous | THE SYSTEM SHALL present every served question on a single page, each with its four options as single-select radio buttons. |
| AC-008 | ubiquitous | THE SYSTEM SHALL display a submit control at the bottom of the exam page. |

### REQ-003 — Submit and score
**User story:** As a candidate, I want my exam scored on submission, so that I know whether I passed.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-009 | event | WHEN a candidate submits the exam THE SYSTEM SHALL compute the score as the percentage of served questions answered correctly. |
| AC-010 | conditional | IF a served question is left unanswered THEN THE SYSTEM SHALL count it as incorrect. |
| AC-011 | conditional | IF the score is greater than or equal to the certificate's pass percentage THEN THE SYSTEM SHALL report the result as PASS. |
| AC-012 | conditional | IF the score is less than the certificate's pass percentage THEN THE SYSTEM SHALL report the result as FAIL. |

### REQ-004 — Show the result with per-question feedback
**User story:** As a candidate, I want to see which answers were right or wrong with an explanation, so that I can learn from the attempt.

**Acceptance criteria:**
| ID | Type | Criterion (EARS) |
|----|------|------------------|
| AC-013 | event | WHEN the result is displayed THE SYSTEM SHALL show the candidate's name, the score, and the PASS or FAIL outcome. |
| AC-014 | ubiquitous | THE SYSTEM SHALL indicate, for each question, whether the candidate's answer was correct or incorrect. |
| AC-015 | ubiquitous | THE SYSTEM SHALL show, for each question, the correct option and its one-line explanation. |

## Non-Functional Requirements
| ID | Category | Criterion (EARS) |
|----|----------|------------------|
| REQ-050 | validation | THE SYSTEM SHALL perform scoring and pass/fail determination server-side and SHALL NOT rely on the client to compute the result. |
| REQ-051 | usability | THE SYSTEM SHALL present a server-rendered Thymeleaf UI for starting an attempt, answering, and viewing the result. |
| REQ-052 | reliability | WHEN an attempt references a question that no longer exists at scoring time THE SYSTEM SHALL score it as incorrect rather than failing the request. |
| REQ-053 | maintainability | THE SYSTEM SHALL reuse the Certification Management proportional-selection rule rather than duplicating it. |

## Out of Scope
- Timed exams, pausing, or resuming an attempt.
- Persisting attempt history or per-candidate accounts (an attempt is scored and shown once).
- Multi-select questions (single-answer only, consistent with Certification Management).
- Authentication and authorization.
- Reviewing past attempts or analytics dashboards.
