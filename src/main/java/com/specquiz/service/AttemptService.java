package com.specquiz.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.specquiz.dto.AttemptResult;
import com.specquiz.dto.AttemptResult.OptionResult;
import com.specquiz.dto.AttemptResult.ResultItem;
import com.specquiz.dto.AttemptSubmission;
import com.specquiz.dto.AttemptView;
import com.specquiz.dto.AttemptView.OptionView;
import com.specquiz.dto.AttemptView.QuestionView;
import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;

/**
 * Stateless exam-taking logic for Certification Attempt (DES-002, DES-004, DES-005).
 * Selection reuses {@link CertificationService#selectQuestionsForAttempt}; scoring is
 * performed server-side from persisted data (REQ-050).
 */
@Service
public class AttemptService {

    private final CertificationService certificationService;

    public AttemptService(CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    /** Builds the exam view by selecting questions proportionally to section weight (DES-004). */
    @Transactional(readOnly = true)
    public AttemptView startAttempt(String candidateName, Long certificateId) {
        Certificate certificate = certificationService.getCertificate(certificateId); // 404 if missing (AC-003)
        List<Question> selected = certificationService.selectQuestionsForAttempt(certificateId); // AC-005, AC-006

        List<QuestionView> questionViews = selected.stream()
                .map(q -> new QuestionView(
                        q.getId(),
                        q.getText(),
                        q.getOptions().stream()
                                .map(o -> new OptionView(o.getId(), o.getText()))
                                .toList()))
                .toList();

        return new AttemptView(candidateName, certificateId, certificate.getTitle(), questionViews);
    }

    /** Scores a submission server-side (DES-005). Unanswered/missing questions count as incorrect. */
    @Transactional(readOnly = true)
    public AttemptResult score(AttemptSubmission submission) {
        Certificate certificate = certificationService.getCertificate(submission.getCertificateId());

        // Build a lookup of questionId -> Question across all sections.
        Map<Long, Question> questionsById = certificate.getSections().stream()
                .flatMap(s -> s.getQuestions().stream())
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        Map<Long, Long> answers = submission.getAnswers();
        List<ResultItem> items = new ArrayList<>();
        int correctCount = 0;

        // The authoritative served-question set comes from the hidden servedQuestionIds
        // field, NOT from the answers map — so unanswered questions (absent from answers)
        // are still counted, and the denominator cannot be shrunk by the client (AC-010).
        // Fail closed: if no served ids are present, there is nothing to score. We do NOT
        // fall back to answers.keySet(), which would let a hand-crafted post drop
        // unanswered questions from the denominator (scoring is a server-side boundary, REQ-050).
        List<Long> servedIds = submission.getServedQuestionIds();
        for (Long questionId : servedIds) {
            Question question = questionsById.get(questionId);
            Long selectedOptionId = answers.get(questionId);

            if (question == null) {
                // REQ-052: a question that no longer exists is scored as incorrect.
                items.add(new ResultItem("(question no longer available)", List.of(), false,
                        "This question was removed after the exam was served."));
                continue;
            }

            Option correctOption = question.correctOption();
            Long correctOptionId = correctOption == null ? null : correctOption.getId();
            boolean isCorrect = selectedOptionId != null && selectedOptionId.equals(correctOptionId); // AC-009, AC-010
            if (isCorrect) {
                correctCount++;
            }

            List<OptionResult> optionResults = question.getOptions().stream()
                    .map(o -> new OptionResult(o.getText(), o.isCorrect(),
                            o.getId().equals(selectedOptionId)))
                    .toList();

            items.add(new ResultItem(question.getText(), optionResults, isCorrect, question.getExplanation())); // AC-014, AC-015
        }

        int total = servedIds.size();
        int score = total == 0 ? 0 : Math.round(correctCount * 100.0f / total); // AC-009
        boolean passed = score >= certificate.getPassPercentage(); // AC-011, AC-012

        return new AttemptResult(submission.getCandidateName(), total, correctCount, score,
                certificate.getPassPercentage(), passed, items); // AC-013
    }
}
