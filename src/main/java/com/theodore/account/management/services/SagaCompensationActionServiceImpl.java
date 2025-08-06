package com.theodore.account.management.services;

import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SagaCompensationActionServiceImpl implements SagaCompensationActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaCompensationActionServiceImpl.class);

    private final MessagingService messagingService;

    public SagaCompensationActionServiceImpl(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public void authServerCredentialsRollback(String authUserId, String email, String logMsg) {
        // Compensation: rollback to user credentials from auth-server
        email = email != null ? email : "unknown";
        LOGGER.info("{} process failed. Rolling back credentials from auth server for user : {} ", logMsg, email);
        var rollbackEvent = new CredentialsRollbackEventDto(authUserId);
        messagingService.rollbackCredentialsSave(rollbackEvent);
    }

}
