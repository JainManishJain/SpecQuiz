package com.specquiz.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.specquiz.entity.Certificate;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.exception.CertificationValidationException;
import com.specquiz.exception.CertificationValidationException.Violation;

/**
 * Pure validation and selection helpers for the Certificate aggregate.
 *
 * <p>Validation aggregates ALL violations before throwing (AC-021), so authors see
 * every problem at once. Proportional selection (DES-012) draws questions in
 * proportion to section weights (AC-019).
 */
@Component
public class CertificationValidator {

    private static final int MIN_QUESTIONS_TOTAL = 5;
    private static final int MIN_QUESTIONS_TO_ASK = 5;
    private static final int WEIGHT_MULTIPLE = 20;
    private static final int WEIGHT_TOTAL = 100;
    private static final int OPTIONS_PER_QUESTION = 4;

    /**
     * Validates the whole certificate aggregate (DES-006). Throws with the complete
     * list of violations if any rule fails.
     */
    public void validateCertificate(Certificate certificate) {
        List<Violation> violations = collectCertificateViolations(certificate);
        if (!violations.isEmpty()) {
            throw new CertificationValidationException(violations);
        }
    }

    /** Collects (without throwing) all structural violations for a certificate. */
    public List<Violation> collectCertificateViolations(Certificate certificate) {
        List<Violation> violations = new ArrayList<>();

        if (certificate.getTitle() == null || certificate.getTitle().isBlank()) {
            violations.add(new Violation("title", "title must not be blank")); // AC-002
        }
        Integer pass = certificate.getPassPercentage();
        if (pass == null || pass < 1 || pass > 100) {
            violations.add(new Violation("passPercentage", "pass percentage must be between 1 and 100")); // AC-003
        }

        List<Section> sections = certificate.getSections();
        if (sections.isEmpty()) {
            violations.add(new Violation("sections", "a certificate must have at least one section")); // AC-007
        } else {
            for (Section section : sections) {
                Integer w = section.getWeight();
                if (w == null || w <= 0 || w % WEIGHT_MULTIPLE != 0) {
                    violations.add(new Violation("section.weight",
                            "section '" + section.getName() + "' weight must be a positive multiple of 20")); // AC-005
                }
            }
            int totalWeight = certificate.totalWeight();
            if (totalWeight != WEIGHT_TOTAL) {
                violations.add(new Violation("sections",
                        "section weights must total 100 (current total: " + totalWeight + ")")); // AC-006
            }
        }

        int totalQuestions = certificate.totalQuestions();
        if (totalQuestions < MIN_QUESTIONS_TOTAL) {
            violations.add(new Violation("questions",
                    "a certificate must have at least 5 questions (current: " + totalQuestions + ")")); // AC-020
        }

        Integer toAsk = certificate.getQuestionsToAsk();
        if (toAsk == null || toAsk < MIN_QUESTIONS_TO_ASK) {
            violations.add(new Violation("questionsToAsk", "must ask at least 5 questions")); // AC-017
        } else if (toAsk > totalQuestions) {
            violations.add(new Violation("questionsToAsk",
                    "cannot ask more questions (" + toAsk + ") than exist in the pool (" + totalQuestions + ")")); // AC-018
        }

        for (Section section : sections) {
            for (Question question : section.getQuestions()) {
                violations.addAll(collectQuestionViolations(question));
            }
        }

        return violations;
    }

    /**
     * Validates a single question (DES-011). Throws if invalid.
     */
    public void validateQuestion(Question question) {
        List<Violation> violations = collectQuestionViolations(question);
        if (!violations.isEmpty()) {
            throw new CertificationValidationException(violations);
        }
    }

    /** Collects (without throwing) violations for a single question. */
    public List<Violation> collectQuestionViolations(Question question) {
        List<Violation> violations = new ArrayList<>();
        if (question.getText() == null || question.getText().isBlank()) {
            violations.add(new Violation("question.text", "question text must not be blank")); // AC-011
        }
        if (question.getExplanation() == null || question.getExplanation().isBlank()) {
            violations.add(new Violation("question.explanation", "explanation must not be blank")); // AC-011
        }
        if (question.getOptions().size() != OPTIONS_PER_QUESTION) {
            violations.add(new Violation("question.options", "a question must have exactly 4 options")); // AC-009
        }
        long correct = question.correctCount();
        if (correct != 1) {
            violations.add(new Violation("question.correct",
                    "a question must have exactly one correct option (found " + correct + ")")); // AC-010
        }
        return violations;
    }

    /**
     * Selects {@code questionsToAsk} questions proportionally to each section's weight
     * (DES-012, AC-019). Uses largest-remainder rounding so the picks sum exactly to N.
     */
    public List<Question> selectProportionally(Certificate certificate, Random random) {
        int toAsk = certificate.getQuestionsToAsk();
        List<Section> sections = certificate.getSections();

        // Base allocation by floor, tracking fractional remainders for largest-remainder.
        int[] alloc = new int[sections.size()];
        double[] remainder = new double[sections.size()];
        int allocated = 0;
        for (int i = 0; i < sections.size(); i++) {
            double exact = toAsk * (sections.get(i).getWeight() / (double) WEIGHT_TOTAL);
            alloc[i] = (int) Math.floor(exact);
            remainder[i] = exact - alloc[i];
            allocated += alloc[i];
        }
        // Distribute the leftover to the largest remainders.
        int leftover = toAsk - allocated;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(remainder[b], remainder[a]));
        for (int k = 0; k < leftover && k < order.size(); k++) {
            alloc[order.get(k)]++;
        }

        // Shuffle each section's pool once, up front, so we can take/redistribute deterministically per seed.
        List<List<Question>> pools = new ArrayList<>();
        for (Section section : sections) {
            List<Question> pool = new ArrayList<>(section.getQuestions());
            java.util.Collections.shuffle(pool, random);
            pools.add(pool);
        }

        // First pass: take each section's allocation, capped at its pool size, tracking any shortfall.
        List<Question> selected = new ArrayList<>();
        int[] taken = new int[sections.size()];
        int shortfall = 0;
        for (int i = 0; i < sections.size(); i++) {
            int take = Math.min(alloc[i], pools.get(i).size());
            selected.addAll(pools.get(i).stream().limit(take).collect(Collectors.toList()));
            taken[i] = take;
            shortfall += alloc[i] - take;
        }

        // F-005: redistribute the shortfall to sections that still have unused questions,
        // so the result sums to N whenever the total pool is large enough.
        while (shortfall > 0) {
            boolean progressed = false;
            for (int i = 0; i < sections.size() && shortfall > 0; i++) {
                if (taken[i] < pools.get(i).size()) {
                    selected.add(pools.get(i).get(taken[i]));
                    taken[i]++;
                    shortfall--;
                    progressed = true;
                }
            }
            if (!progressed) {
                break; // total pool exhausted (should not happen once the certificate is valid)
            }
        }
        return selected;
    }
}
