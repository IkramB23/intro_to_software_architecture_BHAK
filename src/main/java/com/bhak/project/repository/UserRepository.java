package com.bhak.project.repository;

import com.bhak.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 *
 * <h2>Purpose</h2>
 * Provides access to the {@code tbl_users} table without requiring
 * a manual implementation. Spring Data automatically generates SQL queries
 * from method signatures (query derivation).
 *
 * <h2>Custom methods</h2>
 * <ul>
 *   <li>{@code findByUsername(String)} – looks up a user by username.
 *       Used for authentication and uniqueness checks.</li>
 *   <li>{@code existsByUsername(String)} – checks whether a username exists.</li>
 * </ul>
 *
 * <h2>Inherited methods</h2>
 * {@code findAll(Pageable)}, {@code findById(Long)}, {@code save()}, {@code delete()},
 * etc. via {@link org.springframework.data.jpa.repository.JpaRepository}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // recherche par pseudo
    User findByUsername(String username);

    boolean existsByUsername(String username);
}
