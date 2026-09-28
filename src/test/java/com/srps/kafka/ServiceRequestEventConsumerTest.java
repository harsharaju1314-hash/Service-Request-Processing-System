package com.srps.kafka;

import com.srps.dto.ServiceRequestEventDto;
import com.srps.enums.EventType;
import com.srps.enums.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ServiceRequestEventConsumerTest {

    private ServiceRequestEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ServiceRequestEventConsumer();
    }

    @Test
    @DisplayName("Should process valid Kafka event safely without exception")
    void shouldProcessValidEventSafely() {
        ServiceRequestEventDto event = new ServiceRequestEventDto(
                "SR-1001",
                EventType.REQUEST_CREATED,
                RequestStatus.OPEN,
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> consumer.consumeEvent(event));
    }

    @Test
    @DisplayName("Should discard null event safely without crashing consumer")
    void shouldHandleNullEventSafely() {
        assertDoesNotThrow(() -> consumer.consumeEvent(null));
    }

    @Test
    @DisplayName("Should discard malformed event with blank request number safely")
    void shouldHandleBlankRequestNumberSafely() {
        ServiceRequestEventDto event = new ServiceRequestEventDto(
                "   ",
                EventType.REQUEST_CREATED,
                RequestStatus.OPEN,
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> consumer.consumeEvent(event));
    }

    @Test
    @DisplayName("Should discard malformed event with missing status safely")
    void shouldHandleMissingStatusSafely() {
        ServiceRequestEventDto event = new ServiceRequestEventDto(
                "SR-1001",
                EventType.REQUEST_CREATED,
                null,
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> consumer.consumeEvent(event));
    }
}
