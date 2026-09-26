package com.specquiz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.specquiz.entity.Certificate;

/**
 * Persistence for the {@link Certificate} aggregate.
 */
public interface CertificationRepository extends JpaRepository<Certificate, Long> {

    boolean existsByTitle(String title);

    Optional<Certificate> findByTitle(String title);
}
