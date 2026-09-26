package com.specquiz.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.specquiz.dto.QuestionRequest;
import com.specquiz.dto.QuestionResponse;
import com.specquiz.service.CertificationService;

/**
 * REST CRUD for questions (DES-003, DES-004, DES-005), used by the authoring page's
 * inline editing.
 */
@RestController
@RequestMapping("/api")
public class QuestionController {

    private final CertificationService service;

    public QuestionController(CertificationService service) {
        this.service = service;
    }

    @PostMapping("/sections/{sectionId}/questions")
    public ResponseEntity<QuestionResponse> add(@PathVariable Long sectionId,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse body = QuestionResponse.from(service.addQuestion(sectionId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionResponse> edit(@PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(QuestionResponse.from(service.editQuestion(id, request)));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
