package com.specquiz.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
 * MVC tests for the Certification Attempt flow. Each test names the AC it covers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificationService certificationService;

    private Long certificateId;
    private Long firstQuestionId;
    private Long firstCorrectOptionId;
    private java.util.List<com.specquiz.entity.Question> questions;

    @BeforeEach
    void setUp() {
        Certificate c = new Certificate("Attempt MVC " + System.nanoTime(), 60, 5);
        Section s = new Section("All", 100);
        for (int i = 0; i < 5; i++) {
            Question q = new Question("Q" + i, "Expl" + i);
            q.addOption(new Option("correct" + i, true));
            q.addOption(new Option("w1", false));
            q.addOption(new Option("w2", false));
            q.addOption(new Option("w3", false));
            s.addQuestion(q);
        }
        c.addSection(s);
        Certificate saved = certificationService.create(c);
        certificateId = saved.getId();
        questions = saved.getSections().get(0).getQuestions();
        Question first = questions.get(0);
        firstQuestionId = first.getId();
        firstCorrectOptionId = first.correctOption().getId();
    }

    // TC-008 — AC-004: the start page offers the certifications dropdown.
    @Test
    void startFormRenders() throws Exception {
        mockMvc.perform(get("/attempts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/start"))
                .andExpect(model().attributeExists("certificates"));
    }

    // TC-009 — AC-002: starting with a blank name re-renders the start form.
    @Test
    void startWithBlankNameReRendersForm() throws Exception {
        mockMvc.perform(post("/attempts")
                        .param("candidateName", "")
                        .param("certificateId", certificateId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/start"));
    }

    // TC-010 — AC-001, AC-005, AC-007: a valid start renders the exam page with the attempt model.
    @Test
    void startValidRendersExam() throws Exception {
        mockMvc.perform(post("/attempts")
                        .param("candidateName", "Alice")
                        .param("certificateId", certificateId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/exam"))
                .andExpect(model().attributeExists("attempt"));
    }

    // TC-011 — AC-003: starting for a missing certificate yields not-found.
    @Test
    void startMissingCertificateNotFound() throws Exception {
        mockMvc.perform(post("/attempts")
                        .param("candidateName", "Alice")
                        .param("certificateId", "999999"))
                .andExpect(status().isNotFound());
    }

    // TC-012 — AC-009, AC-011, AC-013: submitting a correct answer renders the result page.
    @Test
    void submitRendersResult() throws Exception {
        mockMvc.perform(post("/attempts/submit")
                        .param("candidateName", "Alice")
                        .param("certificateId", certificateId.toString())
                        .param("servedQuestionIds", firstQuestionId.toString())
                        .param("answers[" + firstQuestionId + "]", firstCorrectOptionId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/result"))
                .andExpect(model().attributeExists("result"));
    }

    // TC-013 — regression for the exam-form binding: a real browser submits servedQuestionIds
    // for every question plus answers[qid] only for answered ones. A correct answer must
    // actually be scored correct (guards against the prior empty-hidden-value collision).
    @Test
    void submitWithServedIdsScoresCorrectly() throws Exception {
        var result = mockMvc.perform(post("/attempts/submit")
                        .param("candidateName", "Bob")
                        .param("certificateId", certificateId.toString())
                        .param("servedQuestionIds", firstQuestionId.toString())
                        .param("answers[" + firstQuestionId + "]", firstCorrectOptionId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/result"))
                .andReturn();
        com.specquiz.dto.AttemptResult attempt =
                (com.specquiz.dto.AttemptResult) result.getModelAndView().getModel().get("result");
        org.assertj.core.api.Assertions.assertThat(attempt.totalQuestions()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(attempt.correctCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(attempt.scorePercentage()).isEqualTo(100);
    }

    // TC-014 — AC-010 via the form: a served question with no answers[qid] entry counts incorrect.
    @Test
    void submitWithUnansweredServedQuestionCountsIncorrect() throws Exception {
        var result = mockMvc.perform(post("/attempts/submit")
                        .param("candidateName", "Carol")
                        .param("certificateId", certificateId.toString())
                        .param("servedQuestionIds", firstQuestionId.toString()))
                // note: no answers[qid] param -> unanswered
                .andExpect(status().isOk())
                .andReturn();
        com.specquiz.dto.AttemptResult attempt =
                (com.specquiz.dto.AttemptResult) result.getModelAndView().getModel().get("result");
        org.assertj.core.api.Assertions.assertThat(attempt.totalQuestions()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(attempt.correctCount()).isEqualTo(0);
    }

    // TC-016 — multi-question regression for the form ordering: serve all 5 questions,
    // answer 3 correctly and leave 2 unanswered, and assert the full-page submission
    // scores exactly those 3 correct out of 5 (directly reproduces the layout the
    // original binding collision lived in).
    @Test
    void submitFullExamMixedAnswersScoresCorrectly() throws Exception {
        var builder = post("/attempts/submit")
                .param("candidateName", "Dave")
                .param("certificateId", certificateId.toString());
        // Serve all five questions.
        for (com.specquiz.entity.Question q : questions) {
            builder = builder.param("servedQuestionIds", q.getId().toString());
        }
        // Answer the first three correctly; leave questions 4 and 5 unanswered.
        for (int i = 0; i < 3; i++) {
            com.specquiz.entity.Question q = questions.get(i);
            builder = builder.param("answers[" + q.getId() + "]", q.correctOption().getId().toString());
        }
        var result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(view().name("attempts/result"))
                .andReturn();
        com.specquiz.dto.AttemptResult attempt =
                (com.specquiz.dto.AttemptResult) result.getModelAndView().getModel().get("result");
        org.assertj.core.api.Assertions.assertThat(attempt.totalQuestions()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(attempt.correctCount()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(attempt.scorePercentage()).isEqualTo(60);
        org.assertj.core.api.Assertions.assertThat(attempt.passed()).isTrue(); // 60 >= 60
    }
}
