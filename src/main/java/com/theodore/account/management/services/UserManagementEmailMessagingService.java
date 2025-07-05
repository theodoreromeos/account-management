package com.theodore.account.management.services;

import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.queue.common.emails.EmailDto;

public interface UserManagementEmailMessagingService {

    void sendToEmailService(EmailDto dto);

    void rollbackCredentialsSave(CredentialsRollbackEventDto dto);

}
