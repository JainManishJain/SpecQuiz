# SpecFlow — Spec-Driven SDLC Workflow

SpecFlow is SpecQuiz's central governance layer for building **every** feature the
same way. Instead of ad-hoc prompts, each SDLC phase has a standardized
**prompt + schema + template** that any developer or AI agent uses to generate a
consistent, validated artifact. The output of one phase is the input to the next.

## The two kinds of files

1. **Phase Specifications (reusable rules)** — live in `.specflow/`, authored once,
   committed, used for every feature. This is the "standardized AI prompting language."
2. **Feature Artifacts (per-feature outputs)** — live in `features/<slug>/`,
   generated per feature per phase.

Pattern for every phase:

```
prompt.md  +  schema.yaml  +  <previous phase artifact>   ── (AI agent) ──▶   features/<slug>/<phase>.md
   (rules)      (contract)        (input)                                        (validated output)
```

## Directory layout

```
.specflow/
├── workflow.yaml                 # phase order + input→output wiring + conventions
├── README.md                     # this file
└── phases/
    ├── 1-requirements/           # TIERED
    │   ├── fsd/                   # Functional Spec — DEFAULT, always (prompt·schema·template)
    │   ├── brd/                   # Business Requirements — OPTIONAL
    │   ├── prd/                   # Product Requirements — OPTIONAL
    │   └── export/               # Jira CSV + Confluence MD — OPTIONAL sub-step
    ├── 2-design/                 # single exhaustive Technical Design Document
    ├── 3-code-and-tests/
    ├── 4-review/
    └── 5-build-and-deploy/

features/
└── <feature-slug>/
    ├── requirements/
    │   ├── brd.md                # only if BRD tier was run
    │   └── prd.md                # only if PRD tier was run
    ├── 1-requirements.md         # the FSD (canonical phase-1 output)
    ├── exports/                  # only if export sub-step was run
    │   ├── jira-stories.csv
    │   └── confluence-<slug>.md
    ├── 2-design.md
    ├── 3-code-and-tests.md
    ├── 4-review.md
    └── 5-build-and-deploy.md
```

## The five phases

| # | Phase | Input | Output | Gate |
|---|-------|-------|--------|------|
| 1 | Requirements | plain-English feature request | `1-requirements.md` (FSD) | — |
| 2 | Design | `1-requirements.md` | `2-design.md` | — |
| 3 | Code & Test Cases | `2-design.md` | `3-code-and-tests.md` + code in `src/` | — |
| 4 | Review | requirements + design + code | `4-review.md` | `verdict: PASS` |
| 5 | Build & Deploy | approved `4-review.md` | `5-build-and-deploy.md` | `build_status: SUCCESS` |

Gates are enforced: Phase 5 runs only when Phase 4's verdict is `PASS`.

### Phase 1 is tiered (choose depth per feature)

| Tier | Required? | Answers | Output |
|------|-----------|---------|--------|
| **FSD** (Functional Spec) | **Always** | how must it behave? (EARS acceptance criteria) | `features/<slug>/1-requirements.md` |
| BRD (Business Requirements) | Optional | why are we doing this? (goals, KPIs, risks) | `features/<slug>/requirements/brd.md` |
| PRD (Product Requirements) | Optional | what/for whom? (personas, features, priority) | `features/<slug>/requirements/prd.md` |
| Export (Jira + Confluence) | Optional | tracker/wiki seed files | `features/<slug>/exports/` |

- **Small feature:** run FSD only.
- **Large feature:** BRD → PRD → FSD, with traceability (`BRD-NNN` ← `PRD-NNN` ← `REQ-NNN`).
- **Export** produces import-ready files only — a Jira CSV (RFC-4180, one Story per
  requirement, `External ID = REQ-NNN` for idempotent re-import) and a single
  Confluence Markdown page. **No live API calls, no credentials** — you seed them manually.

### Phase 2 is one exhaustive document

The Design phase produces a single Technical Design Document detailed enough to
implement from directly: architecture + ADRs, full component list, complete data
model (fields, types, nullability, indexes, relationships), full API contracts
(all status codes), DTOs with validation, business logic, sequence flows,
validation & error handling, security, configuration, and a requirements-coverage
table. See `phases/2-design/schema.yaml` for the enforced contract.

## Conventions (see `workflow.yaml` for the authoritative copy)

- **Acceptance criteria** use **EARS** notation (`WHEN … THE SYSTEM SHALL …`).
- **Traceability IDs:** `BRD-NNN` (business goal), `PRD-NNN` (product feature),
  `FEAT-NNN` (feature), `REQ-NNN` (requirement), `AC-NNN` (acceptance criterion),
  `DES-NNN` (design element), `TC-NNN` (test case). The chain flows
  `BRD ← PRD ← REQ/AC ← DES ← TC`.
- **Code layout:** base package `com.specquiz`, classes grouped by stereotype and
  prefixed with the feature name:
  `QuestionController`, `QuestionService`, `QuestionRepository`, entity `Question`,
  DTOs `QuestionRequest` / `QuestionResponse`, utilities `QuestionUtil`.

## How to run a phase (with Kiro)

Reference the phase's rule files with `#File` and point at the prior artifact.

Phase 1, FSD tier (default) for the `question-management` feature:

> Follow the rules in `#.specflow/phases/1-requirements/fsd/prompt.md` and the
> contract in `#.specflow/phases/1-requirements/fsd/schema.yaml`. Feature request:
> "<plain-English description>". Generate `features/question-management/1-requirements.md`.

Phase 2 (Design) for the same feature:

> Follow the rules in `#.specflow/phases/2-design/prompt.md` and the contract in
> `#.specflow/phases/2-design/schema.yaml`. Input:
> `#features/question-management/1-requirements.md`. Generate
> `features/question-management/2-design.md`.

For the optional Phase 1 tiers, reference `brd/`, `prd/`, or `export/` prompt +
schema instead, in the order BRD → PRD → FSD → export.

The agent produces the artifact, and it is validated against the phase's schema
before the next phase begins.

## How to run a phase (scripted / CI)

A prompt runner reads `workflow.yaml`, and for a given phase concatenates
`prompt.md` + `schema.yaml` + the declared input artifact(s) into the LLM prompt,
then validates the response against `schema.yaml` (and checks the gate field).

## Adding or changing rules

Edit the files under `.specflow/phases/<phase>/`. Because every feature reads these
same files, one change updates the workflow for the whole team. Version this
directory like code — schema changes are contract changes.
