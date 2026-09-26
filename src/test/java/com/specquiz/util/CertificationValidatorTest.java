package com.specquiz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.exception.CertificationValidationException;

/**
 * Unit tests for {@link CertificationValidator}. Each test names the acceptance
 * criterion (AC) it covers.
 */
class CertificationValidatorTest {

    private final CertificationValidator validator = new CertificationValidator();

    private Question validQuestion(String text) {
        Question q = new Question(text, "because " + text);
        q.addOption(new Option("correct", true));
        q.addOption(new Option("w1", false));
        q.addOption(new Option("w2", false));
        q.addOption(new Option("w3", false));
        return q;
    }

    private Certificate validCertificate() {
        Certificate cert = new Certificate("Sample Cert", 70, 5);
        Section a = new Section("A", 60);
        for (int i = 0; i < 3; i++) {
            a.addQuestion(validQuestion("A-Q" + i));
        }
        Section b = new Section("B", 40);
        for (int i = 0; i < 2; i++) {
            b.addQuestion(validQuestion("B-Q" + i));
        }
        cert.addSection(a);
        cert.addSection(b);
        return cert;
    }

    // TC-001 — AC-001: a fully valid certificate passes validation.
    @Test
    void validCertificatePasses() {
        Certificate cert = validCertificate();
        validator.validateCertificate(cert);
        assertThat(validator.collectCertificateViolations(cert)).isEmpty();
    }

    // TC-002 — AC-002: blank title is rejected.
    @Test
    void blankTitleRejected() {
        Certificate cert = validCertificate();
        cert.setTitle("  ");
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("title"));
    }

    // TC-003 — AC-003: pass percentage out of 1..100 is rejected.
    @Test
    void invalidPassPercentageRejected() {
        Certificate cert = validCertificate();
        cert.setPassPercentage(0);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("passPercentage"));
    }

    // TC-004 — AC-005: a section weight that is not a multiple of 20 is rejected.
    @Test
    void weightNotMultipleOf20Rejected() {
        Certificate cert = validCertificate();
        cert.getSections().get(0).setWeight(30);
        cert.getSections().get(1).setWeight(70);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("section.weight"));
    }

    // TC-005 — AC-006: section weights not summing to 100 is rejected with the current total.
    @Test
    void weightsNotSummingTo100Rejected() {
        Certificate cert = validCertificate();
        cert.getSections().get(1).setWeight(20); // total 80
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("sections") && v.message().contains("80"));
    }

    // TC-006 — AC-007: a certificate with no sections is rejected.
    @Test
    void noSectionsRejected() {
        Certificate cert = new Certificate("Empty", 70, 5);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("sections"));
    }

    // TC-007 — AC-009: a question without exactly four options is rejected.
    @Test
    void questionWithoutFourOptionsRejected() {
        Question q = new Question("Q", "expl");
        q.addOption(new Option("only", true));
        assertThat(validator.collectQuestionViolations(q))
                .anyMatch(v -> v.field().equals("question.options"));
    }

    // TC-008 — AC-010: a question without exactly one correct option is rejected.
    @Test
    void questionWithoutExactlyOneCorrectRejected() {
        Question q = new Question("Q", "expl");
        q.addOption(new Option("a", true));
        q.addOption(new Option("b", true));
        q.addOption(new Option("c", false));
        q.addOption(new Option("d", false));
        assertThat(validator.collectQuestionViolations(q))
                .anyMatch(v -> v.field().equals("question.correct"));
    }

    // TC-009 — AC-011: blank question text or explanation is rejected.
    @Test
    void blankTextOrExplanationRejected() {
        Question q = validQuestion("Q");
        q.setText("  ");
        q.setExplanation("");
        List<CertificationValidationException.Violation> violations = validator.collectQuestionViolations(q);
        assertThat(violations).anyMatch(v -> v.field().equals("question.text"));
        assertThat(violations).anyMatch(v -> v.field().equals("question.explanation"));
    }

    // TC-010 — AC-020: fewer than five questions total is rejected.
    @Test
    void fewerThanFiveQuestionsRejected() {
        Certificate cert = new Certificate("Few", 70, 5);
        Section a = new Section("A", 100);
        for (int i = 0; i < 3; i++) {
            a.addQuestion(validQuestion("Q" + i));
        }
        cert.addSection(a);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("questions"));
    }

    // TC-011 — AC-017: questionsToAsk below 5 is rejected.
    @Test
    void questionsToAskBelowFiveRejected() {
        Certificate cert = validCertificate();
        cert.setQuestionsToAsk(4);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("questionsToAsk"));
    }

    // TC-012 — AC-018: questionsToAsk greater than the pool size is rejected.
    @Test
    void questionsToAskAbovePoolRejected() {
        Certificate cert = validCertificate(); // pool = 5
        cert.setQuestionsToAsk(6);
        assertThat(validator.collectCertificateViolations(cert))
                .anyMatch(v -> v.field().equals("questionsToAsk") && v.message().contains("pool"));
    }

    // TC-013 — AC-021: multiple violations are all reported together.
    @Test
    void multipleViolationsAggregated() {
        Certificate cert = validCertificate();
        cert.setTitle("");
        cert.setPassPercentage(0);
        cert.getSections().get(1).setWeight(20); // total 80
        assertThatThrownBy(() -> validator.validateCertificate(cert))
                .isInstanceOf(CertificationValidationException.class)
                .satisfies(ex -> assertThat(((CertificationValidationException) ex).getViolations()).hasSizeGreaterThanOrEqualTo(3));
    }

    // TC-014 — AC-019: proportional selection draws in proportion to weight and sums to N.
    @Test
    void proportionalSelectionRespectsWeights() {
        Certificate cert = new Certificate("Prop", 70, 5);
        Section a = new Section("A", 80);
        for (int i = 0; i < 10; i++) {
            a.addQuestion(validQuestion("A" + i));
        }
        Section b = new Section("B", 20);
        for (int i = 0; i < 10; i++) {
            b.addQuestion(validQuestion("B" + i));
        }
        cert.addSection(a);
        cert.addSection(b);

        List<Question> selected = validator.selectProportionally(cert, new Random(42));
        assertThat(selected).hasSize(5); // 80% of 5 = 4 from A, 20% = 1 from B
        long fromA = selected.stream().filter(q -> q.getText().startsWith("A")).count();
        long fromB = selected.stream().filter(q -> q.getText().startsWith("B")).count();
        assertThat(fromA).isEqualTo(4);
        assertThat(fromB).isEqualTo(1);
    }

    // TC-040 — F-005 / AC-019: a lopsided distribution still returns exactly N by
    // redistributing the shortfall to sections with spare questions.
    @Test
    void proportionalSelectionRedistributesShortfall() {
        Certificate cert = new Certificate("Lopsided", 70, 5);
        // Section A is weighted 80% (allocated 4) but only has 1 question -> shortfall of 3.
        Section a = new Section("A", 80);
        a.addQuestion(validQuestion("A0"));
        // Section B is weighted 20% (allocated 1) but has plenty to absorb the shortfall.
        Section b = new Section("B", 20);
        for (int i = 0; i < 10; i++) {
            b.addQuestion(validQuestion("B" + i));
        }
        cert.addSection(a);
        cert.addSection(b);

        List<Question> selected = validator.selectProportionally(cert, new Random(7));
        assertThat(selected).hasSize(5); // still exactly N despite A's tiny pool
        long fromA = selected.stream().filter(q -> q.getText().startsWith("A")).count();
        assertThat(fromA).isEqualTo(1); // took all of A's 1 question
    }
}
