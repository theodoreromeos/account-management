package com.theodore.account.management.config.other;

import com.theodore.queue.common.authserver.RollbackQueueConfig;
import com.theodore.queue.common.config.CommonRabbitMqConfigs;
import com.theodore.queue.common.emails.EmailQueueConfig;
import com.theodore.queue.common.services.MessagingService;
import com.theodore.queue.common.services.MessagingServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonRabbitMqConfigs.class, EmailQueueConfig.class, RollbackQueueConfig.class})
public class RabbitMqAutoConfiguration {

    @Bean
    MessagingService messagingService(RabbitTemplate rabbitTemplate) {
        return new MessagingServiceImpl(rabbitTemplate);
    }

}
