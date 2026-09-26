---
featureId: FEAT-NNN
slug: <feature-slug>
title: <Feature Title>
phase: code-and-tests
status: draft
sourceArtifact: features/<feature-slug>/2-design.md
---

# Code & Test Cases — <Feature Title>

## Implementation Summary
<What was built, in a few sentences.>

## File Manifest
| File | Stereotype | Implements | Change |
|------|-----------|------------|--------|
| `src/main/java/com/specquiz/entity/<Feature>.java` | Entity | DES-00x | new |
| `src/main/java/com/specquiz/repository/<Feature>Repository.java` | Repository | DES-00x | new |
| `src/main/java/com/specquiz/service/<Feature>Service.java` | Service | DES-00x | new |
| `src/main/java/com/specquiz/controller/<Feature>Controller.java` | Controller | DES-00x | new |
| `src/main/java/com/specquiz/dto/<Feature>Request.java` | DTO | DES-00x | new |
| `src/main/java/com/specquiz/dto/<Feature>Response.java` | DTO | DES-00x | new |

## Test Cases
| TC | Covers (AC) | Level | Description | Expected result |
|----|-------------|-------|-------------|-----------------|
| TC-001 | AC-001 | unit/integration | <what it exercises> | <expected> |
| TC-002 | AC-002 | integration | <what it exercises> | <expected> |

## Acceptance Criteria Coverage
| AC | Covered by |
|----|-----------|
| AC-001 | TC-001 |
| AC-002 | TC-002 |

## Build / Test Result
- `./gradlew build`: <PASS/FAIL>
- Tests run: <n>, passed: <n>, failed: <n>
