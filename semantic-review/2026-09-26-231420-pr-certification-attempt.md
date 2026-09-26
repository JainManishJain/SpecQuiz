# Certification Attempt — re-review after form-binding blocker remediation

Second-pass review of FEAT-002 verifying the scoring blocker is genuinely fixed. The prior blocker: `exam.html` rendered a hidden `answers[qid]=""` input before the radios of the same name, and because `answers` binds to a `Map<Long,Long>` Spring kept the first (empty) value, so every answered question scored incorrect — real submissions always scored zero. The remediation splits the served-question set away from the answers map: `AttemptSubmission` gains a `List<Long> servedQuestionIds`, `exam.html` renders a hidden `servedQuestionIds` per question and drops the empty `answers[qid]` input so radios are the sole source, and `AttemptService.score` iterates `servedQuestionIds` (falling back to `answers.keySet()` only when empty), counting a served id absent from `answers` as incorrect. Suite grew to 56 tests, all green under `./gradlew clean test`.

Watch for: (1) the blocker is resolved — the empty hidden input is gone and radios are the only `answers[qid]` source, so no binding collision remains; TC-013 asserts the corrected scoring against the real `AttemptResult` model (`correctCount=1`, `score=100`), not a view-name check (confirmed). (2) The `servedQuestionIds.isEmpty()` fallback to `answers.keySet()` re-opens the exact denominator-shrink hole the fix closed, for any submit that omits served ids; the shipped template never does, so it is a latent minor, not a live defect (confirmed). (3) TC-013 and TC-014 each serve a single question, so neither exercises the multi-question form ordering that actually triggered the original Spring map-binding collision (confirmed).

**Verdict**: APPROVED

## High-level view

The blocker is closed at the layer that caused it. The exam form no longer emits an empty `answers[qid]` hidden input, so there is no first-value collision for Spring's map binder to keep; the radios are the only inputs named `answers[qid]`, and a separate hidden `servedQuestionIds` per question carries the authoritative denominator. A correct answer now binds and scores correct.

Scoring moved its source of truth from `answers.keySet()` to `servedQuestionIds`, which decouples the denominator from what the client chose to answer. A served id with no answer resolves to a null selection and counts incorrect, so AC-010 holds through the new iteration path.

The regression surface from the DTO/template/service change is small and the aggregate is intact, but the scorer keeps a fallback: when `servedQuestionIds` is empty it reverts to `answers.keySet()`. That fallback is precisely the pre-fix behavior — a client that posts answers but no served ids gets a denominator equal to the number of answered questions, so unanswered questions vanish from the score. The shipped template always sends served ids, so nothing today hits it, but it is a client-controllable soft spot in security-relevant scoring code.

The two previously-untested minors are now covered: TC-014 asserts served-set authority (a served-but-unanswered question counts incorrect via the model), and TC-015 asserts AC-006 proportional selection (80/20 → 4+1) for this feature directly. The remaining gap is depth, not breadth: the regression guards operate on a single served question, so the multi-question ordering that originally produced the collision is not directly reproduced by a test.

<details>
<summary>Issues (2)</summary>

1. **`answers.keySet()` fallback re-opens the denominator-shrink hole** — `AttemptService.score` (line 72-74) falls back to `answers.keySet()` when `servedQuestionIds` is empty, which is the pre-fix behavior: a submit with answers but no served ids drops unanswered questions from the denominator. The shipped template always sends served ids so it is not live, but consider failing closed (reject/empty result) instead of silently trusting the answers map, or document why the fallback is safe. (likely, non-blocking)
2. **Regression guards use a single served question** — TC-013/TC-014 serve one question each, so the multi-question form ordering that triggered the original Spring map-binding collision is not directly reproduced. Add a variant that serves all 5 with a mix of answered/unanswered to lock the ordering regression. (confirmed, non-blocking)

</details>

<details>
<summary>Details</summary>

### Blocker — empty hidden input removed, served ids carry the denominator — RESOLVED

`exam.html` no longer renders the empty `answers[qid]` hidden input that caused the collision. Each question now emits one hidden `servedQuestionIds` field and the radios are the only inputs named `answers[qid]`:

```html
<input type="hidden" name="servedQuestionIds" th:value="${q.questionId()}"/>
<label class="option" th:each="o : ${q.options()}">
    <input type="radio" th:name="'answers[' + ${q.questionId()} + ']'" th:value="${o.optionId()}"/>
```

Because there is no longer a same-named empty input preceding the radios, Spring's `Map<Long,Long>` binder receives only the radio's value for an answered question, so a correct selection binds to the correct option id instead of the empty string it previously kept. `AttemptService.score` iterates `submission.getServedQuestionIds()` for the served set and looks up the answer per id (lines 72-89), and `total = servedIds.size()` fixes the denominator to the served set rather than the answered set.

TC-013 asserts the corrected behavior against the real model, not the view name: it posts `servedQuestionIds` plus `answers[qid]` for the answered question, then pulls `AttemptResult` off the `ModelAndView` and asserts `totalQuestions()==1`, `correctCount()==1`, `scorePercentage()==100`. A correct answer that previously scored zero now scores 100 through the full controller → service path, so the guard would fail if the collision returned.

### AC-010 — served-but-unanswered still counts incorrect — RESOLVED

A served id absent from `answers` yields a null `selectedOptionId`, so `isCorrect` is false and the id still contributes to `total` — the unanswered case counts and is not dropped from the denominator. TC-014 posts a `servedQuestionIds` param with no matching `answers[qid]` and asserts `totalQuestions()==1`, `correctCount()==0` via the model — the unanswered path holds through the form, and the denominator is not shrunk by the missing answer. TC-004 covers the same at the service layer with a mixed 2-correct/3-unanswered submission scoring 40 and failing.

### Regression from the DTO/template/service change — the fallback is the one soft spot

The aggregate and selection paths are intact: `startAttempt` and selection are unchanged, and the existing service/MVC tests (TC-001..TC-012) still pass, so the DTO field addition and template rewrite did not disturb the read path. The one behavioral risk introduced by the change is the fallback in `score`:

```java
List<Long> servedIds = submission.getServedQuestionIds().isEmpty()
        ? new ArrayList<>(answers.keySet())
        : submission.getServedQuestionIds();
```

This is the pre-fix denominator. Any submission that carries answers but no `servedQuestionIds` scores against the answered set only, so unanswered questions disappear from both numerator and denominator — the same client-influenceable outcome the remediation set out to remove, just gated behind an empty served list. The shipped `exam.html` always emits a served id per question, so no rendered form reaches this branch, which keeps it a latent minor rather than a live regression. But scoring is the security-relevant boundary here (the design explicitly chose server-side scoring so "client cannot influence the outcome"), and this branch lets a hand-crafted post do exactly that. Failing closed — treating an empty served set as a zero-question or rejected submission — would match the stated posture better than silently trusting `answers.keySet()`.

### Previously-untested minors — now covered

Served-set authority (the denominator comes from served ids, not the answers map) is now asserted end-to-end by TC-014, which is the case that distinguishes the two sources: with a served id and no answer, only served-set iteration produces `total==1`; keyset iteration would produce `total==0`. AC-006 proportional selection for this feature is covered by TC-015, which builds an 80/20 certificate asking 5 and asserts exactly 4 from the 80% section and 1 from the 20% section via the served `AttemptView`, rather than relying on the Certification Management suite. Both minors from the prior review are closed with behavioral assertions.

The residual gap is coverage depth on the regression itself: TC-013 and TC-014 each serve exactly one question. The original defect manifested with multiple same-named `answers[qid]` groups interleaved with the empty hidden inputs across several questions on one page; a single-question form is the weakest reproduction of that layout. A test that serves all five, answers some and leaves others blank, and asserts the per-id correctness plus the count would directly pin the multi-question ordering the bug lived in.

</details>

<details>
<summary>File map</summary>

- `dto/AttemptSubmission.java` — adds `List<Long> servedQuestionIds` alongside `Map<Long,Long> answers`; javadoc explains the authoritative-served-set rationale.
- `service/AttemptService.java` — `score` iterates `servedQuestionIds` (lines 72-89), falls back to `answers.keySet()` when empty (line 72-74), `total = servedIds.size()`.
- `templates/attempts/exam.html` — hidden `servedQuestionIds` per question; empty `answers[qid]` hidden input removed; radios are the sole `answers[qid]` source.
- `controller/AttemptController.java` — unchanged binding of `@ModelAttribute AttemptSubmission` on `POST /attempts/submit`.
- `test/.../controller/AttemptControllerTest.java` — TC-013 (real-form submit scores correct via model), TC-014 (served-but-unanswered counts incorrect via model).
- `test/.../service/AttemptServiceTest.java` — TC-015 (80/20 serves 4+1); submission helper now sets `servedQuestionIds` from the answer keys.

Not a git repository; reviewed against the working tree. Suite verified with `./gradlew clean test` — 56 tests, 0 failures, 0 errors.

</details>
