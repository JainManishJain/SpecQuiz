---
featureId: FEAT-001
slug: certification-management
title: Certification Management
phase: requirements
tier: brd
status: draft
sourceArtifact: null
---

# Business Requirements Document — Certification Management

## Problem Statement
Teams preparing for certification exams (for example AWS Certified AI Practitioner
or AWS Certified Cloud Practitioner) lack a lightweight, self-hosted way to author
practice exams that mirror the structure of the real thing. Real certification
exams are organised into weighted domains/sections and have a defined pass mark.
Without a tool to model that structure — weighted sections, a curated question
pool, and a configurable pass percentage — exam authors resort to ad-hoc
spreadsheets that cannot be turned into an interactive, scored practice test.
Certification Management provides the authoring foundation so realistic practice
exams can be created, curated, and served.

## Business Goals
| ID | Goal | Success metric (KPI + target) |
|----|------|-------------------------------|
| BRD-001 | Enable authors to create structured, weighted certification exams without engineering help | An author can publish a valid certification (>=1 section, >=5 questions, weights summing to 100) in under 15 minutes, with zero code changes |
| BRD-002 | Ensure authored exams faithfully mirror real certification structure | 100% of published certifications enforce weighted sections (multiples of 20 summing to 100) and a configurable pass percentage |
| BRD-003 | Provide ready-to-use practice content out of the box | At least 2 sample certifications, each with >=1 section and >=5 questions, are available immediately on application startup with no manual data entry |
| BRD-004 | Keep the question pool accurate and maintainable over time | Authors can edit or delete any individual question, with changes reflected immediately (0 stale questions after an edit/delete) |

## Stakeholders
| Role | Interest / concern |
|------|--------------------|
| Exam Author (Trainer / SME) | Wants a fast, guided way to build realistic weighted exams and keep questions current |
| Certification Candidate | Depends on well-structured, correctly-weighted practice exams to gauge readiness |
| Training Program Owner | Wants consistent exam structure and a defensible pass mark across all certifications |
| Development Team | Wants the authoring model to cleanly feed the exam-taking (attempt) feature |

## Constraints
- Single correct answer per question only (no multi-select) in this release.
- Section weights are restricted to multiples of 20 (20, 40, 60, 80, 100) and must total 100 per certificate.
- Each certificate must have at least one section and at least five questions; the number of questions asked per attempt is at least five.
- Server-rendered Thymeleaf UI within the existing Spring Boot application; no separate frontend stack.
- H2 in-memory database for this release; sample data is bootstrapped on every startup.

## Assumptions
- Authors are trusted internal users; authentication/authorization is out of scope for this release.
- Question content is plain text (no images or rich media) in this release.
- A single language (English) is sufficient for now.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Authors create invalid exams (weights not summing to 100, too few questions) | high | medium | Enforce validation rules at save time and surface clear errors in the UI |
| In-memory H2 loses authored data on restart | medium | high | Bootstrap sample data on startup so the app is always usable; document that authored data is non-persistent this release |
| Weighted section sampling produces an exam that does not respect proportions | medium | medium | Define and test proportional sampling explicitly in Design/Test phases |
