package com.specquiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.specquiz.dto.AttemptResult;
import com.specquiz.dto.AttemptSubmission;
import com.specquiz.dto.AttemptView;
import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.exception.CertificationNotFoundException;

/**
 * Integration tests for {@link AttemptService}. Each test names the AC it covers.
 */
@SpringBootTest
@Transactional
class AttemptServiceTest {

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private CertificationService certificationService;

    private Certificate cert;

    @BeforeEach
    void setUp() {
        // A certificate with one 100%-weight section and 5 questions; pass mark 60; ask all 5.
        Certificate c = new Certificate("Attempt Cert " + System.nanoTime(), 60, 5);
        Section s = new Section("All", 100);
        for (int i = 0; i < 5; i++) {
            s.addQuestion(q("Q" + i));
        }
        c.addSection(s);
        cert = certificationService.create(c);
    }

    private Question q(String tag) {
        Question question = new Question("Question " + tag, "Explanation " + tag);
        question.addOption(new Option("correct-" + tag, true));
        question.addOption(new Option("w1-" + tag, false));
        question.addOption(new Option("w2-" + tag, false));
        question.addOption(new Option("w3-" + tag, false));
        return question;
    }

    private Map<Long, Long> answersAllCorrect(Certificate c, int howMany) {
        Map<Long, Long> answers = new LinkedHashMap<>();
        List<Question> questions = c.getSections().get(0).getQuestions();
        for (int i = 0; i < howMany && i < questions.size(); i++) {
            Question qq = questions.get(i);
            answers.put(qq.getId(), qq.correctOption().getId());
        }
        return answers;
    }

    private AttemptSubmission submission(Certificate c, Map<Long, Long> answers) {
        AttemptSubmission sub = new AttemptSubmission();
        sub.setCandidateName("Alice");
        sub.setCertificateId(c.getId());
        // Served ids are the authoritative set; default to the keys the test built.
        sub.setServedQuestionIds(new java.util.ArrayList<>(answers.keySet()));
        // Only non-null answers populate the answers map (mirrors radios being the only source).
        Map<Long, Long> nonNull = new LinkedHashMap<>();
        answers.forEach((qid, oid) -> {
            if (oid != null) {
                nonNull.put(qid, oid);
            }
        });
        sub.setAnswers(nonNull);
        return sub;
    }

    // TC-001 — AC-001, AC-003, AC-005, AC-006: starting a valid attempt serves questionsToAsk questions.
    @Test
    void startAttemptServesConfiguredCount() {
        AttemptView view = attemptService.startAttempt("Alice", cert.getId());
        assertThat(view.candidateName()).isEqualTo("Alice");
        assertThat(view.questions()).hasSize(5); // questionsToAsk
        assertThat(view.questions().get(0).options()).hasSize(4);
    }

    // TC-002 — AC-003: starting an attempt for a missing certificate is a not-found error.
    @Test
    void startAttemptMissingCertificateThrows() {
        assertThatThrownBy(() -> attemptService.startAttempt("Alice", 999999L))
                .isInstanceOf(CertificationNotFoundException.class);
    }

    // TC-003 — AC-009, AC-011: all-correct answers score 100 and PASS.
    @Test
    void allCorrectPasses() {
        AttemptResult result = attemptService.score(submission(cert, answersAllCorrect(cert, 5)));
        assertThat(result.totalQuestions()).isEqualTo(5);
        assertThat(result.correctCount()).isEqualTo(5);
        assertThat(result.scorePercentage()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
    }

    // TC-004 — AC-010, AC-012: unanswered questions count as incorrect and can fail the attempt.
    @Test
    void unansweredCountAsIncorrectAndCanFail() {
        Map<Long, Long> answers = new LinkedHashMap<>();
        List<Question> questions = cert.getSections().get(0).getQuestions();
        // Answer only 2 of 5 correctly; leave 3 unanswered (key present, value null).
        for (int i = 0; i < 5; i++) {
            Question qq = questions.get(i);
            answers.put(qq.getId(), i < 2 ? qq.correctOption().getId() : null);
        }
        AttemptResult result = attemptService.score(submission(cert, answers));
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.scorePercentage()).isEqualTo(40); // 2/5
        assertThat(result.passed()).isFalse(); // below 60
    }

    // TC-005 — AC-011 boundary: score exactly equal to pass percentage passes.
    @Test
    void scoreEqualToPassMarkPasses() {
        Map<Long, Long> answers = new LinkedHashMap<>();
        List<Question> questions = cert.getSections().get(0).getQuestions();
        // 3 of 5 correct = 60% == pass mark.
        for (int i = 0; i < 5; i++) {
            Question qq = questions.get(i);
            answers.put(qq.getId(), i < 3 ? qq.correctOption().getId() : null);
        }
        AttemptResult result = attemptService.score(submission(cert, answers));
        assertThat(result.scorePercentage()).isEqualTo(60);
        assertThat(result.passed()).isTrue(); // >= pass mark
    }

    // TC-006 — AC-014, AC-015: result items flag correctness and carry the explanation + correct option.
    @Test
    void resultItemsCarryFeedback() {
        AttemptResult result = attemptService.score(submission(cert, answersAllCorrect(cert, 5)));
        assertThat(result.items()).hasSize(5);
        AttemptResult.ResultItem first = result.items().get(0);
        assertThat(first.correct()).isTrue();
        assertThat(first.explanation()).startsWith("Explanation");
        assertThat(first.options()).anyMatch(AttemptResult.OptionResult::correct);
        assertThat(first.options()).anyMatch(AttemptResult.OptionResult::selected);
    }

    // TC-007 — REQ-052: a submitted answer for a non-existent question is scored as incorrect, not an error.
    @Test
    void missingQuestionScoredIncorrect() {
        Map<Long, Long> answers = new LinkedHashMap<>();
        answers.put(888888L, 1L); // question id that does not exist
        AttemptResult result = attemptService.score(submission(cert, answers));
        assertThat(result.totalQuestions()).isEqualTo(1);
        assertThat(result.correctCount()).isEqualTo(0);
        assertThat(result.passed()).isFalse();
    }

    // TC-015 — AC-006: served questions are drawn proportionally to section weight for this feature.
    @Test
    void startAttemptSelectsProportionally() {
        // 80/20 certificate asking 5 questions -> 4 from the 80% section, 1 from the 20% section.
        Certificate c = new Certificate("Proportional Attempt " + System.nanoTime(), 60, 5);
        Section big = new Section("Big", 80);
        for (int i = 0; i < 10; i++) {
            big.addQuestion(q("BIG" + i));
        }
        Section small = new Section("Small", 20);
        for (int i = 0; i < 10; i++) {
            small.addQuestion(q("SMALL" + i));
        }
        c.addSection(big);
        c.addSection(small);
        Certificate saved = certificationService.create(c);

        AttemptView view = attemptService.startAttempt("Alice", saved.getId());
        assertThat(view.questions()).hasSize(5);
        long fromBig = view.questions().stream().filter(qv -> qv.text().contains("BIG")).count();
        long fromSmall = view.questions().stream().filter(qv -> qv.text().contains("SMALL")).count();
        assertThat(fromBig).isEqualTo(4);
        assertThat(fromSmall).isEqualTo(1);
    }
}
