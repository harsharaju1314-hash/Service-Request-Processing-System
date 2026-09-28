# Code Review & Self-Review Checklist

This checklist defines standard engineering quality criteria utilized for self-reviewing and evaluating code changes in the Service Request Processing System.

---

## 1. Naming Conventions & Code Style
- [x] Class names use `PascalCase` (e.g., `ServiceRequestService`, `CustomerResponseDto`).
- [x] Method and variable names use `camelCase` and clearly express intent (e.g., `validateStatusTransition`, `requestNumber`).
- [x] Enums use `UPPER_SNAKE_CASE` (e.g., `IN_PROGRESS`, `REQUEST_CREATED`).
- [x] No single-letter or ambiguous abbreviations in domain logic.

---

## 2. Architecture & Separation of Concerns
- [x] Controllers do not contain business logic; they only handle request mapping, DTO validation, and HTTP responses.
- [x] Service classes encapsulate all validation rules, domain logic, and transactional boundaries.
- [x] Repositories contain only database access methods; no business calculations.
- [x] Kafka producers and consumers are decoupled into dedicated packages (`com.srps.kafka`).

---

## 3. Input Validation & Security
- [x] All REST request bodies are validated using Bean Validation annotations (`@Valid`, `@NotBlank`, `@NotNull`, `@Email`, `@Size`).
- [x] Business pre-conditions (e.g., customer existence, duplicate request numbers, valid assigned agent) are checked in the service layer.
- [x] Parameterized JPQL queries are used to prevent SQL injection vulnerabilities.
- [x] DTOs are used for all public API endpoints to prevent entity leakage.

---

## 4. Exception Handling & HTTP Status Codes
- [x] No generic `catch (Exception e)` suppressing errors without logging.
- [x] Dedicated custom exceptions used for business conflicts:
  - `CustomerNotFoundException` / `ServiceRequestNotFoundException` (404)
  - `InvalidStatusTransitionException` / `DuplicateRequestException` (409)
  - `InvalidRequestException` / `MethodArgumentNotValidException` (400)
- [x] Centralized `@RestControllerAdvice` produces uniform JSON error responses with timestamp, HTTP status, error type, and descriptive message.

---

## 5. Database & Transactions
- [x] `@Transactional` applied appropriately on state-mutating service methods; `@Transactional(readOnly = true)` on query methods.
- [x] Foreign key constraints, unique constraints, and indices properly defined in `schema.sql` and JPA entities.
- [x] Entity lifecycle timestamps handled automatically via `@PrePersist` and `@PreUpdate`.

---

## 6. Messaging & Kafka
- [x] Kafka event payload (`ServiceRequestEventDto`) contains only necessary event metadata (`requestNumber`, `eventType`, `status`, `timestamp`) without sensitive customer PII.
- [x] Consumer has defensive null/empty payload guards to prevent consumer thread failure.
- [x] Producer uses asynchronous callbacks to log success/failure without blocking the main business execution thread.

---

## 7. Logging & Observability
- [x] SLF4J logger instantiated via `LoggerFactory.getLogger(Class.class)`.
- [x] Appropriate log levels used:
  - `INFO`: Normal lifecycle milestones (request created, assigned, closed).
  - `WARN`: Handled business anomalies (invalid transition attempt, duplicate number).
  - `ERROR`: Unhandled exceptions and Kafka send failures.
  - `DEBUG`: Query parameters and internal transitions.
- [x] No passwords, credentials, or sensitive personal information logged.

---

## 8. Unit & Integration Testing
- [x] All core business workflows covered by JUnit 5 and Mockito tests.
- [x] Both positive paths (valid transitions) and negative paths (illegal jumps, duplicates, missing entities) tested.
- [x] MockMvc API tests verify HTTP status codes, JSON response structure, and validation error payloads.
- [x] All tests run and pass locally with 100% success rate (`mvnw test`).
