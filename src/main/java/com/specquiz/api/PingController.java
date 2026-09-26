package com.specquiz.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal REST endpoint used to confirm the application is up and serving requests.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "application", "SpecQuiz",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
