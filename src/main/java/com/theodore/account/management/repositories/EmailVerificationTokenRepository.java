package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Query(value = "select * from email_verification_token token where token.user_id = ?1 and token.status = 'PENDING'",
            nativeQuery = true)
    Optional<EmailVerificationToken> findByUserIdAndStatusPending(String userId);

    long deleteByStatus(EmailVerificationToken.VerificationStatus status);

}
