![Java](https://img.shields.io/badge/Java-17-blue)  
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen)  
![Build](https://img.shields.io/badge/build-passing-brightgreen)

# Book Management System – Event-Driven Backend

> A modular monolith built with Spring Boot demonstrating **event-driven architecture**, **resilient async processing**,
> and **secure transactional workflows**.
---

## Project Overview

This project manages a library system with:

- Secure authentication & token workflows
- Booking lifecycle management with business constraints
- Scheduler-driven reminders
- Event-driven mail system with retry & recovery
- Reporting & admin bulk notifications

**Goal:**   
Design a backend that is not just functional, but **reliable, consistent, and production-aware**.
  
---  
---

## Key Architectural Highlights

- **Event-driven mail processing** (decoupled from business logic)
- **Best-effort idempotent reminder system** with database guarantees
- **Retry & recovery strategy** (`@Retryable` + `@Recover`)
- **Two distinct mail processing strategies**:
- Per-booking (reminders)
- Per-user aggregated (admin notifications)
- **Transactional consistency with isolated operations (`REQUIRES_NEW`)**
- **Integration testing with real SMTP (GreenMail)**

----------

## Architecture Overview

```
Controller → Service → Repository → Database  
                ↓  
          Domain Events  
                ↓  
       Async Event Listeners  
                ↓  
           Mail System
```

### Design Principles

- Thin controllers, rich service layer
- Event-driven side effects (mail, notifications)
- Asynchronous processing (`@Async`)
- Retry & recovery for resilience
- Clear transactional boundaries

Detailed docs:

- [Architecture](docs/architecture.md)
- [Auth Flow](docs/authentication-flow.md)
- [Reminder System](docs/reminder-system.md)

---  

## Core System Flows

### Booking

- Enforces business rules:
- No duplicate active bookings
- No borrowing with overdue books
- No borrowing with unpaid fines
- Ensures atomic inventory updates
- Uses transactional boundaries for consistency

---  

### Reminder System

Flow:

```
   Scheduler  
       ↓  
BookingReminderService  
       ↓  
Publish BookingReminderEvent  
       ↓  
Async Listener (@Retryable)  
       ↓  
ReminderProcessingService
```

Key characteristics:

- Per-booking processing
- Isolated transactions (`REQUIRES_NEW`)
- Retry-enabled (`@Retryable`)
- Log-based idempotency protection

---  

## Idempotency & Reliability

The reminder system implements **best-effort idempotency**:

- `SELECT ... FOR UPDATE` prevents concurrent re-processing *when log exists*
- Unique constraint prevents duplicate log entries
- Safe to retry due to transactional isolation

### Known Trade-off!

In rare cases, **duplicate emails may occur**:

- Concurrent execution before log creation
- Failure after email send but before log persistence

This is an intentional design trade-off for a monolithic system.

### Future Improvement

- Reservation-based processing (`PENDING → SENT`)
- Outbox pattern for guaranteed delivery

---  

## Mail System

### Event-Driven Flow

```
Business Action  
     ↓  
Publish Event  
     ↓  
Async Listener (@Retryable)  
     ↓  
MailTemplateService  
     ↓  
MailService (SMTP)
```

### Features

- Registration & verification emails
- Password reset
- Booking reminders
- Admin bulk notifications

---  

### Mail Processing Strategies

Two different strategies are used depending on use case:

#### 1. Per-Booking (Reminders)

- One email per booking
- Focus: precision & timing
- Used by scheduler

#### 2. Per-User Aggregation (Admin Notifications)

- One email per user with aggregated data
- Includes:
- Overdue books
- Unpaid fines
- Heavy users
- Focus: readability & reduced email noise

This separation ensures both **accuracy** and **usability**.
  
---  

## Retry & Failure Handling

- `@Retryable` for transient failures
- `@Recover` for final fallback
- Failed emails stored in `FailedMailLog`

Acts as a lightweight **dead-letter system**
  
---  

## Security

- Spring Security (session-based)
- URL + method-level authorization
- Token-based flows:
- Email verification
- Password reset

### Security Highlights

- Atomic token consumption (race-condition safe)
- Enumeration attack mitigation
- CSRF protection (cookie + header)
- Custom authentication handlers

---  

## Testing Strategy

- Integration tests with **GreenMail (real SMTP simulation)**
- Full flow testing:
- DB → Event → Listener → Mail
- Covers:
- Retry behavior
- Reminder processing
- Aggregated notifications

---  

## Request Logging

RequestLoggingFilter logs method, URI, status and duration for all non-static requests.

Example log:

GET /api/users → 200 (63 ms)

### Future Improvement

- Add metrics using Micrometer and expose counters (like mail.send, mail.failed)

- Rebuild RequestLoggingFilter into @Component with FilterRegistrationBean to inject dependencies in filter (like
  MeterRegistry)

---  

## Running the Application

### Requirements

- Java 17+

- Maven

- MySQL

----------

### Docker (recommended)

1. Start MySQL using Docker:

```bash  
docker compose up -d
```  

2. Run the application:

```bash  
./mvnw spring-boot:run
```  

3. Open browser:

```  
http://localhost:8080  
```  

Flyway will automatically:

- create schema

- run migrations

- insert demo data

To stop:

```bash  
docker compose down
```

----------

### Run Tests

```bash  
./mvnw test
```

----------

## Profiles & Mail Configuration

By default, the application runs in **dev mode**:

```properties  
spring.profiles.active=dev
```

In dev mode:

- Mail sending can be disabled or use mock configuration

- No real credentials are required

----------

### Enable Real Email Sending

To enable real email sending:

1. Switch to production profile:

```properties
spring.profiles.active=prod
```

2. Provide environment variables:

- MAIL_USERNAME=your@gmail.com
- MAIL_PASSWORD=your_app_password

3. Gmail requires:

- App password

- 2FA enabled

----------

## Design Trade-offs

- **Send-before-log in reminders**
    - Ensures emails are not lost
    - Accepts rare duplicate risk under concurrency

- **Monolith instead of microservices**
    - Simpler development and deployment
    - Future-ready for event-driven extraction

- **Database-based dead-letter handling**
    - Simpler than message queues
    - Sufficient for current scale

---

## Future Improvements

- Reservation-based idempotency (prevent duplicate sends)

- Outbox pattern (distributed reliability)

- Metrics & tracing

- Rate limiting for sensitive endpoints

- Cleanup strategy for FailedMailLog

----------

## What This Project Demonstrates

- Event-driven backend design

- Trade-off-aware system design

- Reliable async processing with retry & recovery

- Concurrency-aware workflows

- Real-world integration testing