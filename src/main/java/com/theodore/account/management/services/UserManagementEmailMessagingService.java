package com.theodore.account.management.services;

import com.theodore.queue.common.authserver.CredentialsQueueEnum;
import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.queue.common.emails.EmailQueueEnum;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserManagementEmailMessagingService {

    private final RabbitTemplate rabbitTemplate;

    public UserManagementEmailMessagingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendToEmailService(EmailDto dto) {
        rabbitTemplate.convertAndSend(
                EmailQueueEnum.QUEUE_EXCHANGE.getValue(),
                EmailQueueEnum.QUEUE_ROUTING_KEY.getValue(),
                dto
        );
    }

    public void rollbackCredentialsSave(CredentialsRollbackEventDto dto) {
        rabbitTemplate.convertAndSend(
                CredentialsQueueEnum.QUEUE_EXCHANGE.getValue(),
                CredentialsQueueEnum.QUEUE_ROUTING_KEY.getValue(),
                dto
        );
    }

}
