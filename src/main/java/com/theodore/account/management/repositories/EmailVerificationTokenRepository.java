package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {


}
