package com.srps.kafka;

import com.srps.dto.ServiceRequestEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestEventConsumer.class);

    @KafkaListener(
            topics = "${spring.kafka.topic.service-request-events:service-request-events}",
            groupId = "${spring.kafka.consumer.group-id:service-request-group}"
    )
    public void consumeEvent(@Payload(required = false) ServiceRequestEventDto event) {
        log.info("[Kafka Consumer] Received raw event payload: {}", event);

        // 1. Validate the event safely
        if (event == null) {
            log.warn("[Kafka Consumer] Received null or unparseable event payload. Discarding safely.");
            return;
        }

        if (event.getRequestNumber() == null || event.getRequestNumber().trim().isEmpty()) {
            log.warn("[Kafka Consumer] Received invalid event with missing requestNumber: {}. Discarding.", event);
            return;
        }

        if (event.getEventType() == null) {
            log.warn("[Kafka Consumer] Received invalid event with missing eventType: {}. Discarding.", event);
            return;
        }

        if (event.getStatus() == null) {
            log.warn("[Kafka Consumer] Received invalid event with missing status: {}. Discarding.", event);
            return;
        }

        // 2. Log the event
        log.info("[Kafka Consumer] Valid event processed: RequestNumber='{}', EventType='{}', Status='{}', Timestamp='{}'",
                event.getRequestNumber(), event.getEventType(), event.getStatus(), event.getTimestamp());

        // 3. Process event appropriately (e.g., downstream auditing / notifications)
        processEvent(event);
    }

    protected void processEvent(ServiceRequestEventDto event) {
        switch (event.getEventType()) {
            case REQUEST_CREATED:
                log.info("[Audit] New service request logged in audit queue: {}", event.getRequestNumber());
                break;
            case REQUEST_ASSIGNED:
                log.info("[Audit] Request assignment logged for: {}", event.getRequestNumber());
                break;
            case REQUEST_STARTED:
                log.info("[Audit] Request execution in progress for: {}", event.getRequestNumber());
                break;
            case REQUEST_RESOLVED:
                log.info("[Audit] Request marked as resolved for: {}", event.getRequestNumber());
                break;
            case REQUEST_CLOSED:
                log.info("[Audit] Request closed permanently for: {}", event.getRequestNumber());
                break;
            case REQUEST_CANCELLED:
                log.info("[Audit] Request cancelled and archived for: {}", event.getRequestNumber());
                break;
            default:
                log.debug("[Audit] Event processed: {}", event);
        }
    }
}
