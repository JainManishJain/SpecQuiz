---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: requirements
tier: prd
status: draft
sourceArtifact: features/certification-management/requirements/brd.md
---

# Product Requirements Document — Certification Management

## Summary
Certification Management is the authoring surface of SpecQuiz. It lets an exam
author define a certification exam that mirrors a real one: a titled certificate
with a configurable pass percentage, one or more weighted sections (weights are
multiples of 20 that sum to 100), and a curated pool of single-answer questions
(four options, one correct, a one-line explanation). Authors get full control over
the question pool (add, edit, delete) and a set of realistic sample certifications
is loaded on startup so the tool is immediately usable. This authored content is
the source the Certification Attempt feature draws on to serve scored practice
exams.

## Personas
### Priya — Exam Author (Trainer / SME)
Priya designs practice exams for cloud certifications. She knows the real exam's
domain structure and pass mark and wants to reproduce it faithfully without asking
engineering for help.
**Needs:**
- Create a certificate with a title and pass percentage
- Define weighted sections that mirror the real exam's domains
- Add and maintain questions with a clear indication of the correct answer
- Trust that invalid exams are rejected before they reach candidates

### Sam — Training Program Owner
Sam oversees the certification program and cares about consistency and a
defensible pass mark across all exams.
**Needs:**
- Guarantee every published exam has a valid structure (weights total 100, minimum question counts)
- Have ready-made sample exams available to demonstrate the tool quickly

## Features
| ID | Name | Priority | Traces to BRD | Description |
|----|------|----------|---------------|-------------|
| PRD-001 | Create certification | must | BRD-001, BRD-002 | Author enters a certificate title and a pass percentage and creates the certificate |
| PRD-002 | Manage weighted sections | must | BRD-002 | Author adds sections (choosing an existing name or a new one) and assigns each a weight that is a multiple of 20; weights must total 100 |
| PRD-003 | Author questions | must | BRD-001 | Author adds a question to a section with four options, marks exactly one correct, and provides a one-line explanation |
| PRD-004 | Edit and delete questions | must | BRD-004 | Author edits or deletes any individual question in the pool |
| PRD-005 | Configure exam length | must | BRD-002 | Author sets how many questions an attempt serves (at least five), drawn proportionally to section weights |
| PRD-006 | Enforce validity rules | must | BRD-002 | The product rejects certificates that violate structural rules (weights not summing to 100, fewer than one section, fewer than five questions) |
| PRD-007 | Bootstrap sample certifications | must | BRD-003 | At least two realistic sample certifications, each with sections and at least five questions, are loaded on startup |
| PRD-008 | Set pass percentage | must | BRD-002 | Author sets the pass percentage that the attempt result is judged against |

## User Journeys
### Author creates a new certification
1. Priya opens the Certification Management page.
2. She enters the certificate title (e.g. "AWS Certified AI Practitioner") and a pass percentage.
3. She adds one or more sections, choosing an existing section name or typing a new one, and assigns each a weight (multiple of 20).
4. The product shows the running total of weights and blocks saving until it equals 100.
5. She adds questions to each section: four options, one marked correct, a one-line explanation.
6. She sets the number of questions the exam will ask (at least five).
7. She saves; the product validates the full structure and confirms the certificate is ready.

### Author maintains the question pool
1. Priya opens an existing certificate.
2. She edits a question's text, options, correct answer, or explanation, or deletes a question outright.
3. The change is reflected immediately in the pool.

## Release Scope
**In scope:**
- Authoring certificates, weighted sections, and single-answer questions
- Full question CRUD (add, edit, delete)
- Structural validation (weights total 100 as multiples of 20; minimums enforced)
- Configurable pass percentage and exam length
- Startup bootstrap of sample certifications
- Server-rendered Thymeleaf UI

**Out of scope:**
- Multi-select or non-multiple-choice question types
- Authentication / authorization and per-author ownership
- Rich media in questions (images, code blocks)
- Persisting authored data across restarts (in-memory H2 this release)
- The exam-taking experience (covered by the Certification Attempt feature)
