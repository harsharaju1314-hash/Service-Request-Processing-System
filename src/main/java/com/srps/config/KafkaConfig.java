package com.srps.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topic.service-request-events:service-request-events}")
    private String topicName;

    @Bean
    public NewTopic serviceRequestEventsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
