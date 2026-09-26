# Certification Management — Requirements

> Confluence-ready page generated from the approved SpecFlow Phase 1 artifacts
> (BRD → PRD → FSD). Paste or import into your Confluence space. Feature: **FEAT-001**.

## Overview
Certification Management is the authoring side of SpecQuiz. An exam author creates a
certification exam that mirrors a real one: a titled certificate with a pass
percentage, one or more weighted sections (each weight a multiple of 20, summing to
100), and a pool of single-answer multiple-choice questions (four options, exactly
one correct, a one-line explanation). Authors have full CRUD over questions, set how
many questions an attempt serves (at least five, drawn proportionally to section
weights), and a set of realistic sample certifications is bootstrapped on startup.

## Business Goals (BRD)
| ID | Goal | Success metric |
|----|------|----------------|
| BRD-001 | Enable authors to create structured, weighted exams without engineering help | Valid certification published in under 15 minutes, zero code changes |
| BRD-002 | Ensure exams mirror real certification structure | 100% of exams enforce weighted sections (multiples of 20 summing to 100) and a pass percentage |
| BRD-003 | Provide ready-to-use content out of the box | >=2 sample certifications on startup, no manual entry |
| BRD-004 | Keep the question pool maintainable | Edit/delete any question, changes reflected immediately |

## Product Features (PRD)
| ID | Name | Priority | Traces to BRD |
|----|------|----------|---------------|
| PRD-001 | Create certification | must | BRD-001, BRD-002 |
| PRD-002 | Manage weighted sections | must | BRD-002 |
| PRD-003 | Author questions | must | BRD-001 |
| PRD-004 | Edit and delete questions | must | BRD-004 |
| PRD-005 | Configure exam length | must | BRD-002 |
| PRD-006 | Enforce validity rules | must | BRD-002 |
| PRD-007 | Bootstrap sample certifications | must | BRD-003 |
| PRD-008 | Set pass percentage | must | BRD-002 |

## Functional Requirements (FSD)
| REQ | User story | Acceptance criteria |
|-----|-----------|---------------------|
| REQ-001 | Create a certificate with title and pass percentage | AC-001, AC-002, AC-003 |
| REQ-002 | Manage weighted sections | AC-004, AC-005, AC-006, AC-007 |
| REQ-003 | Author a question | AC-008, AC-009, AC-010, AC-011 |
| REQ-004 | Edit a question | AC-012, AC-013, AC-014 |
| REQ-005 | Delete a question | AC-015, AC-016 |
| REQ-006 | Configure exam length | AC-017, AC-018, AC-019 |
| REQ-007 | Enforce certificate validity on save | AC-020, AC-021 |
| REQ-008 | Set pass percentage | AC-022 |
| REQ-009 | Bootstrap sample certifications on startup | AC-023, AC-024 |
| REQ-010 | View certifications and their questions | AC-025, AC-026 |

## Non-Functional Requirements
| REQ | Category | Statement |
|-----|----------|-----------|
| REQ-050 | validation | All structural validation performed server-side |
| REQ-051 | usability | Display running total of section weights toward 100 |
| REQ-052 | usability | Server-rendered Thymeleaf UI for all authoring actions |
| REQ-053 | reliability | Clear, structured error responses for rejected requests |
| REQ-054 | performance | Certificate list renders within 2 seconds (local dev) |
| REQ-055 | maintainability | Code grouped by stereotype, feature-prefixed, under com.specquiz |

## Out of Scope
- Multi-select or non-multiple-choice question types
- Authentication / authorization and per-author ownership
- Rich media in questions
- Persistence across restarts (in-memory H2 this release)
- The exam-taking experience (Certification Attempt feature)

## Traceability
`BRD-001..004` ← `PRD-001..008` ← `REQ-001..010` (each with `AC-###`). Downstream,
Design will map each REQ/AC to `DES-###`, and Code & Tests will map each AC to `TC-###`.
