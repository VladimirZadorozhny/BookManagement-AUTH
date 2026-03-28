# Booking Flow

## Overview

The booking system manages borrowing and returning books with strict business rules:

- Prevents duplicate active bookings
- Blocks users with overdue books
- Blocks users with unpaid fines
- Ensures atomic inventory updates
- Calculates fines on return

---  

## Borrow Book Flow

```mermaid  
sequenceDiagram  
 participant U as User  
 participant C as Controller  
 participant S as UserBookingService  
 participant DB as Database  
 participant INV as InventoryService  
  
 U->>C: Rent Book  
 C->>S: rentBook(userId, bookId)  
  
 S->>DB: Load user + bookings  
  
 alt Already borrowed  
 S-->>C: Error  
 end  
  
 alt Has overdue books  
 S-->>C: Error  
 end  
  
 alt Has unpaid fines  
 S-->>C: Error  
 end  
  
 S->>INV: decrementStock(bookId)  
  
 alt Not available  
 INV-->>S: Error  
 S-->>C: Error  
 end  
  
 S->>DB: Create booking  
 S->>DB: Save booking  
  
 S-->>C: Success
 ```

----------

## Return Book Flow

```mermaid
sequenceDiagram  
 participant U as User  
 participant C as Controller  
 participant S as BookingService  
 participant DB as Database  
 participant INV as InventoryService  
  
 U->>C: Return Book  
 C->>S: returnBook(userId, bookId)  
  
 S->>DB: Find active booking  
  
 alt Not found  
 S-->>C: Error  
 end  
  
 S->>S: Set returnedAt  
 S->>S: Calculate fine  
  
 S->>INV: incrementStock(bookId)  
  
 S-->>C: Success
 ```

----------

## Key Design Decisions

### 1. Business Rule Enforcement

All rules are enforced in the service layer:

- No duplicate bookings

- No borrowing with overdue books

- No borrowing with unpaid fines

----------

### 2. Atomic Inventory Updates

Inventory updates use direct database operations:

- Prevent race conditions

- Ensure consistency under concurrency

----------

### 3. Transactional Consistency

All operations are wrapped in `@Transactional`:

- Booking creation and stock update happen together

- Prevents partial updates

----------

### 4. Fine Calculation Strategy

- Fine is calculated on return

- For active bookings, fine is computed dynamically when needed

----------

## Summary

This design ensures:

- Data consistency

- Concurrency safety

- Clear separation of responsibilities