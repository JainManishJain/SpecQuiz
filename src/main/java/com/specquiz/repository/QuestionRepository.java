package com.specquiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.specquiz.entity.Question;

/**
 * Persistence for individual {@link Question}s (used by question CRUD).
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
