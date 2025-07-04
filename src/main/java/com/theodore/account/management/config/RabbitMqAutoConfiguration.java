package com.theodore.account.management.config;

import com.theodore.queue.common.config.CommonRabbitMqConfigs;
import com.theodore.queue.common.emails.EmailQueueConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonRabbitMqConfigs.class, EmailQueueConfig.class})
public class RabbitMqAutoConfiguration {
}
