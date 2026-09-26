package com.specquiz.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate root for a certification exam definition (FEAT-001).
 * Owns one or more weighted {@link Section}s.
 */
@Entity
@Table(name = "certificate", uniqueConstraints = @UniqueConstraint(name = "uk_certificate_title", columnNames = "title"))
@Getter
@Setter
@NoArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "pass_percentage", nullable = false)
    private Integer passPercentage;

    @Column(name = "questions_to_ask", nullable = false)
    private Integer questionsToAsk;

    @OneToMany(mappedBy = "certificate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public Certificate(String title, Integer passPercentage, Integer questionsToAsk) {
        this.title = title;
        this.passPercentage = passPercentage;
        this.questionsToAsk = questionsToAsk;
    }

    /** Adds a section and maintains both sides of the relationship. */
    public void addSection(Section section) {
        section.setCertificate(this);
        this.sections.add(section);
    }

    /** Total number of questions across all sections (the pool size). */
    public int totalQuestions() {
        return sections.stream().mapToInt(s -> s.getQuestions().size()).sum();
    }

    /** Sum of all section weights. */
    public int totalWeight() {
        return sections.stream().mapToInt(Section::getWeight).sum();
    }
}
