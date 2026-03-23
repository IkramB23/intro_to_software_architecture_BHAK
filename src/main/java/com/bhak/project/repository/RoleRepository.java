package com.bhak.project.repository;

import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Role} entity.
 *
 * <h2>Purpose</h2>
 * Accesses the {@code tbl_roles} table. Used by {@code DataInitializer}
 * to create roles at startup and by services to resolve
 * a {@link RoleType} to a {@code Role} entity.
 *
 * <h2>Methods</h2>
 * <ul>
 *   <li>{@code findByName(RoleType)} – returns the matching role.</li>
 *   <li>{@code existsByName(RoleType)} – checks its existence (initialisation).</li>
 * </ul>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(RoleType name);

    boolean existsByName(RoleType name);
}
