package com.specquiz.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Inbound submitted answers (DES-003).
 *
 * <p>{@code servedQuestionIds} is the authoritative list of questions the exam served
 * (carried in a hidden field), so the scorer's denominator is not at the mercy of what
 * the client chose to answer. {@code answers} maps a questionId to the selected optionId;
 * a question whose id is served but absent from {@code answers} was left unanswered and
 * counts as incorrect (AC-010). The radios are the only inputs that populate
 * {@code answers}, avoiding the earlier empty-hidden-value binding collision.
 */
@Data
public class AttemptSubmission {

    @NotBlank
    private String candidateName;

    @NotNull
    private Long certificateId;

    private List<Long> servedQuestionIds = new ArrayList<>();

    private Map<Long, Long> answers = new LinkedHashMap<>();
}
