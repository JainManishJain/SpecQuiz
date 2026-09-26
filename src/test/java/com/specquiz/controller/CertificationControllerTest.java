package com.specquiz.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.service.CertificationService;

/**
 * Controller-layer (MVC) tests for the Certification Management authoring pages.
 * Covers the create/validate/pass-percentage paths and the list page that the
 * earlier suite did not exercise. Each test names the AC / finding it covers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificationService service;

    private Long validCertificateId;
    private Long invalidDraftId;

    @BeforeEach
    void setUp() {
        Certificate valid = new Certificate("MVC Valid " + System.nanoTime(), 70, 5);
        Section a = new Section("Alpha", 60);
        for (int i = 0; i < 3; i++) {
            a.addQuestion(q("A" + i));
        }
        Section b = new Section("Beta", 40);
        for (int i = 0; i < 2; i++) {
            b.addQuestion(q("B" + i));
        }
        valid.addSection(a);
        valid.addSection(b);
        validCertificateId = service.create(valid).getId();

        Certificate draft = new Certificate("MVC Draft " + System.nanoTime(), 70, 5);
        Section only = new Section("Solo", 40); // invalid: weight != 100, < 5 questions
        only.addQuestion(q("D0"));
        draft.addSection(only);
        invalidDraftId = service.createDraft(draft).getId();
    }

    private Question q(String tag) {
        Question question = new Question("Q-" + tag, "expl-" + tag);
        question.addOption(new Option("correct", true));
        question.addOption(new Option("w1", false));
        question.addOption(new Option("w2", false));
        question.addOption(new Option("w3", false));
        return question;
    }

    // TC-031 — AC-025: the list page renders with the certificates model attribute.
    @Test
    void listPageRenders() throws Exception {
        mockMvc.perform(get("/certifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("certifications/list"))
                .andExpect(model().attributeExists("certificates"));
    }

    // TC-032 — AC-026: the view page renders a certificate.
    @Test
    void viewPageRenders() throws Exception {
        mockMvc.perform(get("/certifications/" + validCertificateId))
                .andExpect(status().isOk())
                .andExpect(view().name("certifications/view"))
                .andExpect(model().attributeExists("certificate"));
    }

    // TC-033 — F-001 / AC-022: out-of-range pass percentage re-renders the view with an error.
    @Test
    void passPercentageOutOfRangeShowsError() throws Exception {
        mockMvc.perform(post("/certifications/" + validCertificateId + "/pass-percentage")
                        .param("passPercentage", "200"))
                .andExpect(status().isOk())
                .andExpect(view().name("certifications/view"))
                .andExpect(model().attributeExists("passPercentageError"));
    }

    // TC-034 — AC-022: a valid pass percentage redirects back to the view.
    @Test
    void passPercentageValidRedirects() throws Exception {
        mockMvc.perform(post("/certifications/" + validCertificateId + "/pass-percentage")
                        .param("passPercentage", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/certifications/*"));
    }

    // TC-035 — F-002 / AC-006, AC-020: validating an invalid draft shows aggregated errors.
    @Test
    void validateInvalidDraftShowsErrors() throws Exception {
        mockMvc.perform(post("/certifications/" + invalidDraftId + "/validate"))
                .andExpect(status().isOk())
                .andExpect(view().name("certifications/view"))
                .andExpect(model().attributeExists("validationErrors"));
    }

    // TC-036 — F-002: validating a valid certificate shows the success message.
    @Test
    void validateValidCertificateShowsOk() throws Exception {
        mockMvc.perform(post("/certifications/" + validCertificateId + "/validate"))
                .andExpect(status().isOk())
                .andExpect(view().name("certifications/view"))
                .andExpect(model().attributeExists("validationOk"));
    }
}
