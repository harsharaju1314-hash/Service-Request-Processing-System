# Debugging Log - Service Request Processing System

This document records genuine technical issues encountered, analyzed, and resolved during the development and testing of the Service Request Processing System.

---

## Issue 1: Incomplete Pre-condition Validation on Transition to `IN_PROGRESS`

### Problem
During the implementation of `ServiceRequestService.updateStatus`, a request in `OPEN` status could be directly forced into `IN_PROGRESS` via the status update endpoint if an invalid transition check was missed, or an `ASSIGNED` request with an unassigned agent (e.g. empty string or blank value) could proceed to `IN_PROGRESS`, violating **Business Rule 4**: *"A request must be assigned before moving to IN_PROGRESS"*.

### Investigation
- Analyzed status transition validation logic in `ServiceRequestService.validateStatusTransition`.
- Observed that initial checks only compared state enum transitions (`from != RequestStatus.ASSIGNED`) without asserting that `assignedTo` is non-null and non-blank.
- Tested a scenario where `status` was set to `ASSIGNED` without a valid agent name; subsequent transition to `IN_PROGRESS` passed silently without assigning an agent.

### Root Cause
The state machine verification relied solely on the enum status flag without checking the entity attribute precondition (`request.getAssignedTo() != null && !request.getAssignedTo().trim().isEmpty()`).

### Fix
Updated `validateStatusTransition` in `ServiceRequestService.java` to explicitly enforce both status and assignment constraints:
```java
case IN_PROGRESS:
    if (from != RequestStatus.ASSIGNED || request.getAssignedTo() == null || request.getAssignedTo().trim().isEmpty()) {
        throw new InvalidStatusTransitionException(
                String.format("Request %s cannot move from %s to IN_PROGRESS. It must be ASSIGNED to an agent first.",
                        request.getRequestNumber(), from)
        );
    }
    break;
```

### Regression Test
Added unit test `shouldThrowExceptionWhenMovingToInProgressWithoutAssignment()` in `ServiceRequestServiceTest.java` to guarantee that unassigned requests cannot transition to `IN_PROGRESS`.

---

## Issue 2: Kafka Consumer Vulnerability to Malformed / Null Event Payloads

### Problem
When receiving malformed JSON messages, empty payloads, or events with missing mandatory fields (such as `requestNumber` or `eventType`), the Kafka consumer listener could encounter unhandled `NullPointerException` or deserialization errors, potentially halting partition consumption or causing consumer loop failures.

### Investigation
- Analyzed `ServiceRequestEventConsumer.consumeEvent`.
- Identified that standard Spring `@KafkaListener` without payload validation directly accessed getters on incoming objects (e.g., `event.getRequestNumber()`), throwing `NullPointerException` if the payload deserialized as `null`.
- Furthermore, missing Spring Kafka `ErrorHandlingDeserializer` configuration could cause fatal deserialization loops on poisoned messages.

### Root Cause
1. Incomplete consumer defensive checks on incoming event objects.
2. Incomplete configuration of fallback error-handling deserializers in `application.yml`.

### Fix
1. Configured `org.springframework.kafka.support.serializer.ErrorHandlingDeserializer` in `application.yml` for the consumer value deserializer.
2. Implemented null-safety and defensive attribute validation guards at the start of `ServiceRequestEventConsumer.consumeEvent`:
```java
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
```

### Regression Test
Added dedicated unit tests in `ServiceRequestEventConsumerTest.java`:
- `shouldProcessValidEventSafely()`
- `shouldHandleNullEventSafely()`
- `shouldHandleBlankRequestNumberSafely()`
- `shouldHandleMissingStatusSafely()`
All tests pass cleanly, ensuring graceful degradation and safety on invalid messages.
