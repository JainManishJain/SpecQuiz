package com.specquiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Inbound model for adding a section (DES-002). The multiple-of-20 rule is enforced
 * in the validator so all structural violations aggregate together.
 */
@Data
public class SectionRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(20)
    @Max(100)
    private Integer weight;
}
