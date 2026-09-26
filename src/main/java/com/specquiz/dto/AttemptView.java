package com.specquiz.dto;

import java.util.List;

/**
 * Data for the exam page (DES-002). Correct flags are intentionally NOT included —
 * correctness is recomputed server-side at scoring time (REQ-050).
 */
public record AttemptView(
        String candidateName,
        Long certificateId,
        String certificateTitle,
        List<QuestionView> questions) {

    public record QuestionView(Long questionId, String text, List<OptionView> options) {
    }

    public record OptionView(Long optionId, String text) {
    }
}
