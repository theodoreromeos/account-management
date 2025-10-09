package com.theodore.account.management.utils;

import com.theodore.account.management.services.EmailTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VerificationTokenCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationTokenCleaner.class);

    private final EmailTokenService emailTokenService;

    public VerificationTokenCleaner(EmailTokenService emailTokenService) {
        this.emailTokenService = emailTokenService;
    }

    @Scheduled(cron = "0 58 13 * * *")
    public void cleanUsedVerificationTokens() {
        LOGGER.trace("Cleaning up email verification tokens");
        emailTokenService.cleanUsedVeriricationTokens();
    }

}
