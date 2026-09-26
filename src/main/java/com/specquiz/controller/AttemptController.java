package com.specquiz.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.specquiz.dto.AttemptSubmission;
import com.specquiz.dto.StartAttemptRequest;
import com.specquiz.service.AttemptService;
import com.specquiz.service.CertificationService;

/**
 * Server-rendered exam-taking UI for Certification Attempt (DES-001, DES-002, DES-003).
 */
@Controller
public class AttemptController {

    private final AttemptService attemptService;
    private final CertificationService certificationService;

    public AttemptController(AttemptService attemptService, CertificationService certificationService) {
        this.attemptService = attemptService;
        this.certificationService = certificationService;
    }

    @GetMapping("/attempts/new")
    public String startForm(Model model) {
        model.addAttribute("startAttemptRequest", new StartAttemptRequest());
        model.addAttribute("certificates", certificationService.findAll()); // AC-004 dropdown
        return "attempts/start";
    }

    @PostMapping("/attempts")
    public String start(@Valid @ModelAttribute StartAttemptRequest startAttemptRequest,
            BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("certificates", certificationService.findAll());
            return "attempts/start"; // AC-002
        }
        model.addAttribute("attempt",
                attemptService.startAttempt(startAttemptRequest.getCandidateName(),
                        startAttemptRequest.getCertificateId())); // AC-001, AC-005..008 (404 -> handler, AC-003)
        return "attempts/exam";
    }

    @PostMapping("/attempts/submit")
    public String submit(@ModelAttribute AttemptSubmission attemptSubmission, Model model) {
        model.addAttribute("result", attemptService.score(attemptSubmission)); // AC-009..015
        return "attempts/result";
    }
}
