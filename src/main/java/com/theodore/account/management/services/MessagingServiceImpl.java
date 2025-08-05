package com.theodore.account.management.services;

import com.theodore.queue.common.authserver.CredentialsQueueEnum;
import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.queue.common.emails.EmailQueueEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingServiceImpl implements MessagingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingServiceImpl.class);

    private final RabbitTemplate rabbitTemplate;

    public MessagingServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendToEmailService(EmailDto emails) {
        LOGGER.info("Sending emails");

        rabbitTemplate.convertAndSend(
                EmailQueueEnum.QUEUE_EXCHANGE.getValue(),
                EmailQueueEnum.QUEUE_ROUTING_KEY.getValue(),
                emails
        );
    }

    @Override
    public void rollbackCredentialsSave(CredentialsRollbackEventDto dto) {
        rabbitTemplate.convertAndSend(
                CredentialsQueueEnum.QUEUE_EXCHANGE.getValue(),
                CredentialsQueueEnum.QUEUE_ROUTING_KEY.getValue(),
                dto
        );
    }

}
