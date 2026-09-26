package com.specquiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Inbound model for creating a certificate (DES-001).
 */
@Data
public class CertificationRequest {

    @NotBlank
    private String title;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer passPercentage;

    @NotNull
    @Min(value = 5, message = "an exam must ask at least 5 questions")
    private Integer questionsToAsk;
}
