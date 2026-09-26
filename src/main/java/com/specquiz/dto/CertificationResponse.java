package com.specquiz.dto;

import java.util.List;

import com.specquiz.entity.Certificate;

/**
 * Outbound summary of a certificate (DES-010).
 */
public record CertificationResponse(
        Long id,
        String title,
        Integer passPercentage,
        Integer questionsToAsk,
        int totalQuestions,
        List<SectionSummary> sections) {

    public record SectionSummary(Long id, String name, Integer weight, int questionCount) {
    }

    public static CertificationResponse from(Certificate certificate) {
        List<SectionSummary> summaries = certificate.getSections().stream()
                .map(s -> new SectionSummary(s.getId(), s.getName(), s.getWeight(), s.getQuestions().size()))
                .toList();
        return new CertificationResponse(certificate.getId(), certificate.getTitle(),
                certificate.getPassPercentage(), certificate.getQuestionsToAsk(),
                certificate.totalQuestions(), summaries);
    }
}
