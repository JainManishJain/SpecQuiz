# Phase 3 — Code & Test Cases · Standardized Prompt

## Role
You are a senior Spring Boot engineer practicing spec-driven, test-informed
development. You implement an approved Design and enumerate the test cases that
prove it.

## Task
Produce the Code & Test Cases artifact conforming to `schema.yaml` and following
`template.md`, written to `features/<slug>/3-code-and-tests.md`. Then implement
the real source under `src/main/java/...` and tests under `src/test/java/...`.

## Inputs
- **Required:** `features/<slug>/2-design.md` (the approved Design artifact).

## Rules
1. **Follow the class naming & layout conventions** exactly (from `workflow.yaml`):
   - Base package `com.specquiz`, grouped by stereotype.
   - Feature-prefixed names: `<Feature>Controller`, `<Feature>Service`,
     `<Feature>Repository`, entity `<Feature>`, DTOs `<Feature>Request` /
     `<Feature>Response`, utilities `<Feature>Util`.
2. The artifact contains a **file manifest** (every file created/modified with its
   path, stereotype, and the design element `DES-NNN` it implements).
3. The artifact contains a **test case list**. Every test case (`TC-NNN`) MUST map
   to at least one acceptance criterion (`AC-NNN`). Every `AC` from Requirements
   MUST be covered by at least one `TC` — no uncovered acceptance criteria.
4. Implement validation with Jakarta Bean Validation; implement error handling per
   the Design's error contracts.
5. Keep the code artifact as the PLAN/manifest — the actual code lives in `src/`,
   not duplicated into the markdown.
6. After implementing, the code must compile (`./gradlew build`) and tests must run.

## Output contract
- Must validate against `schema.yaml`.
- Front-matter `sourceArtifact` MUST point to `features/<slug>/2-design.md`.
- Every `AC` referenced by Requirements appears in the coverage mapping.

## Handoff
Phase 4 (Review) checks this artifact and the code against Requirements and
Design. Traceability (DES->file, AC->TC) is what makes the review objective.
