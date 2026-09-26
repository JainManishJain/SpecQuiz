package com.specquiz.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.specquiz.dto.CertificationRequest;
import com.specquiz.dto.SectionRequest;
import com.specquiz.entity.Certificate;
import com.specquiz.exception.CertificationValidationException;
import com.specquiz.service.CertificationService;

/**
 * Server-rendered authoring UI for Certification Management (DES-001, DES-002,
 * DES-008, DES-009, DES-010). REQ-052 (Thymeleaf).
 */
@Controller
public class CertificationController {

    private final CertificationService service;

    public CertificationController(CertificationService service) {
        this.service = service;
    }

    @GetMapping("/certifications")
    public String list(Model model) {
        model.addAttribute("certificates", service.findAll()); // AC-025
        return "certifications/list";
    }

    @GetMapping("/certifications/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("certificate", service.getCertificate(id)); // AC-026
        return "certifications/view";
    }

    @GetMapping("/certifications/new")
    public String newForm(Model model) {
        model.addAttribute("certificationRequest", new CertificationRequest());
        return "certifications/form";
    }

    @PostMapping("/certifications")
    public String create(@Valid @ModelAttribute CertificationRequest certificationRequest,
            BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            return "certifications/form"; // AC-002, AC-003
        }
        Certificate certificate = new Certificate(
                certificationRequest.getTitle(),
                certificationRequest.getPassPercentage(),
                certificationRequest.getQuestionsToAsk());
        // Persist the shell without validating the full aggregate yet; sections/questions
        // are added on the view page, and full validity is checked on save/validate.
        Certificate saved = service.createDraft(certificate); // AC-001
        return "redirect:/certifications/" + saved.getId();
    }

    @PostMapping("/certifications/{id}/sections")
    public String addSection(@PathVariable Long id,
            @Valid @ModelAttribute SectionRequest sectionRequest,
            BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("certificate", service.getCertificate(id));
            model.addAttribute("sectionError", "Section name and a weight of 20-100 are required");
            return "certifications/view"; // AC-004, AC-005
        }
        service.addSection(id, sectionRequest.getName(), sectionRequest.getWeight());
        return "redirect:/certifications/" + id;
    }

    @PostMapping("/certifications/{id}/exam-length")
    public String setExamLength(@PathVariable Long id,
            @RequestParam Integer questionsToAsk, Model model) {
        try {
            service.setQuestionsToAsk(id, questionsToAsk); // AC-017, AC-018
        } catch (CertificationValidationException ex) {
            model.addAttribute("certificate", service.getCertificate(id));
            model.addAttribute("examLengthError", ex.getViolations());
            return "certifications/view";
        }
        return "redirect:/certifications/" + id;
    }

    @PostMapping("/certifications/{id}/pass-percentage")
    public String setPassPercentage(@PathVariable Long id,
            @RequestParam Integer passPercentage, Model model) {
        try {
            service.setPassPercentage(id, passPercentage); // AC-022, AC-003 (F-001)
        } catch (CertificationValidationException ex) {
            model.addAttribute("certificate", service.getCertificate(id));
            model.addAttribute("passPercentageError", ex.getViolations());
            return "certifications/view";
        }
        return "redirect:/certifications/" + id;
    }

    @PostMapping("/certifications/{id}/validate")
    public String validate(@PathVariable Long id, Model model) {
        try {
            service.validateCertificate(id); // F-002: enforces AC-006, AC-007, AC-020
            model.addAttribute("validationOk", "Certificate is valid and ready.");
        } catch (CertificationValidationException ex) {
            model.addAttribute("validationErrors", ex.getViolations());
        }
        model.addAttribute("certificate", service.getCertificate(id));
        return "certifications/view";
    }
}
