# Authentication Flow

```
           Register
              ↓
Inactive user + verification token
              ↓
       Verification email
              ↓
            Verify
              ↓
   Atomic token consumption
              ↓
         Activate user
    
```

```
 User requests password reset
             ↓
        Generate token
             ↓
       Store token (DB)
             ↓
   Send email (event-driven)
             ↓
      User clicks link
             ↓
       Validate token
             ↓
    Consume token (atomic)
             ↓
       Update password
```

```mermaid  
sequenceDiagram  
participant U as User  
participant C as Controller  
participant S as UserAuthLifecycleService  
participant DB as Database  
participant E as EventPublisher  
participant M as MailListener  
  
U->>C: Register Request  
C->>S: register()  
S->>DB: Save user (inactive)  
S->>E: Publish Registration Event  
  
E->>M: Handle event (async)  
M->>M: Build email  
M->>M: Send email  
  
U->>C: Confirm token  
C->>S: confirm()  
S->>DB: Activate user
```