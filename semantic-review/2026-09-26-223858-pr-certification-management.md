# Certification Management remediation (re-review of NEEDS_CHANGES findings)

Second-pass review verifying the five findings from the prior spec-driven review (one blocker, three majors, one minor) are genuinely fixed rather than papered over, and that the fixes did not regress the aggregate or the authoring flow. The remediation adds a server-side 1..100 pass-percentage guard, an explicit `validateCertificate(id)` action wired to a new `POST /certifications/{id}/validate` endpoint and a "Validate certificate" button, a `requireUniqueTitle` pre-check with a `selfId` escape hatch, three new global exception handlers, and shortfall redistribution in proportional selection. Test count grew from 26 to 41 (15 validator + 16 service + 6 MVC + 3 REST + 1 context), and the full suite is green under `./gradlew clean test`.

Watch for: (1) All five findings are fixed and each behavioral fix except F-004 has a test that asserts the corrected behavior (confirmed). (2) The two new F-004 handlers (`ConstraintViolationException`, `HttpMessageNotReadableException`) are correct by inspection but untested, and the `ConstraintViolationException` path is not actually reachable from any endpoint since neither MVC query-param method is `@Validated` (confirmed — coverage gap, not a defect). (3) No blocker or major remains unresolved.

**Verdict**: APPROVED

## High-level view

The blocker (F-001) is closed at the layer that matters. `setPassPercentage` now rejects null/`<1`/`>100` in the service before persisting, the controller catches the validation exception and re-renders with a field error, and the HTML input carries `min/max`. Two tests pin it: a service test drives both 0 and 200, and a MockMvc test drives 200 and asserts the error model attribute.

The central major (F-002) is closed with a real authoring action, not a bootstrap-only path. `validateCertificate(Long)` reloads the aggregate and runs the full DES-006 validation; `POST /certifications/{id}/validate` and a Validate button expose it, and both the success and the aggregated-error branches are asserted at the service and MVC layers. AC-006/AC-007/AC-020 are now reachable through the UI, which was the prior gap.

Duplicate-title (F-003) is handled proactively via a `findByTitle` pre-check that throws a structured `title` violation, with a `DataIntegrityViolationException` handler as a backstop. The pre-check takes a `selfId` argument and excludes it, so a no-op re-save of an existing title does not falsely trip — the edit/no-op case the remediation needed to handle.

The error-handling major (F-004) is functionally closed: the three named exceptions now map to 400/409 with the standard error body instead of falling through to 500. The caveat is that no test exercises the two new query-param/malformed-body handlers, and the `ConstraintViolationException` handler has no live trigger because the pass-percentage bound moved into the service rather than onto a `@Validated` param. This is a coverage gap, not a regression.

Proportional selection (F-005) now redistributes any per-section shortfall to sections with spare questions, so the result sums to N whenever the total pool allows, and a lopsided-distribution test asserts exactly that. The `addQuestion` change to `saveAndFlush` still maintains the aggregate: the question is linked to its managed section and added to the section's collection before flush, and a service test confirms the reloaded section's question count grows by one.

<details>
<summary>Issues (1)</summary>

1. **New F-004 handlers untested and one is unreachable** — `ConstraintViolationException` and `HttpMessageNotReadableException` handlers are correct by inspection but have no test; the `ConstraintViolationException` path has no live trigger since neither MVC query-param endpoint is `@Validated`. Add a malformed-JSON MockMvc test against the REST endpoint to lock in the 400 body, and either drop the unreachable handler or add a `@Validated` bound that exercises it. (info, non-blocking)

</details>

<details>
<summary>Details</summary>

### F-001 — pass-percentage 1..100 bound — RESOLVED

The bound is now enforced server-side in `CertificationService.setPassPercentage` (lines 122-124): null, `<1`, or `>100` throws a `CertificationValidationException` carrying a `passPercentage` violation before the entity is touched. The controller (lines 94-106) catches it and re-renders `certifications/view` with a `passPercentageError` model attribute rather than redirecting, mirroring the exam-length path, and `view.html` renders that attribute and sets `min="1" max="100"` on the input.

Two tests assert the fixed behavior, not just the happy path: TC-029 drives both `0` and `200` through the service and expects the exception; TC-033 posts `passPercentage=200` through MockMvc and asserts status 200 with the `passPercentageError` attribute present. TC-034 confirms a valid value still redirects. The bound now lives at the service layer where persistence happens, so the DTO-bypass hole from the prior review no longer exists.

### F-002 — aggregate validation on the authoring path — RESOLVED

`validateCertificate(Long)` (service lines 91-94) reloads the aggregate through `getCertificate` and runs `validator.validateCertificate`, which throws with the complete violation set (AC-021). `POST /certifications/{id}/validate` (controller lines 108-118) invokes it, putting `validationOk` on success or `validationErrors` on failure, and `view.html` has a "Validate certificate" button posting to that endpoint plus both display blocks. So AC-006 (weights sum to 100), AC-007 (≥1 section), and AC-020 (≥5 questions) are now enforced through a real, user-triggerable action instead of only at bootstrap or incidentally via exam-length changes.

Asserted at both layers: TC-027 creates an invalid draft via `createDraft` and asserts `validateCertificate` throws with a `sections`/`questions` violation; TC-028 asserts a valid certificate passes; TC-035 posts to `/validate` for an invalid draft and asserts `validationErrors`; TC-036 asserts `validationOk` for a valid one. These close the prior "no save/validate endpoint exists" gap.

### F-003 — duplicate title no longer 500s — RESOLVED

`requireUniqueTitle(title, selfId)` (service lines 189-199) queries `findByTitle` and throws a structured `title` violation when a different certificate already holds the name. Both `create` and `createDraft` call it with `selfId = null`. The `GlobalExceptionHandler` also maps `DataIntegrityViolationException` to 409 with a `title` violation as a backstop if the pre-check is ever bypassed.

```java
private void requireUniqueTitle(String title, Long selfId) {
    if (title == null || title.isBlank()) {
        return;
    }
    certificationRepository.findByTitle(title)
            .filter(existing -> !existing.getId().equals(selfId))
            .ifPresent(existing -> {
                throw new CertificationValidationException(List.of(
                        new Violation("title", "a certificate with this title already exists")));
            });
}
```

The `selfId` filter is the edit/no-op safeguard called out in the remediation brief: a re-save that keeps the same title is excluded from the collision check, so it will not falsely reject. Note that no current caller passes a non-null `selfId` (there is no rename endpoint yet), so the escape hatch is presently forward-looking; that is acceptable and does not affect any shipped path. TC-030 asserts a duplicate title through `create` throws with a `title` violation, so the 500 is gone at the path that exists today.

### F-004 — query-param bounds and malformed body no longer 500 — RESOLVED (coverage caveat)

`GlobalExceptionHandler` gains three handlers: `ConstraintViolationException` → 400 with per-violation entries, `HttpMessageNotReadableException` → 400 "malformed request body", and `DataIntegrityViolationException` → 409. All build the same standard error body, so the two exceptions named in the prior review no longer surface as unstyled 500s.

Two caveats, both non-blocking:

- Neither new handler has a test. The `HttpMessageNotReadableException` handler is reachable — a malformed JSON body posted to the REST `QuestionController` would hit it — but nothing posts malformed JSON in the suite, so the 400 mapping is verified only by inspection.
- The `ConstraintViolationException` handler has no live trigger. That exception is raised only when a `@Validated` bean has a violated constraint on a method parameter, and neither `setPassPercentage` nor `setExamLength` is `@Validated` or annotates its `@RequestParam Integer` with `@Min/@Max` — the pass-percentage bound moved into the service (F-001) instead. So the handler is dead code today. It does no harm, but it is not exercised and cannot be until a `@Validated` param is introduced.

The finding was "these fall through to 500"; that is fixed for every exception that can actually be thrown. A malformed-body MockMvc test and either wiring or removing the unreachable handler would tighten it.

### F-005 — proportional selection returns exactly N — RESOLVED

`selectProportionally` keeps the largest-remainder base allocation, then adds a redistribution loop: after taking `min(alloc, pool)` from each section and tracking the shortfall, it walks sections that still have unused questions and pulls from them until the shortfall reaches zero or the total pool is exhausted. Because aggregate validation guarantees the total pool ≥ `questionsToAsk`, the loop fills to N in every valid certificate.

TC-040 asserts this concretely: section A is weighted 80% (allocated 4) but holds a single question, section B holds ten; the result is exactly 5, with A contributing its lone question and B absorbing the shortfall. TC-014 still asserts the proportional split on a well-populated certificate. The lossy `Math.min` clamp from the prior review is gone.

### Regression check on the remediation itself

The `addQuestion` switch to `questionRepository.saveAndFlush(question)` (service lines 137-149) was the main regression risk, since it persists the child directly rather than cascading through the certificate. It still maintains the aggregate: the question's `section` is set and it is added to `section.getQuestions()` before the flush, and the section is a managed entity loaded in the same transaction, so both sides of the relationship stay consistent and the returned instance carries its generated id. TC-017 reloads the certificate after the call and asserts the target section's question count increased by one, and the REST TC-037 asserts a 201 with a populated `id` and four options — together confirming no aggregate drift.

`create` now calls `requireUniqueTitle` before `validator.validateCertificate`; TC-016/TC-026/TC-030 cover the persist, invalid-aggregate-reject, and duplicate-title-reject paths respectively, so the added pre-check did not break the bootstrap-protection contract. `validateCertificate` reuses `getCertificate`, which eagerly initializes the lazy graph, so it validates the full aggregate rather than a detached shell — a real risk given the child is now flushed independently.

### Test adequacy

The suite grew to 41 tests, all passing, and the additions target exactly the previously-invisible layers: MVC tests for list/view/pass-percentage/validate and REST tests for question add/validation-error/not-found. The validator and service tests remain behavioral — asserting on `violation.field()` and message content, not counts. The one honest gap is F-004 (no malformed-body or constraint-violation test), and the previously-accepted AC-004 (existing/new section name datalist) remains UI-only.

</details>

<details>
<summary>File map</summary>

- `service/CertificationService.java` — `setPassPercentage` 1..100 guard (F-001), `validateCertificate(Long)` (F-002), `requireUniqueTitle(title, selfId)` via `findByTitle` (F-003), `addQuestion` now `saveAndFlush`.
- `controller/CertificationController.java` — `POST /certifications/{id}/validate`; pass-percentage catches the validation error and re-renders.
- `exception/GlobalExceptionHandler.java` — new `ConstraintViolationException`, `HttpMessageNotReadableException`, `DataIntegrityViolationException` handlers (F-004).
- `util/CertificationValidator.java` — `selectProportionally` redistributes shortfall (F-005).
- `templates/certifications/view.html` — Validate button, `validationOk`/`validationErrors`/`passPercentageError` displays, `min/max` on pass-% input.
- `test/.../CertificationValidatorTest.java` (15), `CertificationServiceTest.java` (16), `controller/CertificationControllerTest.java` (6 MockMvc), `controller/QuestionControllerTest.java` (3 REST) — plus 1 context test = 41 passing.

Not a git repository; reviewed against the working tree. Suite verified with `./gradlew clean test`.

</details>
