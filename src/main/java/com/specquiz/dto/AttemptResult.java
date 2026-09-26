package com.specquiz.dto;

import java.util.List;

/**
 * Outbound attempt result (DES-005): score, PASS/FAIL, and per-question feedback.
 */
public record AttemptResult(
        String candidateName,
        int totalQuestions,
        int correctCount,
        int scorePercentage,
        int passPercentage,
        boolean passed,
        List<ResultItem> items) {

    public record ResultItem(
            String questionText,
            List<OptionResult> options,
            boolean correct,
            String explanation) {
    }

    public record OptionResult(String text, boolean correct, boolean selected) {
    }
}
