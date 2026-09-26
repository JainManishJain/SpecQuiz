package com.specquiz.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specquiz.dto.QuestionRequest;
import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.service.CertificationService;

import java.util.List;

/**
 * REST-layer tests for {@link QuestionController} (question CRUD). Covers the
 * created/validation-error/not-found responses and confirms the structured error
 * body with a violations array.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificationService service;

    @Autowired
    private ObjectMapper objectMapper;

    private Long sectionId;

    @BeforeEach
    void setUp() {
        Certificate cert = new Certificate("REST Cert " + System.nanoTime(), 70, 5);
        Section a = new Section("Alpha", 60);
        for (int i = 0; i < 3; i++) {
            a.addQuestion(q("A" + i));
        }
        Section b = new Section("Beta", 40);
        for (int i = 0; i < 2; i++) {
            b.addQuestion(q("B" + i));
        }
        cert.addSection(a);
        cert.addSection(b);
        Certificate saved = service.create(cert);
        sectionId = saved.getSections().get(0).getId();
    }

    private Question q(String tag) {
        Question question = new Question("Q-" + tag, "expl-" + tag);
        question.addOption(new Option("correct", true));
        question.addOption(new Option("w1", false));
        question.addOption(new Option("w2", false));
        question.addOption(new Option("w3", false));
        return question;
    }

    private QuestionRequest request(List<String> options, Integer correctIndex) {
        QuestionRequest r = new QuestionRequest();
        r.setText("What is 2 + 2?");
        r.setExplanation("Basic arithmetic.");
        r.setOptions(options);
        r.setCorrectOptionIndex(correctIndex);
        return r;
    }

    // TC-037 — AC-008: adding a valid question returns 201 with the question body.
    @Test
    void addValidQuestionReturns201() throws Exception {
        QuestionRequest req = request(List.of("4", "3", "5", "22"), 0);
        mockMvc.perform(post("/api/sections/" + sectionId + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.options.length()").value(4));
    }

    // TC-038 — AC-009: fewer than four options returns 400 with a violations array.
    @Test
    void addQuestionWithWrongOptionCountReturns400() throws Exception {
        QuestionRequest req = request(List.of("4", "3", "5"), 0); // only 3
        mockMvc.perform(post("/api/sections/" + sectionId + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray());
    }

    // TC-039 — AC-016: deleting a non-existent question returns 404.
    @Test
    void deleteMissingQuestionReturns404() throws Exception {
        mockMvc.perform(delete("/api/questions/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
