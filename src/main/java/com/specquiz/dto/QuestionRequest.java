package com.specquiz.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Inbound model for creating or editing a question (DES-003, DES-004).
 * Exactly four options with exactly one marked correct via {@code correctOptionIndex}.
 */
@Data
public class QuestionRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String explanation;

    @NotEmpty
    @Size(min = 4, max = 4, message = "a question must have exactly 4 options")
    private List<@NotBlank String> options;

    @NotNull
    @Min(0)
    @Max(3)
    private Integer correctOptionIndex;
}
