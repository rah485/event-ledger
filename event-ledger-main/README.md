# Event Ledger

## Architecture Overview

Event Ledger is implemented as two independently runnable Spring Boot microservices:

- Gateway Service (public API): receives events, validates input, enforces idempotency, stores events locally, and calls Account Service synchronously.
- Account Service (internal API): stores account and transaction state, computes balances, and serves account-level queries.

High-level flow:

1. Client sends transaction event to Gateway.
2. Gateway validates and checks idempotency by eventId.
3. Gateway stores the event in its own H2 database.
4. Gateway calls Account Service to apply the transaction.
5. Account Service updates balance and transaction history in its own H2 database.

The services do not share database state.

## Tech Stack for commit 5

- Java 17
- Spring Boot 3.5.3
- Spring Web
- Spring Data JPA
- H2
- Spring Validation
- Spring Boot Actuator
- Log4j2
- Resilience4j (retry)
- JUnit 5, Mockito
- Docker Compose

## Project Structure

- [gateway-service/gateway-service](gateway-service/gateway-service)
- [account-service/account-service](account-service/account-service)
- [docker-compose.yml](docker-compose.yml)

## Setup Instructions

### Prerequisites

- JDK 17 installed and available on PATH
- Maven Wrapper supported shell (`mvnw.cmd` on Windows)
- Docker Desktop (optional, only for Docker Compose run)

### Install Dependencies

Dependencies are resolved automatically when running tests or startup commands.
If you want to pre-fetch dependencies:

1. cd gateway-service/gateway-service
2. mvnw.cmd dependency:go-offline
3. cd ../../account-service/account-service
4. mvnw.cmd dependency:go-offline

## Configuration

Both services use application.properties.

- Gateway: [gateway-service/gateway-service/src/main/resources/application.properties](gateway-service/gateway-service/src/main/resources/application.properties)
- Account: [account-service/account-service/src/main/resources/application.properties](account-service/account-service/src/main/resources/application.properties)

Default ports:

- Gateway: 8080
- Account: 8081

## API Endpoints

### Gateway Service

- POST /events
- GET /events/{eventId}
- GET /events?account={accountId}
- GET /health

### Account Service

- POST /accounts/{accountId}/transactions
- GET /accounts/{accountId}/balance
- GET /accounts/{accountId}
- GET /health

## Running Locally (Manual)

Open two terminals from the project root.

Terminal 1 (Account Service):

1. cd account-service/account-service
2. mvnw.cmd spring-boot:run

Terminal 2 (Gateway Service):

1. cd gateway-service/gateway-service
2. mvnw.cmd spring-boot:run

Health checks:

- http://localhost:8080/health
- http://localhost:8081/health

## Running with Docker Compose

From project root:

1. docker compose up --build

Health checks:

- http://localhost:8080/health
- http://localhost:8081/health

Stop:

1. docker compose down

## Running Tests

Gateway service tests:

1. cd gateway-service/gateway-service
2. mvnw.cmd test

Account service tests:

1. cd account-service/account-service
2. mvnw.cmd test

Real cross-service flow test:

1. cd gateway-service/gateway-service
2. mvnw.cmd -Dtest=GatewayAccountFlowIntegrationTest test

## Requirement Coverage Summary

### Core Functionality

- Idempotency by eventId is implemented in both services to prevent duplicate balance updates.
- Out-of-order arrivals are tolerated; event listing is sorted by eventTimestamp in Gateway.
- Balance formula implemented in Account Service: total credit minus total debit.
- Validation and HTTP error responses are implemented via bean validation and global exception handlers.

### Service Separation

- Gateway and Account are separate processes and separate Maven projects.
- Each service uses its own H2 database.

### Distributed Tracing

- Gateway generates/propagates trace id using X-Trace-Id.
- Gateway forwards trace id to Account Service.
- Both services log trace id in structured logs.

### Observability

- JSON structured logs in both services via Log4j2.
- /health implemented on both services with DB connectivity check.
- Custom metric in Gateway: gateway.events.created.

### Resiliency Pattern Choice

Implemented pattern: timeout + retry with backoff-like delay.

- Timeout: HTTP client timeout configured for account-service calls.
- Retry: Resilience4j retry on connection and service-unavailable exceptions.

Gateway resiliency config:

- resilience4j.retry.instances.accountService.max-attempts=3
- resilience4j.retry.instances.accountService.wait-duration=500ms

Behavior:

- If account-service call keeps failing, Gateway returns HTTP 503 via global exception handling.
- Gateway GET event endpoints continue to work from local gateway DB.

## Design Decisions

1. Separate databases per service to preserve microservice boundaries.
2. Idempotency enforced at both gateway and account layers using eventId uniqueness checks.
3. Trace propagation implemented with a lightweight custom trace header approach.
4. Structured logging standardized with traceId field for correlation.
5. Retry and timeout implemented in Gateway client to satisfy resiliency requirement without introducing optional complexity.
