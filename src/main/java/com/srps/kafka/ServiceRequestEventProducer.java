package com.srps.kafka;

import com.srps.dto.ServiceRequestEventDto;
import com.srps.enums.EventType;
import com.srps.enums.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
public class ServiceRequestEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public ServiceRequestEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${spring.kafka.topic.service-request-events:service-request-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishEvent(String requestNumber, EventType eventType, RequestStatus status) {
        ServiceRequestEventDto event = new ServiceRequestEventDto(
                requestNumber,
                eventType,
                status,
                LocalDateTime.now()
        );

        log.info("[Kafka Producer] Publishing event to topic '{}': RequestNumber='{}', EventType='{}', Status='{}'",
                topicName, requestNumber, eventType, status);

        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, requestNumber, event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka Producer] Failed to send event for request '{}' due to: {}",
                            requestNumber, ex.getMessage(), ex);
                } else {
                    log.debug("[Kafka Producer] Event sent successfully for request '{}' with offset [{}]",
                            requestNumber, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("[Kafka Producer] Unexpected exception while publishing event for request '{}': {}",
                    requestNumber, ex.getMessage(), ex);
        }
    }
}
