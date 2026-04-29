# Account Management Service

The **Account Management Service** is one of the microservices in the Mobility app.
It is responsible for managing user profiles and organizations,
handling the full user registration lifecycle.
From the account creation to email confirmation and serving as the authoritative
source of user identity data for the rest of the platform.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture & Integrations](#architecture--integrations)
- [Key Features](#key-features)
- [Registration Flow](#registration-flow)
    - [Simple User Registration](#simple-user-registration)
    - [Organization User Registration](#organization-user-registration)
    - [Account Confirmation](#account-confirmation)
- [Saga Pattern & Compensating Transactions](#saga-pattern--compensating-transactions)
- [Caching](#caching)
- [Security & Token Propagation](#security--token-propagation)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Prerequisites](#prerequisites)
- [Running the Service](#running-the-service)

---

## Overview

The Account Management Service sits at the intersection of identity and business logic.
It orchestrates account creation by coordinating with the Auth Server over gRPC,
persists user profiles in PostgreSQL, and publishes emails via RabbitMQ.
It is designed with resilience in mind and every multi-step operation
is wrapped in a saga with full compensating transaction support.

---

## Tech Stack

| Layer             | Technology                                          |
|-------------------|-----------------------------------------------------|
| Language          | Java 21                                             |
| Framework         | Spring Boot 3.5.6                                   |
| Build Tool        | Maven                                               |
| Inbound API       | REST (Spring MVC)                                   |
| Database          | PostgreSQL (via Spring Data JPA)                    |
| Schema Migrations | Liquibase                                           |
| Cache             | Redis                                               |
| Messaging         | RabbitMQ                                            |
| Outbound RPC      | gRPC (does not expose a gRPC server - client only ) |
| Object Mapping    | MapStruct                                           |
| Auth Protocol     | OAuth 2.1 (Spring Authorization Server client)      |
| API Docs          | SpringDoc OpenAPI (Swagger UI)                      |
| AOP               | Spring AOP                                          |

---

## Architecture & Integrations

**Auth Server** - Called over gRPC to create and confirm user credentials.
All outgoing gRPC calls are intercepted by `GrpcClientAuthInterceptor`,
which automatically fetches a client-credentials OAuth 2.1 token and attaches it to the
request metadata.

**Email Service** - Decoupled via RabbitMQ.
Registration confirmation links and success notifications are published as
messages. The Email Service consumes and delivers them asynchronously.

**Racing Core Service** - Consumes user identity data from this service
(for example getting a user ID from an email address) to give a use the driver role.

---

## Key Features

- **User registration:** for standalone (simple) users and organization/company-affiliated users
- **Email-based account confirmation:** using signed JWT tokens
- **Saga orchestration:** with rollback support for distributed, multi-step registration workflows
- **gRPC client:** with automatic OAuth 2.1 bearer token injection
- **Redis caching:** for frequently read user identity lookups
- **MapStruct:** for clean, performant and type safe mapping
- **Centralized exception handling:** with structured error responses
- **AOP-based performance monitoring:** automatic slow-method detection across service and repository layers

---

## Registration Flow

### Simple User Registration

A *simple user* is an individual without an organizational affiliation.

1. **Idempotency check:** If a user with the same email and mobile number already exists,
   the service returns the existing record immediately without creating duplicates.
2. **Auth Server step (gRPC):**  User credentials (email, mobile number, hashed password)
   are sent to the Auth Server via gRPC.
   The returned `authUserId` is stored in the saga context.
3. **Profile persistence step (JPA):** A `UserProfile` entity is created
   and saved to PostgreSQL using the `authUserId` returned in the previous step.
4. **Email step (RabbitMQ):**  A signed JWT confirmation token is generated and embedded
   in a confirmation link, which is published to the email queue.

If any step fails, compensating transactions execute in reverse order (
see [Saga Pattern](#saga-pattern--compensating-transactions)).

### Organization User Registration

Follows the same saga structure as simple user registration with additional
steps to associate the user with a company/organization entity.

### Account Confirmation

1. The user clicks the confirmation link containing a signed JWT token.
2. The token is parsed and validated. The embedded email and user ID are extracted.
3. The corresponding `VerificationToken` is loaded and its state is validated.
4. The Auth Server is called over gRPC to mark the user as confirmed.
5. The token is marked as used.
6. A success notification email is published to the RabbitMQ email queue.

---

## Saga Pattern & Compensating Transactions

Long-running registration workflows are managed by a `SagaOrchestrator`.
Each step is registered with both a forward action and a compensating (rollback) action:

```
Step 1: CREATE_AUTH_USER_STEP
  --> Forward:     Create credentials on Auth Server via gRPC
  --> Compensate:  Call authServerCredentialsRollback via RabbitMQ

Step 2: SAVE_USER_PROFILE_STEP
  --> Forward:     Persist UserProfile to PostgreSQL
  --> Compensate:  Delete the saved UserProfile

Step 3: SEND_EMAIL_STEP
  --> Forward:     Publish confirmation email to RabbitMQ
  --> Compensate:  No compensation here
```

Auth Server rollback messages are published over RabbitMQ,
keeping the compensation path fully decoupled and asynchronous.

---

## Caching

Redis is used to cache user identity lookups that are called frequently.

```java

@Cacheable(cacheNames = CacheNames.USER_ID_FROM_EMAIL, key = "#username", unless = "#result == null")
public String getUserIdToCreateDriver(String username) {
    //stuff
}
```

Cache entries are keyed by the normalized email address.
Null results are explicitly excluded from caching via the `unless` condition.

---

## AOP Logging

A custom `LoggingAspect` monitors all service and repository layer methods using Spring AOP.
Methods that exceed a threshold (which is determined by a configuration)
are automatically flagged with a `[SLOW]` warning log:

```
WARN  ConfirmationService - ConfirmationService.confirmSimpleUserEmail() took 1345ms [SLOW]
```

This provides lightweight, zero-boilerplate performance observability
across the entire service layer without modifying business logic.

---

## Security & Token Propagation

All outbound gRPC calls to the Auth Server are authenticated using
OAuth 2.1 client credentials flow.

The `GrpcClientAuthInterceptor` is registered as a global gRPC client interceptor
and transparently:

1. Requests an access token from the Auth Server using the `mobility-api` client registration.
2. Attaches the token as a `Bearer` value in the gRPC `Authorization` metadata header.

This happens automatically for every outbound gRPC call.

---

## Error Handling

All exceptions are handled centrally by `AccountManagementExceptionHandler` (`@RestControllerAdvice`),
which returns consistent `MobilityAppErrorResponse` Json bodies with a human-readable message
and a timestamp.

`MobilityAppErrorResponse` example:

```
{
"timestamp": "2026-02-20T10:15:30Z",
"message": "Username not found"
}
```

### HTTP status codes

1. **400 Bad Request:** Request validation fails or request arguments are invalid
2. **401 Unauthorized:** Missing Authentication
3. **403 Forbidden:** Token is invalid/expired, or user lacks permissions
4. **404 Not Found:** Requested resource does not exist
5. **409 Conflict:** Invalid status, uniqueness, data integrity, token verification conflict
6. **500 Internal Server Error:** Any unexpected/unhandled server error

| Exception                                               | HTTP Status               | Response Message                                      |
|---------------------------------------------------------|---------------------------|-------------------------------------------------------|
| `MethodArgumentNotValidException`                       | 400 Bad Request           | Field-level validation errors are joined and returned |
| `IllegalArgumentException`                              | 400 Bad Request           | ex.getMessage()                                       |
| `AccountConfirmationException`                          | 400 Bad Request           | Account confirmation failed                           |
| `ExpiredJwtException`                                   | 401 Unauthorized          | Expired token                                         |
| `JwtException`                                          | 401 Unauthorized          | Invalid token                                         |
| `AccessDeniedException`, `AuthorizationDeniedException` | 403 Forbidden             | Invalid Permissions                                   |
| `NotFoundException`                                     | 404 Not Found             | ex.getMessage()                                       |
| `InvalidStatusException`                                | 409 Conflict              | Invalid Status                                        |
| `DataIntegrityViolationException`                       | 409 Conflict              | Data Integrity error                                  |
| `EmailTokenVerificationFailedException`                 | 409 Conflict              | ex.getMessage()                                       |
| `RuntimeException`, `Exception`                         | 500 Internal Server Error | Unexpected error occurred                             |

---

## API Endpoints Reference

Full interactive documentation is available via **Swagger UI** at
`http://localhost:8085/account-management/swagger-ui/index.html` when running locally.

---

## Configuration

The service is configured via `application.properties` and environment variables.
Environment-specific overrides are applied via Spring profiles (`local`, `staging`, `prod`),
activated either through the Maven profile flags or by setting `spring.profiles.active` directly.

**Maven Profiles**

| Profile ID | Usage                                                    |
|------------|----------------------------------------------------------|
| `local`    | Local development --> `application-local.properties`     |
| `staging`  | Staging environment --> `application-staging.properties` |
| `prod`     | Production environment --> `application-prod.properties` |

Activate a profile at build or run time:

```bash
mvn spring-boot:run -Plocal
#or
mvn clean package -DskipTests
java -jar target/account-management-1.0.0.jar --spring.profiles.active=staging
```

---

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **RabbitMQ**
- **Docker**
- **Redis**

---

## Logging

The application uses a custom `logback-spring.xml` configuration supporting
Spring profile specific log levels and output formatting.

```
src/main/resources/logback-spring.xml
```

---

## Shared Libraries

This service depends on three internal libraries:

| Library                 | Purpose                                                 |
|-------------------------|---------------------------------------------------------|
| `infrastructure-common` | Shared infrastructure utilities and base configurations |
| `proto-common`          | Protobuf / gRPC service definitions                     |
| `rabbitmq-common`       | RabbitMQ connection management and event abstractions   |

These must be available in the local Maven repository or a private artifact
registry before building.

---

## Running the Service

### Full Stack

All four microservices, databases, and infrastructure are managed from the
[mobility-app](https://github.com/mobility-systems/mobility-app) repository:

```bash
git clone https://github.com/mobility-systems/mobility-app.git
cd mobility-app
docker compose up -d
```

### Standalone development

> [!WARNING]
> **Make sure all required infrastructure services (PostgreSQL, Redis, RabbitMQ)
> are available before starting.**

```bash
#build
mvn clean package -Plocal -DskipTests
#run
java -jar target/account-management-1.0.0.jar
```

Or with Maven directly:

```bash
mvn spring-boot:run
```