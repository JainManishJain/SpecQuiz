package com.specquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for SpecQuiz.
 *
 * <p>SpecQuiz is a spec-driven self-evaluation and knowledge-testing
 * application for certification exams.
 */
@SpringBootApplication
public class SpecQuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpecQuizApplication.class, args);
    }
}
