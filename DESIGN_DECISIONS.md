# Design Decisions & Technical Trade-offs

This document outlines the architectural decisions, design choices, and technology trade-offs made during the design and implementation of the Service Request Processing System.

---

## 1. Why Java 17?

- **Strong Typing & Compile-Time Safety:** Prevents entire classes of runtime errors through static typing.
- **Modern Language Features:** Java 17 LTS provides enhanced switch expressions, pattern matching, records (if needed), improved JVM garbage collection, and long-term stability.
- **Enterprise Standard:** Java is the industry standard for robust, high-performance back-end business systems.

---

## 2. Why Spring Boot 3.x?

- **Productivity & Convention Over Configuration:** Eliminates boilerplate XML configuration while providing opinionated starters for web, data persistence, validation, and messaging.
- **Embedded Server & Container Readiness:** Built-in embedded Tomcat enables seamless local execution and self-contained JAR packaging.
- **Centralized Ecosystem:** First-class support for Dependency Injection, Spring Data JPA, Hibernate, Bean Validation, and Spring Kafka in a single unified framework.

---

## 3. Why PostgreSQL (Relational Database)?

- **ACID Compliance & Data Integrity:** Service requests and financial/customer operations require strict transactional integrity, referential constraints (`FOREIGN KEY` to Customer), and unique constraints (e.g. `request_number` and `email`).
- **PostgreSQL vs In-Memory (H2) for Production:** While H2 is great for fast in-memory unit/integration tests, PostgreSQL provides durable storage, complex indexing (B-Tree indices on `status`, `priority`, `category`), and realistic production concurrency.

---

## 4. Why Spring Data JPA & Hibernate?

- **Object-Relational Mapping (ORM):** Maps Java domain models to database tables cleanly, reducing manual SQL string concatenation and JDBC boilerplate.
- **Declarative Repository Interfaces:** Provides pre-built CRUD methods (`findById`, `save`, `findAll`) and readable JPQL queries (`findWithFilters`) with parameter binding to prevent SQL injection.
- **Transaction Management:** Declarative `@Transactional` annotations ensure atomic commits and automatic rollbacks on unhandled runtime exceptions.

---

## 5. Why Apache Kafka (Asynchronous Messaging)?

- **Decoupled Architecture:** Business operations (such as creating or closing a request) should complete promptly without waiting for downstream processes (such as email notifications, analytics, or audit logging).
- **Event-Driven Resilience:** If downstream services (e.g., notification systems) experience latency or temporary downtime, events are safely buffered in the Kafka topic (`service-request-events`) and processed once the consumer recovers.
- **Auditability:** Every lifecycle change produces an immutable event stream with timestamps and state transitions.

---

## 6. Why Layered Architecture (Controller → Service → Repository)?

- **Separation of Concerns:**
  - **Controller Layer:** Handles HTTP requests, URL path variable extraction, query parameters, Bean Validation parsing, and HTTP status code formatting.
  - **Service Layer:** Houses all business rules, workflow state machine transitions, entity transformations, and event triggers. Contains zero web-specific code.
  - **Repository Layer:** Encapsulates database queries, JPA entity persistence, and data retrieval.
- **Testability:** Decoupled layers make it straightforward to isolate and unit-test business logic using Mockito without launching web servers or database connections.

---

## 7. Why DTOs (Data Transfer Objects)?

- **Security & Encapsulation:** Prevents over-posting attacks and avoids leaking internal database schemas, foreign key IDs, or sensitive customer details.
- **Validation Boundaries:** Bean Validation annotations (`@NotBlank`, `@Email`, `@NotNull`) sit squarely on the incoming DTO contracts rather than polluting persistent JPA entities.
- **API Versioning & Stability:** Allows internal database schemas to evolve independently from external REST API contracts.
