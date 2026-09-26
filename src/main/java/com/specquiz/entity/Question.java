package com.specquiz.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single-answer multiple-choice question: four {@link Option}s, exactly one correct,
 * plus a one-line explanation.
 */
@Entity
@Table(name = "question", indexes = @Index(name = "idx_question_section", columnList = "section_id"))
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Option> options = new ArrayList<>();

    public Question(String text, String explanation) {
        this.text = text;
        this.explanation = explanation;
    }

    /** Adds an option and maintains both sides of the relationship. */
    public void addOption(Option option) {
        option.setQuestion(this);
        this.options.add(option);
    }

    /** Number of options currently flagged correct. */
    public long correctCount() {
        return options.stream().filter(Option::isCorrect).count();
    }

    /** The correct option, or null if not exactly one. */
    public Option correctOption() {
        return options.stream().filter(Option::isCorrect).findFirst().orElse(null);
    }
}
