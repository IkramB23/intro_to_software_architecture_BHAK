package com.bhak.project.repository;

import com.bhak.project.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Credentials} entity.
 *
 * <h2>Purpose</h2>
 * Accesses the {@code tbl_credentials} table containing sensitive data
 * (email, phone, hashed password). Mainly used for
 * uniqueness checks during registration and account updates.
 *
 * <h2>Methods</h2>
 * <ul>
 *   <li>{@code findByEmail(String)} – lookup by email address.</li>
 *   <li>{@code findByPhoneNumber(String)} – lookup by phone number.</li>
 *   <li>{@code existsByEmail(String)} – existence check.</li>
 * </ul>
 */
@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    Credentials findByEmail(String email);

    Credentials findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
