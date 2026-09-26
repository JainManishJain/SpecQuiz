package com.specquiz.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.specquiz.dto.QuestionRequest;
import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.exception.CertificationNotFoundException;
import com.specquiz.exception.CertificationValidationException;
import com.specquiz.exception.CertificationValidationException.Violation;
import com.specquiz.repository.CertificationRepository;
import com.specquiz.repository.QuestionRepository;
import com.specquiz.util.CertificationValidator;

/**
 * Business logic for Certification Management (DES-001..DES-012).
 */
@Service
@Transactional
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final QuestionRepository questionRepository;
    private final CertificationValidator validator;

    public CertificationService(CertificationRepository certificationRepository,
            QuestionRepository questionRepository, CertificationValidator validator) {
        this.certificationRepository = certificationRepository;
        this.questionRepository = questionRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<Certificate> findAll() {
        List<Certificate> all = certificationRepository.findAll();
        all.forEach(this::initialize);
        return all;
    }

    @Transactional(readOnly = true)
    public Certificate getCertificate(Long id) {
        Certificate certificate = certificationRepository.findById(id)
                .orElseThrow(() -> CertificationNotFoundException.of("Certificate", id));
        initialize(certificate);
        return certificate;
    }

    /**
     * Forces the lazy aggregate graph (sections -> questions -> options) to load
     * within the transaction, so callers (Thymeleaf views, tests) can traverse it
     * after the session closes.
     */
    private void initialize(Certificate certificate) {
        certificate.getSections().forEach(section -> {
            section.getQuestions().forEach(question -> question.getOptions().size());
        });
    }

    /**
     * Persists a fully-built certificate after validating the whole aggregate (DES-006).
     * Used by the startup bootstrap so seeded data passes the same rules (AC-024).
     */
    public Certificate create(Certificate certificate) {
        requireUniqueTitle(certificate.getTitle(), null); // F-003
        validator.validateCertificate(certificate); // full structural validation
        return certificationRepository.save(certificate);
    }

    /**
     * Persists a new certificate shell (title, pass %, exam length) without requiring
     * sections/questions yet (DES-001, AC-001). Full validity is enforced on save/validate
     * and on exam-length changes. Basic field bounds are guarded by bean validation.
     */
    public Certificate createDraft(Certificate certificate) {
        requireUniqueTitle(certificate.getTitle(), null); // F-003
        return certificationRepository.save(certificate);
    }

    /**
     * Selects the questions to serve for an attempt, proportionally to section weight
     * (DES-004, reuses DES-012 / AC-006). Read-only; used by the Certification Attempt feature.
     */
    @Transactional(readOnly = true)
    public List<Question> selectQuestionsForAttempt(Long certificateId) {
        Certificate certificate = getCertificate(certificateId);
        return validator.selectProportionally(certificate, new java.util.Random());
    }

    /**
     * Explicit save/validate action (F-002): runs the full aggregate validation
     * (DES-006) against a persisted certificate so AC-006 (weights sum to 100),
     * AC-007 (>=1 section), and AC-020 (>=5 questions) are enforced through a real
     * authoring action, not just at bootstrap. Throws with all violations (AC-021).
     */
    public Certificate validateCertificate(Long certificateId) {
        Certificate certificate = getCertificate(certificateId);
        validator.validateCertificate(certificate);
        return certificate;
    }

    /** Adds a section to a certificate (DES-002). Weight rule enforced on next save/validate. */
    public Section addSection(Long certificateId, String name, Integer weight) {
        Certificate certificate = getCertificate(certificateId);
        Section section = new Section(name, weight);
        certificate.addSection(section);
        certificationRepository.save(certificate);
        return section;
    }

    /** Sets the exam length, validating against the pool size (DES-008, AC-017, AC-018). */
    public Certificate setQuestionsToAsk(Long certificateId, Integer questionsToAsk) {
        Certificate certificate = getCertificate(certificateId);
        Integer previous = certificate.getQuestionsToAsk();
        certificate.setQuestionsToAsk(questionsToAsk);
        try {
            validator.validateCertificate(certificate);
        } catch (RuntimeException ex) {
            certificate.setQuestionsToAsk(previous);
            throw ex;
        }
        return certificationRepository.save(certificate);
    }

    /** Sets the pass percentage, enforcing the 1..100 bound server-side (DES-009, AC-022, F-001). */
    public Certificate setPassPercentage(Long certificateId, Integer passPercentage) {
        if (passPercentage == null || passPercentage < 1 || passPercentage > 100) {
            throw new CertificationValidationException(List.of(
                    new Violation("passPercentage", "pass percentage must be between 1 and 100"))); // AC-022, AC-003
        }
        Certificate certificate = getCertificate(certificateId);
        certificate.setPassPercentage(passPercentage);
        return certificationRepository.save(certificate);
    }

    /** Adds a question to a section after validating the question (DES-003, DES-011). */
    public Question addQuestion(Long sectionId, QuestionRequest request) {
        Certificate certificate = findCertificateBySectionId(sectionId);
        Section section = certificate.getSections().stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> CertificationNotFoundException.of("Section", sectionId));

        Question question = buildQuestion(request);
        validator.validateQuestion(question); // AC-008, AC-009, AC-010, AC-011
        question.setSection(section);
        section.getQuestions().add(question);
        // Persist the question directly so the returned instance carries its generated id.
        return questionRepository.saveAndFlush(question);
    }

    /** Edits a question; rejects invalid edits and keeps the prior version (DES-004, AC-013). */
    public Question editQuestion(Long questionId, QuestionRequest request) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> CertificationNotFoundException.of("Question", questionId)); // AC-014

        Question candidate = buildQuestion(request);
        validator.validateQuestion(candidate); // AC-013: throws before mutating persisted state

        existing.setText(request.getText());
        existing.setExplanation(request.getExplanation());
        existing.getOptions().clear();
        for (int i = 0; i < request.getOptions().size(); i++) {
            existing.addOption(new Option(request.getOptions().get(i), i == request.getCorrectOptionIndex()));
        }
        return questionRepository.save(existing); // AC-012
    }

    /** Deletes a question (DES-005). */
    public void deleteQuestion(Long questionId) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> CertificationNotFoundException.of("Question", questionId)); // AC-016
        Section section = existing.getSection();
        if (section != null) {
            section.getQuestions().remove(existing);
        }
        questionRepository.delete(existing); // AC-015
    }

    private Question buildQuestion(QuestionRequest request) {
        Question question = new Question(request.getText(), request.getExplanation());
        List<String> options = request.getOptions();
        for (int i = 0; i < options.size(); i++) {
            question.addOption(new Option(options.get(i), i == request.getCorrectOptionIndex()));
        }
        return question;
    }

    /**
     * Rejects a duplicate title with a structured validation error instead of letting
     * the DB unique constraint surface as a 500 (F-003, AC-002/REQ-053).
     * {@code selfId} is excluded from the check to allow no-op re-saves.
     */
    private void requireUniqueTitle(String title, Long selfId) {
        if (title == null || title.isBlank()) {
            return; // blank-title handling is the validator's job
        }
        certificationRepository.findByTitle(title)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new CertificationValidationException(List.of(
                            new Violation("title", "a certificate with this title already exists")));
                });
    }

    private Certificate findCertificateBySectionId(Long sectionId) {
        return certificationRepository.findAll().stream()
                .filter(c -> c.getSections().stream().anyMatch(s -> s.getId().equals(sectionId)))
                .findFirst()
                .orElseThrow(() -> CertificationNotFoundException.of("Section", sectionId));
    }
}
