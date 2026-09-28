# Development Process & Iterations - Service Request Processing System

This project was built following an iterative Agile methodology broken down into four distinct, realistic development iterations (Sprints).

---

## Sprint 1 — Core Application & Domain Foundations

**Goal:** Establish the Spring Boot foundation, PostgreSQL database persistence layer, domain entities, and fundamental customer and request CRUD operations.

### Scope & Tasks Completed
- Initialized Maven build setup with Spring Boot 3.2.x, Java 17, Spring Data JPA, Hibernate, PostgreSQL driver, and H2 test database.
- Created JPA domain models:
  - `Customer` entity with unique email constraint and automated audit timestamp (`createdAt`).
  - `ServiceRequest` entity with `ManyToOne` relationship to Customer, unique request number constraint, enums (`Priority`, `RequestStatus`), and lifecycle timestamps (`createdAt`, `updatedAt`).
- Created Spring Data JPA repositories:
  - `CustomerRepository`
  - `ServiceRequestRepository` with JPQL filtering queries (`findWithFilters`).
- Developed DTOs for request/response decoupling: `CreateCustomerDto`, `CustomerResponseDto`, `CreateServiceRequestDto`, `ServiceRequestResponseDto`.
- Implemented `CustomerService` and `CustomerController` (`POST /api/customers`, `GET /api/customers`, `GET /api/customers/{id}`).
- Implemented baseline `ServiceRequestController` (`POST /api/requests`, `GET /api/requests/{id}`, `GET /api/requests`).
- Configured PostgreSQL DDL `schema.sql` and `data.sql` for seed data.

---

## Sprint 2 — Business Workflow, State Transitions & Error Handling

**Goal:** Implement strict business rules, state transition validation, assignment, cancellation, and centralized exception handling.

### Scope & Tasks Completed
- Implemented workflow rules and validations in `ServiceRequestService`:
  - `assignRequest` (`PUT /api/requests/{id}/assign`): Assigns an agent to an eligible request and updates status to `ASSIGNED`.
  - `updateStatus` (`PUT /api/requests/{id}/status`): Enforces allowed state transitions (`ASSIGNED -> IN_PROGRESS -> RESOLVED`).
  - `closeRequest` (`PUT /api/requests/{id}/close`): Allows closing only already `RESOLVED` requests.
  - `cancelRequest` (`PUT /api/requests/{id}/cancel`): Permits cancellation of `OPEN`, `ASSIGNED`, or `IN_PROGRESS` requests, rejecting already `CLOSED` or `RESOLVED` requests.
  - Guard checks against modifications to `CLOSED` or `CANCELLED` requests.
- Created custom exception hierarchy:
  - `CustomerNotFoundException`
  - `ServiceRequestNotFoundException`
  - `InvalidStatusTransitionException`
  - `DuplicateRequestException`
  - `InvalidRequestException`
- Built centralized `GlobalExceptionHandler` with `@RestControllerAdvice` returning structured `ErrorResponseDto` with standard HTTP status codes (400, 404, 409, 500).
- Configured Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@NotNull`, `@Size`) across all incoming API DTOs.

---

## Sprint 3 — Asynchronous Event Processing with Apache Kafka

**Goal:** Integrate Apache Kafka to publish lifecycle audit events asynchronously upon state changes and process events safely in a background consumer.

### Scope & Tasks Completed
- Added Apache Kafka dependency (`spring-kafka`) and configured topic creation bean (`KafkaConfig` creating topic `service-request-events`).
- Defined lightweight, non-sensitive event payload DTO (`ServiceRequestEventDto`) containing `requestNumber`, `eventType`, `status`, and `timestamp`.
- Implemented `ServiceRequestEventProducer`:
  - Integrated `KafkaTemplate<String, Object>` to publish events on:
    - `REQUEST_CREATED`
    - `REQUEST_ASSIGNED`
    - `REQUEST_STARTED`
    - `REQUEST_RESOLVED`
    - `REQUEST_CLOSED`
    - `REQUEST_CANCELLED`
  - Added non-blocking asynchronous callbacks with SLF4J logging for error handling.
- Implemented `ServiceRequestEventConsumer`:
  - Built `@KafkaListener` subscribed to `service-request-events`.
  - Added defensive payload validation (safely discarding null or malformed payloads without crashing the listener).
  - Integrated structured audit logging per event type.
- Configured `docker-compose.yml` defining local PostgreSQL and Apache Kafka (with Zookeeper) services for single-command environment startup.

---

## Sprint 4 — Quality Assurance, Testing, Refactoring & Documentation

**Goal:** Author comprehensive automated unit and API integration tests, identify and document real bugs, conduct code self-review, and produce complete technical documentation.

### Scope & Tasks Completed
- Authored JUnit 5 and Mockito unit tests in `CustomerServiceTest`, `ServiceRequestServiceTest`, and `ServiceRequestEventConsumerTest` covering all 10 core business scenarios:
  1. Successful request creation
  2. Customer not found handling
  3. Duplicate request number rejection
  4. Successful agent assignment
  5. Missing request assignment failure
  6. Valid workflow status transitions
  7. Invalid status transition rejection
  8. Closed request immutability verification
  9. Request cancellation rules
  10. Kafka event publishing verification
- Authored Spring MVC Integration tests using `@WebMvcTest` and `MockMvc` in `CustomerControllerIntegrationTest` and `ServiceRequestControllerIntegrationTest`.
- Performed build and test verification via Maven (`mvnw test`) achieving 100% test pass rate across 35 test cases.
- Investigated, resolved, and documented genuine development issues in `DEBUGGING.md`.
- Completed architectural design rationale in `DESIGN_DECISIONS.md`.
- Created code quality checklist in `CODE_REVIEW_CHECKLIST.md`.
- Prepared interview preparation guide in `INTERVIEW.md`.
- Authored production-grade `README.md` with complete API specifications and execution instructions.
