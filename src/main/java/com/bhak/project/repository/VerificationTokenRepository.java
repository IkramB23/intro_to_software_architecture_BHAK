package com.bhak.project.repository;

import com.bhak.project.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// acces aux tokens de verification d'e-mail
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
}
