---
featureId: FEAT-NNN
slug: <feature-slug>
title: <Feature Title>
phase: design
status: draft
sourceArtifact: features/<feature-slug>/1-requirements.md
---

# Technical Design — <Feature Title>

## 1. Overview
<Design approach and the key decisions in a few paragraphs.>

## 2. Architecture
<How the feature flows through the layers.>

### Layers
| Layer | Responsibility |
|-------|----------------|
| Controller | REST endpoints, request/response mapping |
| Service | Business logic, validation orchestration, transactions |
| Repository | Spring Data JPA persistence |
| Database | H2 (dev) |

### Design Decisions (ADRs)
| Decision | Rationale | Alternatives considered |
|----------|-----------|-------------------------|
| <decision> | <why> | <what else> |

## 3. Components
| Class | Stereotype | Package | Responsibility | Depends on | Implements |
|-------|-----------|---------|----------------|------------|------------|
| `<Feature>Controller` | Controller | com.specquiz.controller | REST endpoints | `<Feature>Service` | DES-001 |
| `<Feature>Service` | Service | com.specquiz.service | Business logic | `<Feature>Repository` | DES-002 |
| `<Feature>Repository` | Repository | com.specquiz.repository | Persistence | — | DES-003 |
| `<Feature>` | Entity | com.specquiz.entity | JPA entity | — | DES-004 |
| `<Feature>Request` | DTO | com.specquiz.dto | Inbound model | — | DES-005 |
| `<Feature>Response` | DTO | com.specquiz.dto | Outbound model | — | DES-006 |

## 4. Data Model
### <Feature> (table: `<table_name>`)
| Field | Column | Type | Nullable | Constraints | Notes |
|-------|--------|------|----------|-------------|-------|
| id | id | Long | no | PK, generated | |
| ... | ... | ... | ... | ... | |

- **Primary key:** id
- **Indexes:** `idx_<...>` on (columns), unique: yes/no
- **Relationships:** <e.g. many-to-one -> Topic, fetch LAZY>

## 5. API Contracts
### DES-001 — <operation>
- **Method / Path:** `POST /api/<resource>`
- **Path params:** none
- **Query params:** none
- **Request body:** `<Feature>Request`
- **Responses:**
  | Status | Body | Description |
  |--------|------|-------------|
  | 201 | `<Feature>Response` | Created |
  | 400 | ErrorResponse | Validation failed |
  | 404 | ErrorResponse | Related resource not found |
- **Satisfies:** REQ-001 (AC-001, AC-002)

## 6. DTOs
### `<Feature>Request` (request)
| Field | Type | Validation | Notes |
|-------|------|-----------|-------|
| <field> | <type> | @NotBlank, @Size(max=...) | |

### `<Feature>Response` (response)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | |

## 7. Business Logic
### DES-002 — <rule name>
Steps:
1. <step>
2. <step>
- **Satisfies:** REQ-001 (AC-001)

## 8. Sequence Flows
### <Operation> flow
1. Controller receives request, binds `<Feature>Request`
2. Bean validation runs
3. Service applies business rules
4. Repository persists
5. Controller returns `<Feature>Response` (201)

## 9. Validation Rules
| Field | Rule | HTTP status | Satisfies |
|-------|------|-------------|-----------|
| <field> | <rule> | 400 | REQ-050 |

## 10. Error Handling
**Standard error body:** `{ timestamp, status, error, message, path }`

| Condition | HTTP status | Exception |
|-----------|-------------|-----------|
| Resource not found | 404 | `<Feature>NotFoundException` |
| Validation failure | 400 | `MethodArgumentNotValidException` |

## 11. Security
- <consideration, e.g. all inputs validated server-side>
- Auth required: yes/no

## 12. Configuration
| Key | Purpose | Default |
|-----|---------|---------|
| <application.yml key / dependency / migration> | <purpose> | <default> |

## 13. Observability (optional)
- Logging: <what to log>
- Metrics: <what to measure>

## 14. Requirements Coverage
| Requirement / AC | Design element(s) |
|------------------|-------------------|
| REQ-001 / AC-001 | DES-001, DES-002 |
| REQ-002 / AC-003 | DES-00x |
