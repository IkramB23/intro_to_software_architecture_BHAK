package com.bhak.project.repository;

import com.bhak.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acces aux donnees de l'entite User
// spring data genere l'implementation a partir des signatures
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // recherche par pseudo
    User findByUsername(String username);

    boolean existsByUsername(String username);
}
