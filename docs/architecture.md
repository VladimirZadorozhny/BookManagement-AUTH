# System Architecture

## Overview

The application follows a modular monolithic architecture built with Spring Boot.

It combines:

- Layered architecture (Controller → Service → Repository)
- Event-driven components (mail system, reminders)
- Asynchronous processing
- Transactional boundaries for consistency

---  

## High-Level Architecture

```text
[ Browser (Thymeleaf + JS) ]
        |
        v
[ Controllers (REST + MVC) ]
        |
        v
[ Services (Business Logic) ]
     |         |           
     v         v           
[ JPA ]   [ Events ]   
     |          |
     v          v
[ MySQL ]   [ Async Listeners ]
                     |
                     v
              [ Mail Service ]
```

  
---  

## Key Architectural Decisions

### 1. Layered Structure

- Controllers handle HTTP requests
- Services contain business logic
- Repositories handle persistence

---  

### 2. Event-Driven Mail System

Instead of sending emails directly:

- Services publish events
- Listeners process them asynchronously

Benefits:

- Loose coupling
- Better performance
- Retry support

---  

### 3. Transaction Management

Critical operations use:

- `@Transactional`
- `REQUIRES_NEW` for isolation (e.g. reminders)

Ensures:

- Consistency
- Safe retries
- No partial updates

---  

### 4. Idempotency Strategy

Used in:

- Reminder system
- Mail retries

Techniques:

- Database constraints (unique keys)
- Pessimistic locking
- Log tables

Notes:

- The system implements **best-effort idempotency**
- Safe under retries due to transactional isolation
- In rare cases, duplicate emails may occur under concurrent execution or partial failures
- This is an intentional trade-off for simplicity in a monolithic architecture

Future improvements:

- Reservation-based processing (status-driven logs)
- Outbox pattern for guaranteed delivery

---  

### 5. Security Layers

- URL-based security (Spring Security config)
- Method-level security (`@PreAuthorize`)

---  

### 6. Hybrid Frontend

- Thymeleaf for structure
- JavaScript for dynamic data
- REST APIs for async updates

---  

## Technology Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- Docker
- Thymeleaf
- Vanilla JavaScript