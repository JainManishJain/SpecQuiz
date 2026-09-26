# SpecQuiz

SpecQuiz is a spec-driven self-evaluation and knowledge-testing application for certification exams. It lets exam authors build weighted, realistic practice exams and lets candidates take and score them — helping candidates prepare for and measure their readiness against certification objectives. The UI is server-rendered with Thymeleaf; question CRUD is exposed over a REST API.

## Features

SpecQuiz ships two features, each built through the full spec-driven workflow (see [Spec-driven development](#spec-driven-development-specflow)).

### 1. Certification Management (authoring)
The authoring side. An exam author can:
- Create a certificate with a **title** and a **pass percentage** (1–100).
- Add **weighted sections** — choose an existing section name or a new one; each weight is a multiple of 20 (20/40/60/80/100) and the weights must total **100**.
- Author **questions** with four options, exactly one correct, and a one-line explanation; **edit** and **delete** questions.
- Set the **exam length** (how many questions an attempt serves — at least 5, drawn proportionally to section weight).
- **Validate** a certificate on demand (reports every rule violation at once).
- Two realistic sample certifications (AWS Certified Cloud Practitioner, AWS Certified AI Practitioner) are **bootstrapped on startup**.

**Try it:** `http://localhost:8080/certifications`

### 2. Certification Attempt (exam-taking)
The exam-taking side. A candidate:
- Enters their **name** and picks a certification from a **dropdown**, then starts.
- Answers all served questions on a **single page** (radio buttons), then submits.
- Gets a **result** scored server-side against the pass percentage: PASS/FAIL, the score, and each question flagged **correct/incorrect** with the correct option and its one-line explanation. Unanswered questions count as incorrect.

**Try it:** `http://localhost:8080/attempts/new`

> Note: the H2 database is in-memory this release, so authored certificates reset on restart (the two sample certifications are always re-seeded). Attempts are not persisted.

## Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Language     | Java 21                             |
| Framework    | Spring Boot 3.5.16                  |
| Web          | Spring Web (REST)                   |
| UI           | Thymeleaf (server-rendered)         |
| Persistence  | Spring Data JPA (Hibernate)         |
| Validation   | Spring Validation (Jakarta Bean Validation) |
| Database     | H2 (in-memory, development)         |
| Boilerplate  | Lombok                              |
| Build tool   | Gradle (Wrapper 8.14.1)             |
| Testing      | Spring Boot Test, JUnit 5           |

## Directory Structure

```
SpecQuiz/
├── build.gradle                # Build config, plugins, and dependencies
├── settings.gradle             # Gradle project settings (root project name)
├── gradle.properties           # Gradle/JVM toolchain and performance settings
├── gradlew / gradlew.bat       # Gradle wrapper scripts
├── gradle/wrapper/             # Gradle wrapper JAR and properties
├── README.md
├── .specflow/                  # Spec-driven SDLC governance (phase prompts/schemas/templates)
├── features/                   # Per-feature spec artifacts (requirements → design → code → review → deploy)
│   ├── certification-management/
│   └── certification-attempt/
└── src/
    ├── main/
    │   ├── java/com/specquiz/
    │   │   ├── SpecQuizApplication.java     # Application entry point
    │   │   ├── controller/                  # REST + Thymeleaf MVC controllers
    │   │   ├── service/                     # Business logic
    │   │   ├── repository/                  # Spring Data JPA repositories
    │   │   ├── entity/                      # JPA entities (Certificate, Section, Question, Option)
    │   │   ├── dto/                          # Request/response models
    │   │   ├── util/                         # Validation & selection helpers
    │   │   ├── config/                       # Startup data bootstrap
    │   │   ├── exception/                    # Exceptions + global handler
    │   │   └── api/PingController.java        # Health endpoint
    │   └── resources/
    │       ├── application.yml               # App + datasource + JPA config
    │       └── templates/                     # Thymeleaf views
    │           ├── certifications/            # Authoring UI (list, form, view)
    │           └── attempts/                   # Exam UI (start, exam, result)
    └── test/
        └── java/com/specquiz/                # 57 tests across validator, service, and controller layers
```

Code is organized by stereotype under `com.specquiz`, with feature-prefixed class
names (e.g. `CertificationController`, `CertificationService`, `AttemptService`).

## Prerequisites

- A JDK 21 (the Gradle toolchain will auto-download one if it is not already installed; `org.gradle.java.installations.auto-download=true` in `gradle.properties`).
- No local Gradle install is required — use the bundled wrapper (`./gradlew`).

## Setup

Clone the repository and move into the project directory:

```bash
git clone <repository-url>
cd SpecQuiz
```

No further setup is needed. The H2 database runs in-memory and is created automatically on startup.

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`.

### Verify it's running

```bash
curl http://localhost:8080/api/ping
```

Expected response:

```json
{
  "application": "SpecQuiz",
  "status": "UP",
  "timestamp": "2026-09-26T00:00:00Z"
}
```

### Use the app

- **Author certifications:** `http://localhost:8080/certifications` — two sample certifications are already loaded.
- **Take an exam:** `http://localhost:8080/attempts/new` — enter a name, pick a certification, and start.

### H2 Console

While the app is running, the H2 web console is available at `http://localhost:8080/h2-console` using:

- **JDBC URL:** `jdbc:h2:mem:specquiz`
- **User:** `sa`
- **Password:** _(empty)_

## Test

```bash
./gradlew test
```

All 57 tests (validator, service, and controller/MVC layers) should pass.

## Configuration

Application configuration lives in `src/main/resources/application.yml`. The default profile uses an in-memory H2 database intended for local development; replace the datasource settings to point at a persistent database for other environments.

## Spec-driven development (SpecFlow)

Every feature in SpecQuiz is built through a standardized, spec-driven SDLC defined
in `.specflow/`. Each feature flows through five phases, and the output of one phase
is the input to the next:

1. **Requirements** — a Functional Spec (FSD) with EARS acceptance criteria (optionally preceded by BRD/PRD and Jira/Confluence exports for larger features).
2. **Design** — a single exhaustive Technical Design Document.
3. **Code & Test Cases** — implementation plus tests mapped to each acceptance criterion.
4. **Review** — an independent conformance review that issues a **PASS/FAIL** gate.
5. **Build & Deploy** — build gate, deployment and rollback plan, release notes.

The per-feature artifacts live under `features/<feature>/`; the reusable phase
rules, JSON-Schema-style contracts, and templates live under `.specflow/`. See
`.specflow/README.md` for the full workflow. Both shipped features
(`certification-management`, `certification-attempt`) have a complete artifact trail,
including their review records — the review gate caught real defects in both features
that were remediated before they passed.
