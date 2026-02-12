package com.bhak.project.repository;

import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acces aux roles en base
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(RoleType name);

    boolean existsByName(RoleType name);
}
