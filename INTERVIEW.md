# Interview Preparation Guide - Service Request Processing System

This guide contains concise, technically accurate, and natural answers to common interview questions based on the actual implementation of this project.

---

## 1. General Project Questions

### Q1: Explain your project.
**Answer:**
"I built a Service Request Processing System using Java 17, Spring Boot, PostgreSQL, and Apache Kafka. It manages internal business support requests through a controlled lifecycle: `OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED`. The application validates business rules (preventing illegal status jumps or modifications on closed requests), persists data with Spring Data JPA and PostgreSQL, and asynchronously broadcasts lifecycle events over Apache Kafka to a background consumer."

### Q2: Why did you choose this project?
**Answer:**
"Rather than building a generic student CRUD app (like a basic todo list or book store), I wanted to build a realistic internal business application that demonstrates real enterprise concepts: domain state machines, layered architecture, data integrity, asynchronous messaging, Bean Validation, and unit/integration testing."

### Q3: What problem does it solve?
**Answer:**
"In many organizations, customer support requests and IT tickets are mishandled when status updates lack validation or when notifications block the main transaction. This system enforces strict operational workflows (for example, ensuring a ticket cannot be resolved before it is assigned and worked on), prevents duplicate request numbers, and decouples auditing/notifications through asynchronous Kafka events."

### Q4: What was your contribution?
**Answer:**
"I designed and implemented the entire monolithic backend service from scratch: configured Spring Boot with Maven, designed the relational database schema, developed REST endpoints and DTO contracts, wrote the core workflow validation logic, integrated Apache Kafka producers and consumers with error handling, and authored 35 JUnit 5/Mockito unit and MockMvc integration tests."

---

## 2. Java Core

### Q1: Why Java?
**Answer:**
"Java provides strong static typing, compile-time safety, memory management via garbage collection, and an extensive enterprise ecosystem. Java 17 LTS also offers modern language enhancements, performance improvements, and long-term support."

### Q2: How did you structure the application?
**Answer:**
"I organized the codebase into a standard layered architecture under `com.srps`:
- `controller`: REST endpoints and HTTP request mapping
- `service`: Core business rules and transaction boundaries
- `repository`: Data access with Spring Data JPA
- `entity`: JPA entities (`Customer`, `ServiceRequest`)
- `dto`: API input/output models and event payloads
- `exception`: Custom exceptions and `@RestControllerAdvice`
- `kafka`: Kafka producer, consumer, and topic configuration
- `config`: Application and messaging configuration"

### Q3: Where is your business logic located?
**Answer:**
"All business logic resides exclusively in the service layer (`ServiceRequestService` and `CustomerService`). Controllers only handle request validation and response mapping, while repositories only handle queries. For instance, checks ensuring a ticket is assigned before starting progress or preventing changes to closed requests are strictly inside `ServiceRequestService`."

### Q4: How did you handle exceptions?
**Answer:**
"I created domain-specific unchecked exceptions extending `RuntimeException` (e.g., `CustomerNotFoundException`, `InvalidStatusTransitionException`, `DuplicateRequestException`). I then implemented a centralized `@RestControllerAdvice` class (`GlobalExceptionHandler`) that catches these exceptions and maps them to appropriate HTTP status codes (400, 404, 409, 500) with a uniform JSON error payload."

---

## 3. Spring Boot

### Q1: Why Spring Boot?
**Answer:**
"Spring Boot drastically reduces boilerplate through auto-configuration, starter dependencies (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`), embedded Tomcat server, and seamless integration with production-ready features like logging and externalized configuration."

### Q2: What is Dependency Injection (DI) and how did you use it?
**Answer:**
"Dependency Injection is an Inversion of Control (IoC) pattern where the Spring container supplies object dependencies at runtime rather than having classes instantiate them with `new`. I used constructor injection throughout the application (e.g., injecting `ServiceRequestRepository` and `KafkaTemplate` into `ServiceRequestService`), which makes classes immutable and easily mockable in unit tests."

### Q3: What is REST?
**Answer:**
"REST (Representational State Transfer) is an architectural style for web services using stateless HTTP methods. In my application, I mapped:
- `POST` for resource creation (`/api/requests`, `/api/customers`)
- `GET` for retrieval and filtering (`/api/requests/{id}`, `/api/requests?status=OPEN`)
- `PUT` for state mutations (`/api/requests/{id}/assign`, `/api/requests/{id}/status`, `/api/requests/{id}/close`)"

### Q4: Why use DTOs instead of exposing JPA Entities?
**Answer:**
"DTOs (Data Transfer Objects) decouple internal database schemas from external API contracts. This prevents over-posting attacks, avoids circular references during JSON serialization, eliminates leaking internal foreign key structures, and allows us to place Bean Validation annotations directly on input payloads."

---

## 4. Database & JPA / Hibernate

### Q1: Why PostgreSQL?
**Answer:**
"PostgreSQL is an ACID-compliant open-source relational database. For service requests, data consistency, foreign keys (`customer_id`), unique constraints (`request_number`, `email`), and indexed lookup fields (`status`, `priority`) are essential."

### Q2: How are Customer and ServiceRequest related?
**Answer:**
"They have a `@ManyToOne` relationship from `ServiceRequest` to `Customer` (a customer can have many service requests, but each request belongs to one customer). In `ServiceRequest`, it is mapped with `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)`."

### Q3: What is JPA and what is Hibernate?
**Answer:**
"JPA (Jakarta Persistence API) is a standard specification in Java for object-relational mapping (ORM). Hibernate is the actual ORM framework that implements the JPA specification, handling SQL generation, caching, and entity lifecycle management under the hood."

---

## 5. Apache Kafka

### Q1: Why did you use Kafka?
**Answer:**
"To asynchronously publish domain events (`REQUEST_CREATED`, `REQUEST_ASSIGNED`, `REQUEST_STARTED`, `REQUEST_RESOLVED`, `REQUEST_CLOSED`, `REQUEST_CANCELLED`) whenever request state changes occur. This decouples core request processing from background auditing, logging, or notifications without impacting HTTP API response times."

### Q2: What is a Kafka Topic, Producer, and Consumer?
**Answer:**
"- **Topic:** A named category or partitioned stream where records are published (`service-request-events`).
- **Producer:** The component (`ServiceRequestEventProducer`) using Spring's `KafkaTemplate` to send messages to the topic.
- **Consumer:** The component (`ServiceRequestEventConsumer`) using `@KafkaListener` to read and process records from the topic."

### Q3: What happens if the consumer receives an invalid or null event?
**Answer:**
"The consumer has defensive validation guards that inspect incoming payloads. If a payload is null, has a blank `requestNumber`, or missing `eventType`/`status`, the consumer logs a warning and safely discards the message without throwing unhandled exceptions that could crash the listener container. In addition, `ErrorHandlingDeserializer` is configured to prevent deserialization poison pills."

---

## 6. Testing

### Q1: Why JUnit 5 and Mockito?
**Answer:**
"JUnit 5 is the modern Java testing framework providing annotations like `@Test`, `@DisplayName`, and `@BeforeEach`. Mockito allows mocking dependencies (like `ServiceRequestRepository` or `KafkaTemplate`) so we can test service layer business rules in total isolation without hitting a real database or message broker."

### Q2: What did you test?
**Answer:**
"I wrote 35 comprehensive automated tests:
- Service layer unit tests for all 10 core business cases (creation, missing customer, duplicate request number, assignment, invalid transitions, closed immutability, cancellation, and Kafka publishing).
- Kafka consumer unit tests for safe handling of valid and invalid payloads.
- Controller integration tests using `@WebMvcTest` and `MockMvc` verifying HTTP status codes (200, 201, 400, 404, 409) and JSON responses."

### Q3: How did you test invalid status transitions?
**Answer:**
"Using JUnit 5's `assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.updateStatus(id, dto))` and verifying with Mockito that `repository.save()` was never called."

---

## 7. Debugging & Problem Solving

### Q1: Tell me about a bug you encountered and how you fixed it.
**Answer:**
"During development of `ServiceRequestService`, I noticed that a request could transition to `IN_PROGRESS` without an assigned agent if an API caller sent a status update directly. The transition logic checked that the previous status was `ASSIGNED`, but didn't verify that `assignedTo` was non-null and not blank.
I investigated the code, identified the missing attribute precondition in `validateStatusTransition`, added an explicit check for `request.getAssignedTo() != null && !request.getAssignedTo().trim().isEmpty()`, and added a regression unit test `shouldThrowExceptionWhenMovingToInProgressWithoutAssignment()` to prevent regressions."

---

## 8. Agile Development

### Q1: How did you divide the project into iterations?
**Answer:**
"I structured the project into 4 iterative sprints:
- **Sprint 1:** Core Spring Boot setup, JPA entities, database schema, and basic CRUD.
- **Sprint 2:** Business workflow state machine, assignment, cancellation, and centralized exception handling.
- **Sprint 3:** Apache Kafka producer, consumer, topic setup, and event handling.
- **Sprint 4:** JUnit 5/Mockito test suite, MockMvc API testing, bug fixing, and documentation."

---

## 9. Code Review

### Q1: What do you look for during a code review?
**Answer:**
"I check against our standard checklist:
1. Adherence to naming conventions and clean code practices.
2. Clear separation of concerns (no business logic in controllers, no DB queries in services).
3. Thorough input validation and appropriate HTTP status codes.
4. Correct use of database transactions and constraints.
5. Defensive exception handling without swallowing errors.
6. Sufficient automated test coverage for both positive and negative boundary scenarios."
