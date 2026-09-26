package com.specquiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.specquiz.dto.QuestionRequest;
import com.specquiz.entity.Certificate;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.exception.CertificationNotFoundException;
import com.specquiz.exception.CertificationValidationException;
import com.specquiz.repository.CertificationRepository;

/**
 * Integration tests for {@link CertificationService} and startup bootstrap.
 * Each test names the acceptance criterion (AC) it covers.
 */
@SpringBootTest
@Transactional // roll back each test so the shared in-memory DB stays isolated between tests
class CertificationServiceTest {

    @Autowired
    private CertificationService service;

    @Autowired
    private CertificationRepository repository;

    private QuestionRequest questionRequest(int correctIndex) {
        QuestionRequest r = new QuestionRequest();
        r.setText("What is 2 + 2?");
        r.setExplanation("Basic arithmetic.");
        r.setOptions(List.of("4", "3", "5", "22"));
        r.setCorrectOptionIndex(correctIndex);
        return r;
    }

    private Certificate persistValidCertificate(String title) {
        Certificate cert = new Certificate(title, 70, 5);
        Section a = new Section("Alpha", 60);
        for (int i = 0; i < 3; i++) {
            a.addQuestion(sampleQuestion("A" + i));
        }
        Section b = new Section("Beta", 40);
        for (int i = 0; i < 2; i++) {
            b.addQuestion(sampleQuestion("B" + i));
        }
        cert.addSection(a);
        cert.addSection(b);
        return service.create(cert);
    }

    private Question sampleQuestion(String tag) {
        Question q = new Question("Q-" + tag, "expl-" + tag);
        q.addOption(new com.specquiz.entity.Option("correct", true));
        q.addOption(new com.specquiz.entity.Option("w1", false));
        q.addOption(new com.specquiz.entity.Option("w2", false));
        q.addOption(new com.specquiz.entity.Option("w3", false));
        return q;
    }

    // TC-015 — AC-023, AC-024: bootstrap loads >=2 valid sample certifications on startup.
    @Test
    void bootstrapLoadsAtLeastTwoValidCertifications() {
        // Assert against the known bootstrapped sample titles only, so data committed by
        // other (non-transactional) tests in the shared context cannot affect this check.
        List<Certificate> samples = service.findAll().stream()
                .filter(c -> c.getTitle().startsWith("AWS Certified"))
                .toList();
        assertThat(samples).hasSizeGreaterThanOrEqualTo(2);
        for (Certificate c : samples) {
            assertThat(c.getSections()).isNotEmpty();
            assertThat(c.totalQuestions()).isGreaterThanOrEqualTo(5);
            assertThat(c.totalWeight()).isEqualTo(100);
        }
    }

    // TC-016 — AC-001: creating a valid certificate persists it.
    @Test
    void createValidCertificatePersists() {
        Certificate saved = persistValidCertificate("Create-Test-Cert");
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    // TC-017 — AC-008: adding a valid question grows the section pool.
    @Test
    void addValidQuestionSucceeds() {
        Certificate cert = persistValidCertificate("Add-Q-Cert");
        Long sectionId = cert.getSections().get(0).getId();
        int before = cert.getSections().get(0).getQuestions().size();

        service.addQuestion(sectionId, questionRequest(0));

        Certificate reloaded = service.getCertificate(cert.getId());
        int after = reloaded.getSections().stream()
                .filter(s -> s.getId().equals(sectionId)).findFirst().get()
                .getQuestions().size();
        assertThat(after).isEqualTo(before + 1);
    }

    // TC-018 — AC-012: editing a question with valid data updates it.
    @Test
    void editQuestionUpdates() {
        Certificate cert = persistValidCertificate("Edit-Q-Cert");
        Question original = cert.getSections().get(0).getQuestions().get(0);

        QuestionRequest edit = questionRequest(1);
        edit.setText("Edited text");
        Question updated = service.editQuestion(original.getId(), edit);

        assertThat(updated.getText()).isEqualTo("Edited text");
        assertThat(updated.correctOption().getText()).isEqualTo("3");
    }

    // TC-019 — AC-013: an invalid edit is rejected and the prior version is retained.
    @Test
    void invalidEditRejectedAndPreserved() {
        Certificate cert = persistValidCertificate("Bad-Edit-Cert");
        Question original = cert.getSections().get(0).getQuestions().get(0);
        String originalText = original.getText();

        QuestionRequest bad = questionRequest(0);
        bad.setOptions(List.of("only-one", "two", "three", "four"));
        bad.setText("   "); // blank -> invalid

        assertThatThrownBy(() -> service.editQuestion(original.getId(), bad))
                .isInstanceOf(CertificationValidationException.class);

        Question reloaded = service.getCertificate(cert.getId())
                .getSections().get(0).getQuestions().get(0);
        assertThat(reloaded.getText()).isEqualTo(originalText);
    }

    // TC-020 — AC-014: editing a non-existent question yields not-found.
    @Test
    void editMissingQuestionNotFound() {
        assertThatThrownBy(() -> service.editQuestion(999999L, questionRequest(0)))
                .isInstanceOf(CertificationNotFoundException.class);
    }

    // TC-021 — AC-015: deleting an existing question removes it.
    @Test
    void deleteQuestionRemovesIt() {
        Certificate cert = persistValidCertificate("Del-Q-Cert");
        Question q = cert.getSections().get(0).getQuestions().get(0);

        service.deleteQuestion(q.getId());

        Certificate reloaded = service.getCertificate(cert.getId());
        boolean stillThere = reloaded.getSections().stream()
                .flatMap(s -> s.getQuestions().stream())
                .anyMatch(x -> x.getId().equals(q.getId()));
        assertThat(stillThere).isFalse();
    }

    // TC-022 — AC-016: deleting a non-existent question yields not-found.
    @Test
    void deleteMissingQuestionNotFound() {
        assertThatThrownBy(() -> service.deleteQuestion(999999L))
                .isInstanceOf(CertificationNotFoundException.class);
    }

    // TC-023 — AC-022: setting a valid pass percentage stores it.
    @Test
    void setPassPercentageStores() {
        Certificate cert = persistValidCertificate("Pass-Cert");
        service.setPassPercentage(cert.getId(), 85);
        assertThat(service.getCertificate(cert.getId()).getPassPercentage()).isEqualTo(85);
    }

    // TC-024 — AC-018: setting exam length above the pool is rejected.
    @Test
    void setExamLengthAbovePoolRejected() {
        Certificate cert = persistValidCertificate("Len-Cert"); // pool = 5
        assertThatThrownBy(() -> service.setQuestionsToAsk(cert.getId(), 99))
                .isInstanceOf(CertificationValidationException.class);
    }

    // TC-025 — AC-026: getCertificate returns sections and questions for viewing.
    @Test
    void getCertificateReturnsStructure() {
        Certificate cert = persistValidCertificate("View-Cert");
        Certificate loaded = service.getCertificate(cert.getId());
        assertThat(loaded.getSections()).hasSize(2);
        assertThat(loaded.totalQuestions()).isEqualTo(5);
    }

    // TC-026 — AC-024, DES-006: create() rejects an invalid aggregate (protects the bootstrap path).
    @Test
    void createRejectsInvalidAggregate() {
        Certificate bad = new Certificate("Invalid-Cert", 70, 5);
        Section a = new Section("Only", 40); // weight != 100
        for (int i = 0; i < 5; i++) {
            a.addQuestion(sampleQuestion("X" + i));
        }
        bad.addSection(a);
        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(CertificationValidationException.class);
    }

    // TC-027 — F-002 / AC-006, AC-020: validateCertificate(id) enforces aggregate rules on the authoring path.
    @Test
    void validateCertificateRejectsInvalidDraft() {
        // A draft created via createDraft with a single 40-weight section and too few questions.
        Certificate draft = new Certificate("Draft-Cert", 70, 5);
        Section a = new Section("Alpha", 40);
        a.addQuestion(sampleQuestion("D0"));
        draft.addSection(a);
        Certificate saved = service.createDraft(draft);

        assertThatThrownBy(() -> service.validateCertificate(saved.getId()))
                .isInstanceOf(CertificationValidationException.class)
                .satisfies(ex -> assertThat(((CertificationValidationException) ex).getViolations())
                        .anyMatch(v -> v.field().equals("sections") || v.field().equals("questions")));
    }

    // TC-028 — F-002: validateCertificate(id) passes for a fully valid certificate.
    @Test
    void validateCertificateAcceptsValid() {
        Certificate cert = persistValidCertificate("Valid-Draft-Cert");
        Certificate result = service.validateCertificate(cert.getId());
        assertThat(result.getId()).isEqualTo(cert.getId());
    }

    // TC-029 — F-001 / AC-022, AC-003: pass percentage outside 1..100 is rejected.
    @Test
    void setPassPercentageRejectsOutOfRange() {
        Certificate cert = persistValidCertificate("PP-Range-Cert");
        assertThatThrownBy(() -> service.setPassPercentage(cert.getId(), 0))
                .isInstanceOf(CertificationValidationException.class);
        assertThatThrownBy(() -> service.setPassPercentage(cert.getId(), 200))
                .isInstanceOf(CertificationValidationException.class);
    }

    // TC-030 — F-003 / AC-002: a duplicate title is rejected with a structured violation.
    @Test
    void createRejectsDuplicateTitle() {
        persistValidCertificate("Dup-Title-Cert");
        Certificate second = new Certificate("Dup-Title-Cert", 70, 5);
        Section a = new Section("Alpha", 100);
        for (int i = 0; i < 5; i++) {
            a.addQuestion(sampleQuestion("S" + i));
        }
        second.addSection(a);
        assertThatThrownBy(() -> service.create(second))
                .isInstanceOf(CertificationValidationException.class)
                .satisfies(ex -> assertThat(((CertificationValidationException) ex).getViolations())
                        .anyMatch(v -> v.field().equals("title")));
    }
}
