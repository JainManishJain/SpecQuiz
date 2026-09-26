package com.specquiz.dto;

import java.util.List;

import com.specquiz.entity.Option;
import com.specquiz.entity.Question;

/**
 * Outbound model for a question (author-facing: includes the correct option id).
 */
public record QuestionResponse(
        Long id,
        String text,
        String explanation,
        List<OptionView> options,
        Long correctOptionId) {

    public record OptionView(Long id, String text) {
    }

    public static QuestionResponse from(Question question) {
        List<OptionView> views = question.getOptions().stream()
                .map(o -> new OptionView(o.getId(), o.getText()))
                .toList();
        Option correct = question.correctOption();
        Long correctId = correct == null ? null : correct.getId();
        return new QuestionResponse(question.getId(), question.getText(),
                question.getExplanation(), views, correctId);
    }
}
