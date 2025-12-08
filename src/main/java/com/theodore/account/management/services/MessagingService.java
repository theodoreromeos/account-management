package com.theodore.account.management.services;

import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.queue.common.emails.EmailDto;

public interface MessagingService {

    void sendToEmailService(EmailDto dto);

    /**
     * Sends a rollback message to the auth server via a queue.
     */
    void rollbackCredentialsSave(CredentialsRollbackEventDto dto);

}
