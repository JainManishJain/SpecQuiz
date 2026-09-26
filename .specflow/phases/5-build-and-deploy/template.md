---
featureId: FEAT-NNN
slug: <feature-slug>
title: <Feature Title>
phase: build-and-deploy
status: draft
sourceArtifact: features/<feature-slug>/4-review.md
build_status: FAILURE
---

# Build & Deploy — <Feature Title>

## Precondition Gate
- Review verdict (`4-review.md`): <PASS | FAIL>
- Proceed: <yes | no>

## Build
| Item | Value |
|------|-------|
| Command | `./gradlew build` |
| Version / tag | 0.0.1-SNAPSHOT |
| Artifact | `build/libs/specquiz-0.0.1-SNAPSHOT.jar` |
| build_status | SUCCESS / FAILURE |

## Deployment Plan
| Step | Action | Notes |
|------|--------|-------|
| 1 | <build/package> | |
| 2 | <run/deploy> | e.g. `java -jar build/libs/specquiz-0.0.1-SNAPSHOT.jar` |
| 3 | <health check> | e.g. `curl http://localhost:8080/api/ping` -> status UP |

Configuration / secrets required (by name only): <e.g. none for dev H2 profile>

## Rollback Plan
| Step | Action |
|------|--------|
| 1 | <stop new version> |
| 2 | <redeploy previous artifact/tag> |
| 3 | <verify health> |

## Release Notes
- Shipped: <summary> (REQ-001, REQ-002, ...)
- Known limitations: <...>
