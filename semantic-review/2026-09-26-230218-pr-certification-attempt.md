# Stateless certification-attempt flow scores every real submission as zero

The change adds the exam-taking side of SpecQuiz (FEAT-002): a start page, a one-page exam, and a scored result, layered statelessly over the existing certificate aggregate. Question selection reuses `CertificationValidator.selectProportionally` through a new `CertificationService.selectQuestionsForAttempt`, and scoring recomputes correctness server-side from persisted options, so the client never sends correctness. The DTOs, service math, and templates are clean and the design intent (REQ-050 server-side scoring, REQ-052 missing-question-as-incorrect, REQ-053 rule reuse) is faithfully coded. The single mechanism that ties the exam form to the scorer, however, is broken.

Watch for: the exam form's hidden `answers[qid]=""` input is rendered *before* the radios with the same name, and Spring's `Map<Long,Long>` binding keeps the **first** value for a key — so the empty string always wins and every answered question binds to null and scores as incorrect (**confirmed** by a real-form MockMvc probe: 3 correct answers scored 0/5, FAIL). The shipped tests never send the hidden param, so all 12 pass while the real UI is unwinnable.

**Verdict**: NEEDS_CHANGES

## High-level view

The scoring math is correct where it runs: `Math.round(correct/total*100)`, a boundary-inclusive `score >= passPercentage` check (TC-005 proves 60==60 passes), a `total==0` divide-by-zero guard, and server-side recomputation from `question.correctOption()` so a client cannot forge a pass. The problem is upstream of the math: the answers never reach the scorer intact.

The defect lives entirely in the form-to-map binding contract. `exam.html` emits, per question, a hidden `<input name="answers[123]" value="">` followed by radio inputs sharing `name="answers[123]"`. The intent is that unanswered questions still submit their key (so they can be counted incorrect) while answered questions override the empty default. Spring MVC does the opposite: for a repeated request parameter bound into a `Map`, it takes the first occurrence, which is the empty hidden value. The empty string converts to a null `Long`, and that null is what the scorer sees for every answered question. A submission of all-correct answers scores zero. This is order-dependent and confirmed by reversing the parameter order in a probe (radio-first yields the right score).

The scorer derives the served-question set from `answers.keySet()` rather than from a separate list of served ids. That happens to work only because the hidden inputs inject every served key; it also means the "served questions" are whatever the client posted, so a client that drops keys shrinks its own denominator. Given the stateless design this is a known trade-off, but it is worth flagging because the same hidden-input mechanism that breaks scoring is also the only thing guaranteeing the denominator is right.

Test coverage is structurally complete against the AC table but has a hole precisely where the bug is: every controller test posts answers as direct `answers[qid]=value` params, never reproducing the hidden-empty-then-radio ordering a browser sends. The manifest openly notes the HTML structure isn't asserted field-by-field; the consequence is that the one integration path that would have caught a total scoring failure is absent.

<details>
<summary>Issues (5)</summary>

1. **Empty hidden value wins the map binding (blocker)** — `exam.html:27` renders `answers[qid]=""` before the radios; Spring keeps the first value so answered questions bind to null and score as incorrect. Remove the hidden shared-name input and carry served ids separately (e.g. one hidden `servedQuestionIds` list the scorer iterates), so radios are the only source of `answers[qid]`.
2. **No integration test exercises the real form ordering (major)** — controller tests send only the radio param, so the blocker passes CI. Add a `POST /attempts/submit` test that sends the hidden empty param first and a radio value second for the same question, asserting it scores correct.
3. **Served set derived from client-submitted keys (minor)** — `AttemptService.score` uses `answers.keySet()` as the served-question set, so the denominator and the AC-005 count are whatever the client posts. Drive scoring from an authoritative served-id list carried in the form.
4. **AC-006 proportional selection not tested for this feature (minor)** — coverage defers to Certification Management's tests; `selectQuestionsForAttempt` has no test asserting the delegation. Add a thin test that a multi-section certificate serves per-section counts proportional to weight.
5. **AC-008 submit control / AC-007 radio structure unasserted (info)** — realized only in `exam.html` and not verified. Optionally assert the rendered exam contains radio inputs and a submit button.

</details>

<details>
<summary>Details</summary>

### The hidden-input + radio binding is order-sensitive and fails closed to zero

This is the mechanism the whole feature hinges on, and it is inverted. `exam.html` renders, for each served question:

```html
<input type="hidden" th:name="'answers[' + ${q.questionId()} + ']'" value=""/>
<label class="option" th:each="o : ${q.options()}">
    <input type="radio" th:name="'answers[' + ${q.questionId()} + ']'" th:value="${o.optionId()}"/>
```

A browser submits the hidden `answers[123]=` first (DOM order) and, for the checked radio, `answers[123]=456` second. Spring binds a repeated parameter into `Map<Long,Long>` by taking the **first** value for the key. The first value is the empty string, which converts to a null `Long`. So for every question the candidate actually answered, `answers.get(questionId)` is null, and the scorer's `selectedOptionId != null && selectedOptionId.equals(correctOptionId)` (`AttemptService.java:84`) is false.

Confirmed empirically with a MockMvc probe that reproduces the real ordering: a 5-question exam with 3 correct answers scored `total=5 correct=0 score=0 passed=false`. Reversing the parameter order (radio value first, empty second) in the same probe produced `correct=3 score=60 passed=true`, isolating parameter ordering as the sole cause. The failure mode is total: no submission made through the actual UI can ever score above zero, so every candidate fails regardless of answers. (**confirmed**)

The intent behind the hidden input — keep the served key present so unanswered questions are counted incorrect (AC-010) — is sound, but sharing the radio's name defeats it. A robust fix separates the two concerns: carry the served question ids in their own hidden field (a repeated `servedQuestionIds` param or a single CSV), let the radios be the *only* inputs named `answers[qid]`, and have the scorer iterate the served-id list, treating a key absent from `answers` as unanswered. That preserves statelessness and the AC-010 semantics without the collision. If the hidden default must stay, it has to be rendered *after* the radios and the binding switched to last-value-wins, which is fragile and not the default — the separate-list approach is the clean fix.

### Scoring math and pass boundary

`score = total == 0 ? 0 : Math.round(correctCount * 100.0f / total)` (`AttemptService.java:100`) and `passed = score >= certificate.getPassPercentage()` (`AttemptService.java:101`) implement AC-009/AC-011/AC-012 boundary-inclusive, and TC-005 (3/5==60 passes) and TC-004 (2/5==40 fails) assert `scorePercentage()` and `passed()` directly. One behavior worth knowing: the score is rounded before the comparison, so a raw 59.5% rounds to 60 and passes a 60 mark — consistent with the spec's `round(...)` definition, not a defect.

### Server-side correctness and XSS posture

Correctness is recomputed from `question.correctOption()` against persisted options (`AttemptService.java:82-84`); the client submits only selected option ids, never correctness, so a forged pass is not possible through the submit endpoint (REQ-050). `AttemptView` omits correct flags, so the exam page never ships the answer key. Candidate name and all question/option/explanation text render through escaped `th:text` on both `exam.html` and `result.html`, so stored option text cannot inject script. These are the security-relevant surfaces the change touches; no gap found.

### Missing-question handling (REQ-052)

`score` builds a `questionId -> Question` lookup from the live certificate and, when a submitted key is absent, adds a placeholder `ResultItem` marked incorrect rather than throwing (`AttemptService.java:78-80`). TC-007 asserts a bogus question id scores incorrect with no error, and it counts toward the denominator. This matches DES-005 step 2a and REQ-052.

### Test coverage gap aligns with the bug

The service tests build the `answers` map directly in Java, and the controller tests post `answers[qid]=value` params one-to-one — neither reproduces a browser's hidden-empty-then-radio submission. That is exactly the path the blocker lives on, so a complete-looking AC table (all 15 mapped) coexists with a feature that cannot score a real submission. The manifest's own note that "HTML structure is not asserted field-by-field" is the tell. Beyond that, AC-006's proportional selection for this feature is delegated to Certification Management's tests with no local assertion that `selectQuestionsForAttempt` actually splits across sections by weight; with a single 100%-weight section in every fixture, a broken delegation would not be caught here.

</details>

<details>
<summary>File map</summary>

- `dto/StartAttemptRequest.java` — start form DTO, `@NotBlank` name + `@NotNull` certificateId.
- `dto/AttemptView.java` — exam-page view; deliberately omits correct flags.
- `dto/AttemptSubmission.java` — inbound `answers` `Map<Long,Long>`; the binding target at the center of the blocker.
- `dto/AttemptResult.java` — result DTO with per-question `ResultItem`/`OptionResult`.
- `service/AttemptService.java` — `startAttempt` (selection) and `score` (server-side scoring); scoring math correct, denominator derived from submitted keys.
- `service/CertificationService.java` — adds `selectQuestionsForAttempt` delegating to `selectProportionally` (REQ-053 satisfied).
- `controller/AttemptController.java` — `GET /attempts/new`, `POST /attempts`, `POST /attempts/submit`.
- `templates/attempts/start.html` — name + certification dropdown (AC-004).
- `templates/attempts/exam.html` — **contains the hidden-empty + radio same-name collision (blocker).**
- `templates/attempts/result.html` — score, PASS/FAIL, per-question feedback; escaped output.
- `test/.../AttemptServiceTest.java` (7), `test/.../AttemptControllerTest.java` (5) — all pass, but none exercise the real-form parameter ordering.

No VCS in this workspace; review is of the working-tree files as listed above.

</details>
