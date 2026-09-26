package com.specquiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Inbound model to start an attempt (DES-001): candidate name + chosen certificate.
 */
@Data
public class StartAttemptRequest {

    @NotBlank
    private String candidateName;

    @NotNull
    private Long certificateId;
}
