package com.bhak.project.repository;

import com.bhak.project.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acces aux donnees d'authentification
@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {

    Credentials findByEmail(String email);

    Credentials findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
