package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Query(value = "select * from email_verification_token token where token.user_id = ?1 and token.status = 'PENDING'",
            nativeQuery = true)
    Optional<EmailVerificationToken> findByUserIdAndStatusPending(String userId);

    @Modifying
    @Query("delete from EmailVerificationToken e where e.status <> :status")
    int deleteByStatusNot(@Param("status") EmailVerificationToken.VerificationStatus status);

}
