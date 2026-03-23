package com.bhak.project.repository;

import com.bhak.project.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link VerificationToken} entity.
 *
 * <h2>Purpose</h2>
 * Accesses the {@code tbl_verification_tokens} table. The primary key is
 * the {@code tokenId} (String), not an auto-increment.
 * Used by {@link com.bhak.project.service.VerificationService} to
 * save, look up, and delete email verification tokens.
 *
 * <h2>Methods</h2>
 * Inherits from {@code JpaRepository<VerificationToken, String>}: {@code save()},
 * {@code findById(String)}, {@code delete()}, etc.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
}
