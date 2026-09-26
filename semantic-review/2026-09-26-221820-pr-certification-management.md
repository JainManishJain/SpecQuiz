# Certification Management authoring feature (spec-driven review)

A layered Spring Boot authoring module: a `Certificate → Section → Question → Option` JPA aggregate, a `CertificationValidator` that aggregates all structural violations (AC-021), a `CertificationService` orchestrating create / section / question CRUD / exam-length / pass-percentage, a Thymeleaf UI plus a REST `QuestionController` for inline question editing, and a `CommandLineRunner` seeding two valid sample certs. The aggregate validation, question validation, and proportional-selection helper are implemented and unit-tested; the 26 declared tests compile and pass under `./gradlew clean test` (14 validator + 11 service + 1 context load). The build is green and the code is well-organized against the design.

Watch for: (1) **`setPassPercentage` persists any integer with no bounds check** — AC-022 / AC-003 / DES-009 all say 1..100, but 0 or 200 is accepted and stored (confirmed, blocker). (2) **The create flow uses `createDraft`, which never validates, and there is no save/validate endpoint anywhere** — a certificate with weights ≠ 100 or < 5 questions can be created and viewed indefinitely as if valid; only an exam-length change ever triggers aggregate validation (confirmed, major). (3) **Duplicate title and query-param bound violations escape the exception handler as HTTP 500** (confirmed, major). (4) Proportional selection can silently return fewer than N when a section's allocation exceeds its pool (confirmed, minor). (5) AC-004 and AC-025 have no automated test (author-flagged, accepted).

**Verdict**: NEEDS_CHANGES

## High-level view

The domain model and validator are the strong core of this change. `CertificationValidator.collectCertificateViolations` implements every rule in DES-006 (title, pass %, ≥1 section, weight multiple-of-20, weights sum to 100 with the current total in the message, ≥5 questions, exam-length bounds, per-question options/correct) and aggregates them, exactly as AC-021 requires. The validator tests assert field-level violations rather than just counts, so they are meaningful.

The weak point is *where* validation runs. The design splits `create` (validates the full aggregate — used only by the bootstrap loader) from `createDraft` (no validation — used by the actual `POST /certifications` UI flow). That split is intentional for empty-shell creation, but the design assumed a "save/validate" action would later enforce the aggregate rules (DES-006 sequence flow, AC-006/AC-007/AC-020). That action does not exist in the code. The only place aggregate validation fires post-creation is `setQuestionsToAsk`. As a result a certificate can permanently hold weights that don't sum to 100, zero sections, or fewer than 5 questions, and both the list and view pages render it as legitimate. AC-006, AC-007, and AC-020 are enforced by the validator in unit tests but are not reachable through any real authoring path except incidentally.

The pass-percentage update path is a straightforward correctness bug: the controller takes a raw `Integer` query param with no bean-validation annotation, and the service stores it without a bounds check, so the 1..100 rule from AC-022/AC-003/DES-009 is not enforced on that endpoint.

Error-handling coverage is narrower than DES-010/§10 specify. The handler covers not-found, aggregated validation, and `MethodArgumentNotValidException`, but not the DB unique-title violation, the `ConstraintViolationException` from query-param bounds, or the malformed-body case the design lists — all of which surface as unstyled 500s.

Proportional selection uses correct largest-remainder rounding, but clamps each section's take to its pool size without redistributing the shortfall, so a lightly-populated section can make the result smaller than N.

Test coverage tracks the manifest honestly: 26 tests, all passing, with AC-004 and AC-025 openly flagged as UI-only. The gap is that the tests never exercise the service `create` rejection path or the controller layer, which is why the `createDraft` and pass-percentage holes slipped through.

<details>
<summary>Issues (7)</summary>

1. **Pass-percentage bounds not enforced** — `setPassPercentage` (controller + service) accepts and stores any integer; add `@Min(1)/@Max(100)` (with `@Validated` on the controller) or a service-side check and re-render with errors per DES-009. (blocker)
2. **No aggregate validation on the authoring path** — the create flow calls `createDraft` and no save/validate endpoint exists, so AC-006/AC-007/AC-020 are never enforced after creation except via exam-length changes. Add an explicit validate/save action that calls `validator.validateCertificate` and surface violations in the view. (major)
3. **Duplicate title returns 500** — the `uk_certificate_title` constraint throws `DataIntegrityViolationException`, unhandled by `GlobalExceptionHandler`; either check `existsByTitle` before save or map the exception to a 400/validation response. (major)
4. **Query-param / malformed-body exceptions unhandled** — `ConstraintViolationException` and `HttpMessageNotReadableException` (both named in the design's error table) fall through to 500; add handlers returning the standard error body. (major)
5. **Proportional selection can return fewer than N** — `Math.min(alloc[i], pool.size())` drops the shortfall instead of redistributing to other sections; redistribute leftover picks so the result sums to N when the total pool allows. (minor)
6. **AC-004 not tested** — "select existing or new section name" is only in `view.html` (datalist); no automated assertion. Author-flagged; add a slice/MVC test or accept as documented UI-only coverage. (info)
7. **AC-025 not tested** — the list page rendering is only exercised by context load, not asserted; add a `@WebMvcTest`/MockMvc test or accept as documented. (info)

</details>

<details>
<summary>Details</summary>

### Where aggregate validation actually runs (the create/createDraft split)

The design's create-certificate sequence flow (§8) and DES-006 assume a save/validate action enforces the full aggregate rules; `CertificationController.create` instead calls `service.createDraft`, which is a bare `repository.save` with no validation:

```java
// CertificationService.java
public Certificate create(Certificate certificate) {
    validator.validateCertificate(certificate); // full structural validation
    return certificationRepository.save(certificate);
}
public Certificate createDraft(Certificate certificate) {
    return certificationRepository.save(certificate); // no validation
}
```

`create` is invoked only by `CertificationDataLoader`. Every UI-created certificate goes through `createDraft`. After that, the only code path that runs `validator.validateCertificate` is `setQuestionsToAsk`. `addSection` and `setPassPercentage` both `save` without aggregate validation. So an author can create a certificate, add a single section with weight 40, add three questions, and the certificate persists and renders on both list and view pages with no indication it is invalid — AC-006 (weights sum to 100), AC-007 (≥1 section is technically met, but the "no sections" state is reachable immediately after create), and AC-020 (≥5 questions) are never enforced through a real authoring action. The rules exist and are unit-tested on the validator, but the feature is missing the endpoint that would apply them to authored data. This is the core spec-conformance gap: the validator faithfully implements DES-006, but the service/controller never call it on the path that matters.

The `setQuestionsToAsk` rollback is done correctly — it captures the previous value, mutates, validates, and restores on failure before rethrowing — so AC-017/AC-018 do hold on that specific endpoint.

### Pass-percentage endpoint skips the 1..100 rule

DES-009 specifies a 200-with-errors response when the value is not 1..100, and AC-022/AC-003 bound it to 1..100. The endpoint enforces neither:

```java
// CertificationController.java
@PostMapping("/certifications/{id}/pass-percentage")
public String setPassPercentage(@PathVariable Long id, @RequestParam Integer passPercentage) {
    service.setPassPercentage(id, passPercentage); // no bounds check
    return "redirect:/certifications/" + id;
}
```

```java
// CertificationService.java
public Certificate setPassPercentage(Long certificateId, Integer passPercentage) {
    Certificate certificate = getCertificate(certificateId);
    certificate.setPassPercentage(passPercentage); // stores 0, 200, negative, ...
    return certificationRepository.save(certificate);
}
```

The DTO `CertificationRequest.passPercentage` is correctly annotated `@Min(1) @Max(100)`, but this endpoint takes a raw query param that bypasses that DTO entirely. TC-023 only checks the happy path (85), so the hole is untested. Fix by validating the bound server-side (annotation with `@Validated`, or an explicit check that re-renders the view with an error like the exam-length path does).

### Error handling narrower than the design's error table

`GlobalExceptionHandler` covers `CertificationNotFoundException` (404), `CertificationValidationException` (400 with violations), and `MethodArgumentNotValidException` (400). The design's §10 table and DES-010 also call for handling malformed bodies (`HttpMessageNotReadableException`) and, implicitly, the unique-title constraint. Two concrete gaps:

- The `uk_certificate_title` unique constraint means a second certificate with the same title throws `DataIntegrityViolationException` at flush time. Nothing handles it, so the author gets a raw 500. `existsByTitle`/`findByTitle` are defined on the repository but never called — the intended pre-check appears to have been dropped. Either pre-check and add a `title` violation, or map the exception.
- Query-param bounds (once added for pass-percentage/exam-length) throw `ConstraintViolationException`, not `MethodArgumentNotValidException`, so they would also fall through to 500 without a dedicated handler.

### Proportional selection: lossy clamp

`selectProportionally` floors each section's exact share and distributes the leftover to the largest fractional remainders (correct largest-remainder; TC-014 verifies 80/20-of-5 → 4/1). The gap is the final take:

```java
int take = Math.min(alloc[i], pool.size());
selected.addAll(pool.stream().limit(take).collect(Collectors.toList()));
```

If a section is allocated more questions than it holds, the shortfall is silently dropped rather than reassigned to sections that still have spare questions, so the returned list can be smaller than `questionsToAsk`. Aggregate validation guarantees the *total* pool ≥ questionsToAsk, but not that each section's pool ≥ its proportional allocation, so this is reachable with a lopsided distribution (e.g. a heavily weighted section with few questions). Since this helper is the contract FEAT-002 will consume, redistributing the leftover is worth fixing now.

### Test adequacy

The validator tests are genuinely behavioral: they assert on `violation.field()` and on message content (TC-005 checks the current total "80" appears; TC-012 checks "pool"), not just counts, so they would catch a rule wired to the wrong field. TC-013 asserts ≥3 aggregated violations, matching AC-021. The service integration tests cover create/add/edit/delete/not-found/pass-percentage/exam-length and the bootstrap invariants, and TC-019 correctly verifies the prior version survives an invalid edit by reloading.

Not tested: the service `create` *rejection* path (only the happy path and the validator are tested — nothing asserts that `create` throws on an invalid aggregate, which is what protects the bootstrap); the pass-percentage out-of-range case (the endpoint bug above); any controller-layer test (no MockMvc/`@WebMvcTest`), which is why the `createDraft` and query-param holes are invisible to the suite; AC-004 (existing/new section name, UI-only, author-flagged); AC-025 (list page, exercised only by context load, author-flagged). The AC-004/AC-025 gaps are disclosed in the manifest and acceptable to defer; the missing controller and pass-percentage tests are what let real defects through.

### Security & injection posture

Server-side validation is the design's stated control (REQ-050); the validator honors it, though the enforcement gaps above weaken it in practice for pass-percentage and aggregate save. JPA uses parameterized queries throughout (derived query methods only), so no SQL injection surface. Thymeleaf `th:text` escapes by default and all user content (titles, question text, options, explanations) is rendered via `th:text`, not `th:utext`, so stored XSS is not a concern — consistent with the design's §11. The inline JS builds the request body with `JSON.stringify` over field values and injects server responses via `textContent`, both of which avoid HTML injection; the only values interpolated into the fetch URL are `sectionId`/`id` sourced from server-rendered numeric ids, not free-form user input. No auth is expected this release (out of scope). No secrets or PII handled.

</details>

<details>
<summary>File map</summary>

- `entity/Certificate.java`, `Section.java`, `Question.java`, `Option.java` — JPA aggregate; helpers `totalWeight`, `totalQuestions`, `correctCount`, `correctOption`. Matches DES-001..003 data model.
- `repository/CertificationRepository.java` — adds `existsByTitle`/`findByTitle` (defined but unused). `QuestionRepository.java` — plain JPA repo.
- `dto/CertificationRequest.java`, `SectionRequest.java`, `QuestionRequest.java` — bean-validated inbound models. `CertificationResponse.java`, `QuestionResponse.java` — outbound records.
- `util/CertificationValidator.java` — DES-006 aggregate validation + DES-011 question validation + DES-012 proportional selection.
- `service/CertificationService.java` — orchestration; `create` (validates) vs `createDraft` (does not); `setQuestionsToAsk` validates + rolls back; `setPassPercentage` does not.
- `controller/CertificationController.java` — Thymeleaf MVC pages + form posts. `QuestionController.java` — REST question CRUD.
- `config/CertificationDataLoader.java` — seeds two valid AWS sample certs via `create`.
- `exception/CertificationNotFoundException.java`, `CertificationValidationException.java`, `GlobalExceptionHandler.java` — structured errors; missing DataIntegrity/ConstraintViolation/HttpMessageNotReadable handlers.
- `templates/certifications/list.html`, `form.html`, `view.html` — authoring UI; view has inline JS calling the REST API.
- `test/.../CertificationValidatorTest.java` (14), `CertificationServiceTest.java` (11) — plus 1 context-load test = 26 passing.

No diff available (not a git repository); reviewed against the working tree.

</details>
