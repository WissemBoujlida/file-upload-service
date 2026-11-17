package com.service.file.upload.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaFileTopicConfig {
    @Bean
    public NewTopic fileTopic() {
        return TopicBuilder
                .name("file")
                .build();
    }
}
