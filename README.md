# Service Request Processing System

A production-ready Spring Boot backend application designed to handle internal business service requests, customer accounts, structured workflow state transitions, and asynchronous lifecycle event auditing using Apache Kafka and PostgreSQL.

---

## Overview

The **Service Request Processing System** provides a centralized RESTful backend for managing customer support and service tickets within an organization. It enforces strict business workflows for processing tickets from creation to closure, preventing invalid status transitions, rejecting duplicate request identifiers, and publishing asynchronous audit events to Apache Kafka on every state change.

---

## Problem Statement

Internal support teams often struggle with inconsistent request processing when ticket states are updated arbitrarily without validation (such as marking an unassigned ticket as resolved), when duplicate ticket IDs create data conflicts, or when synchronous notifications introduce latency into core operations.

This system addresses these challenges by:
- Enforcing an explicit state-machine workflow (`OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED`).
- Guaranteeing data integrity with relational foreign keys and unique constraints in PostgreSQL.
- Offloading event notifications and audit logging to Apache Kafka asynchronously without blocking REST API transactions.
- Providing standardized input validation and centralized error responses with proper HTTP status codes.

---

## Key Features

- **Customer Management:** Create and retrieve customer profiles with unique email verification.
- **Service Request Lifecycle:** Full lifecycle management supporting priority classification (`LOW`, `MEDIUM`, `HIGH`) and category tracking.
- **Strict Workflow State Engine:** Prevents invalid jumps (e.g., `OPEN -> RESOLVED`) and blocks modifications to closed or cancelled requests.
- **Agent Assignment:** Assigns requests to specific support agents, automatically advancing status from `OPEN` to `ASSIGNED`.
- **Filtering & Retrieval:** Dynamic querying by status, priority, and category.
- **Asynchronous Event Auditing:** Publishes lifecycle events (`REQUEST_CREATED`, `REQUEST_ASSIGNED`, `REQUEST_STARTED`, `REQUEST_RESOLVED`, `REQUEST_CLOSED`, `REQUEST_CANCELLED`) to the Kafka topic `service-request-events`.
- **Resilient Kafka Consumer:** Consumes, validates, and logs events safely with defensive checks against malformed or null payloads.
- **Centralized Exception Handling:** Returns uniform JSON error structures across all endpoints with appropriate HTTP status codes (`400`, `404`, `409`, `500`).
- **Comprehensive Automated Tests:** 35 automated unit and MockMvc integration tests with JUnit 5 and Mockito.

---

## Tech Stack

| Technology | Purpose |
| :--- | :--- |
| **Java 17 (LTS)** | Core programming language |
| **Spring Boot 3.2.5** | Application framework |
| **Spring Web (MVC)** | REST API controllers and request routing |
| **Spring Data JPA / Hibernate** | Object-Relational Mapping (ORM) and repository abstractions |
| **PostgreSQL** | Primary relational database |
| **Apache Kafka** | Asynchronous message broker for domain lifecycle events |
| **Jakarta Bean Validation** | Declarative DTO input validation |
| **H2 Database** | In-memory relational database for isolated test execution |
| **JUnit 5 & Mockito** | Unit testing and dependency mocking |
| **MockMvc** | REST API integration testing |
| **Maven** | Dependency management and build automation |
| **Docker & Docker Compose** | Local containerized PostgreSQL and Kafka setup |

---

## System Architecture / Workflow

The system follows a clean layered architecture adhering to the separation of concerns:

```
[ Client / Postman / Frontend ]
               │  HTTP (REST / JSON)
               ▼
      [ Controller Layer ]  ──>  Validates DTOs (@Valid) & Maps HTTP Codes
               │
               ▼
       [ Service Layer ]    ──>  Enforces Business Rules & State Transitions
         │            │
         ▼            ▼
[ Repository Layer ]  [ Kafka Producer ]
         │                    │
         ▼                    ▼
   [ PostgreSQL ]     [ Kafka Broker ]  ──>  [ Kafka Consumer ]  ──>  [ Audit Log ]
```

### Request Lifecycle Workflow

```
       ┌──────────┐
       │   OPEN   │
       └────┬─────┘
            │  Assign Agent
            ▼
       ┌──────────┐
       │ ASSIGNED │
       └────┬─────┘
            │  Start Work
            ▼
     ┌─────────────┐
     │ IN_PROGRESS │
     └──────┬──────┘
            │  Resolve Issue
            ▼
       ┌──────────┐
       │ RESOLVED │
       └────┬─────┘
            │  Close Ticket
            ▼
       ┌──────────┐
       │  CLOSED  │ (Read-Only / Terminal State)
       └──────────┘

* Requests in OPEN, ASSIGNED, or IN_PROGRESS can also transition to CANCELLED.
```

---

## Project Structure

```
service-request-processing-system/
├── .mvn/wrapper/                  # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/srps/
│   │   │   ├── config/            # Kafka topic & infrastructure configuration
│   │   │   │   └── KafkaConfig.java
│   │   │   ├── controller/        # REST API controllers
│   │   │   │   ├── CustomerController.java
│   │   │   │   └── ServiceRequestController.java
│   │   │   ├── dto/               # Request/response DTOs & event payloads
│   │   │   │   ├── AssignRequestDto.java
│   │   │   │   ├── CreateCustomerDto.java
│   │   │   │   ├── CreateServiceRequestDto.java
│   │   │   │   ├── CustomerResponseDto.java
│   │   │   │   ├── ErrorResponseDto.java
│   │   │   │   ├── ServiceRequestEventDto.java
│   │   │   │   ├── ServiceRequestResponseDto.java
│   │   │   │   └── UpdateRequestStatusDto.java
│   │   │   ├── entity/            # JPA entities
│   │   │   │   ├── Customer.java
│   │   │   │   └── ServiceRequest.java
│   │   │   ├── enums/             # Domain enumerations
│   │   │   │   ├── EventType.java
│   │   │   │   ├── Priority.java
│   │   │   │   └── RequestStatus.java
│   │   │   ├── exception/         # Custom exceptions & global handler
│   │   │   │   ├── CustomerNotFoundException.java
│   │   │   │   ├── DuplicateRequestException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidRequestException.java
│   │   │   │   ├── InvalidStatusTransitionException.java
│   │   │   │   └── ServiceRequestNotFoundException.java
│   │   │   ├── kafka/             # Kafka producer and consumer
│   │   │   │   ├── ServiceRequestEventConsumer.java
│   │   │   │   └── ServiceRequestEventProducer.java
│   │   │   ├── repository/        # Spring Data JPA repositories
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   └── ServiceRequestRepository.java
│   │   │   ├── service/           # Business logic layer
│   │   │   │   ├── CustomerService.java
│   │   │   │   └── ServiceRequestService.java
│   │   │   └── ServiceRequestApplication.java
│   │   └── resources/
│   │       ├── application.yml    # Default configuration (PostgreSQL + Kafka)
│   │       ├── schema.sql         # PostgreSQL schema DDL
│   │       └── data.sql           # Initial seed data
│   └── test/
│       ├── java/com/srps/
│       │   ├── controller/        # MockMvc API integration tests
│       │   ├── kafka/             # Kafka consumer unit tests
│       │   └── service/           # Service layer unit tests with Mockito
│       └── resources/
│           └── application.yml    # Test configuration (H2 in-memory)
├── docker-compose.yml             # Docker Compose for PostgreSQL & Kafka
├── CODE_REVIEW_CHECKLIST.md       # Engineering quality checklist
├── DEBUGGING.md                   # Real issue investigation log
├── DESIGN_DECISIONS.md            # Architecture decisions & trade-offs
├── DEVELOPMENT.md                 # Iterative Sprint breakdown
├── INTERVIEW.md                   # Interview questions & preparation guide
├── mvnw / mvnw.cmd                # Maven wrapper executables
└── pom.xml                        # Maven project descriptor
```

---

## How It Works

1. **Client Submission:** A client submits a request through `POST /api/requests`.
2. **Validation:** Bean Validation verifies non-blank fields and valid formats; the service verifies customer existence and checks for duplicate request numbers.
3. **Persistence:** The entity is persisted into PostgreSQL within a database transaction.
4. **Asynchronous Event:** An event (`REQUEST_CREATED`) is emitted to the Kafka topic `service-request-events` with non-blocking callbacks.
5. **State Progression:** Support agents assign tickets (`PUT /api/requests/{id}/assign`), commence work (`IN_PROGRESS`), mark resolution (`RESOLVED`), and close tickets (`CLOSED`), each triggering corresponding validated state transitions and Kafka events.

---

## API Endpoints

### Customer APIs

| Method | Endpoint | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/customers` | Register a new customer |
| `GET` | `/api/customers` | List all registered customers |
| `GET` | `/api/customers/{id}` | Get customer details by ID |

### Service Request APIs

| Method | Endpoint | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/requests` | Create a new service request |
| `GET` | `/api/requests/{id}` | Retrieve request details by ID |
| `GET` | `/api/requests` | Filter requests by `status`, `priority`, or `category` |
| `PUT` | `/api/requests/{id}/assign` | Assign request to a support agent (moves status to `ASSIGNED`) |
| `PUT` | `/api/requests/{id}/status` | Update request status according to workflow rules |
| `PUT` | `/api/requests/{id}/close` | Close a resolved request |
| `PUT` | `/api/requests/{id}/cancel` | Cancel an eligible request (`OPEN`, `ASSIGNED`, or `IN_PROGRESS`) |

---

## Business Rules

1. **Mandatory Fields:** Every request requires a valid `customerId`, `category`, `description`, and `priority`.
2. **Entity Existence:** Requests and customers must exist prior to assignment or association.
3. **Sequential Workflow:** Requests cannot skip lifecycle stages (e.g., `OPEN -> RESOLVED` is rejected).
4. **Assignment Prerequisite:** A request must be in `ASSIGNED` status with an assigned agent before transitioning to `IN_PROGRESS`.
5. **Resolution Prerequisite:** A request must be `IN_PROGRESS` before it can become `RESOLVED`.
6. **Closure Prerequisite:** Only `RESOLVED` requests can transition to `CLOSED`.
7. **Immutability of Closed Requests:** Closed requests cannot be reassigned, transitioned, or reopened.
8. **Terminal State for Cancelled Requests:** Cancelled requests cannot undergo further state modifications.
9. **Uniqueness:** Request numbers (`requestNumber`) and customer email addresses must be unique.

---

## Installation & Setup

### Prerequisites

- **Java 17** or higher
- **Git**
- **Docker** and **Docker Compose** (recommended for local PostgreSQL and Kafka)

### 1. Clone the Repository

```bash
git clone https://github.com/harsharaju1314-hash/Service-Request-Processing-System.git
cd Service-Request-Processing-System
```

### 2. Start PostgreSQL & Kafka with Docker

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL** on `localhost:5432` (Database: `servicerequestdb`, User: `postgres`, Password: `postgres`)
- **Apache Kafka** on `localhost:9092`
- **Zookeeper** on `localhost:2181`

---

## Running the Application

Run the application using the included Maven wrapper:

```bash
# On Windows (PowerShell / Command Prompt)
.\mvnw.cmd spring-boot:run

# On Linux / macOS
./mvnw spring-boot:run
```

The server will start on port `8080`.

---

## Running Automated Tests

Run the full unit and integration test suite:

```bash
# On Windows
.\mvnw.cmd test

# On Linux / macOS
./mvnw test
```

All 35 tests run against an in-memory H2 database with Mockito mocks without requiring external infrastructure running.

---

## Sample Usage

### 1. Create a Customer

**Request:**
```http
POST /api/customers
Content-Type: application/json

{
  "name": "Ravi Kumar",
  "email": "ravi.kumar@example.com"
}
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "name": "Ravi Kumar",
  "email": "ravi.kumar@example.com",
  "createdAt": "2026-09-28T23:25:00"
}
```

### 2. Create a Service Request

**Request:**
```http
POST /api/requests
Content-Type: application/json

{
  "requestNumber": "SR-1001",
  "customerId": 1,
  "category": "Account Access",
  "description": "Unable to access account after password reset",
  "priority": "HIGH"
}
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "requestNumber": "SR-1001",
  "customerId": 1,
  "customerName": "Ravi Kumar",
  "customerEmail": "ravi.kumar@example.com",
  "category": "Account Access",
  "description": "Unable to access account after password reset",
  "priority": "HIGH",
  "status": "OPEN",
  "assignedTo": null,
  "createdAt": "2026-09-28T23:25:10",
  "updatedAt": "2026-09-28T23:25:10"
}
```

### 3. Assign the Request to an Agent

**Request:**
```http
PUT /api/requests/1/assign
Content-Type: application/json

{
  "assignedTo": "agent_sarah"
}
```

**Response (`200 OK`):**
```json
{
  "id": 1,
  "requestNumber": "SR-1001",
  "status": "ASSIGNED",
  "assignedTo": "agent_sarah"
}
```

### 4. Progress to IN_PROGRESS and RESOLVED

**Start work:**
```http
PUT /api/requests/1/status
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

**Resolve request:**
```http
PUT /api/requests/1/status
Content-Type: application/json

{
  "status": "RESOLVED"
}
```

### 5. Close Request

```http
PUT /api/requests/1/close
```

**Response (`200 OK`):**
```json
{
  "id": 1,
  "requestNumber": "SR-1001",
  "status": "CLOSED"
}
```

---

## Future Improvements

- Implement Spring Security with JWT for role-based access control (Admin, Agent, Customer).
- Add pagination support using Spring Data `Pageable` for high-volume request listing.
- Implement an outbox pattern for transactional Kafka event guarantees.
- Add OpenAPI / Swagger UI documentation (`springdoc-openapi`).

---

## What I Learned

- Designing and enforcing domain state machine transitions cleanly within the service layer.
- Integrating Apache Kafka producers and consumers in Spring Boot with error handling deserializers and non-blocking asynchronous callbacks.
- Implementing centralized exception handling with `@RestControllerAdvice` to map business exceptions to meaningful HTTP status codes.
- Authoring isolated unit tests with Mockito and web-tier integration tests with MockMvc.
- Structuring real-world Git feature branches and iterative development sprints.

---

## Author

**Harsha Raju**
- GitHub: [harsharaju1314-hash](https://github.com/harsharaju1314-hash)
