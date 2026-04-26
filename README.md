    Pooler-backend

    Feature
        1. Authentication
        2. Community
        3. Search NearBy Location
        4. Invitation - Accept/Decline/Pending
        5. Location - Near by and your home location
    
        security work Flow ---
        
       ```
---

## 🚀 Quick Start

### Option 1 — Maven (Local)

```bash
# Dev profile (default)
./mvnw spring-boot:run

# Staging profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

# Prod profile
JWT_SECRET=<64-char-hex> DB_URL=<url> ./mvnw spring-boot:run -P prod
```

### Option 2 — Docker Compose

```bash
# Copy env file
cp .env.example .env

# Start dev stack (app + Mailhog mail catcher)
docker compose --profile dev up -d

# View logs
docker compose logs -f auth-service

# Stop
docker compose down
```
        
        ## 🔑 Auth Flow (Mobile / Kotlin)
           1. POST /api/v1/auth/register      → { accessToken, refreshToken, sessionToken, user }
           2. POST /api/v1/auth/login         → { accessToken, refreshToken, sessionToken, user }
           3. GET  /api/v1/users/me           → Authorization: Bearer <accessToken>
           4. POST /api/v1/auth/refresh       → { refreshToken } → new accessToken
           5. POST /api/v1/auth/logout        → revoke current device
           6. POST /api/v1/auth/logout-all    → revoke all devices
           7. POST /api/v1/auth/forgot-password → sends reset email
           8. POST /api/v1/auth/reset-password  → { token, newPassword, confirmPassword }

        
        
        
        
        Mobile Request Headers
        |-----------------------------------------------------------------------------------|
        |    Header                      Value                               Required       |
        |-----------------------------------------------------------------------------------|
        | Authorization               Bearer <accessToken>                       ✅         |
        | ----------------------------------------------------------------------------------|
        | X-Device-Id                 Unique device identifier                Recommended   |
        |---------------------------------------------------------------------------------- |
        | X-Platform                  ANDROID or IOS                          Recommended   |
        | ----------------------------------------------------------------------------------|
        | X-App-Version               e.g. 2.1.0                              Recommended   |
        | ----------------------------------------------------------------------------------|
        | X-Session-Token             Session token (dual auth)               Optional      |
        | ----------------------------------------------------------------------------------|
        | X-Correlation-ID            Request trace ID                        Optional      |
        | ----------------------------------------------------------------------------------|

        
        ## 📋 API Endpoints
        
        | Method | Endpoint                          | Auth    | Description             |
        |--------|-----------------------------------|---------|-------------------------|
        | POST   | /api/v1/auth/register             | ❌       | Register new user       |
        | POST   | /api/v1/auth/login                | ❌       | Login                   |
        | POST   | /api/v1/auth/refresh              | ❌       | Refresh access token    |
        | POST   | /api/v1/auth/logout               | ✅       | Logout current device   |
        | POST   | /api/v1/auth/logout-all           | ✅       | Logout all devices      |
        | POST   | /api/v1/auth/forgot-password      | ❌       | Request password reset  |
        | POST   | /api/v1/auth/reset-password       | ❌       | Complete password reset |
        | GET    | /api/v1/users/me                  | ✅       | Get profile             |
        | PUT    | /api/v1/users/me                  | ✅       | Update profile          |
        | PUT    | /api/v1/users/me/change-password  | ✅       | Change password         |
        | DELETE | /api/v1/users/me                  | ✅       | Delete account          |
        | GET    | /api/v1/admin/users               | 🔒ADMIN | List all users          |
        | GET    | /api/v1/admin/users/{id}          | 🔒ADMIN | Get user by ID          |
        | PUT    | /api/v1/admin/users/{id}/suspend  | 🔒ADMIN | Suspend user            |
        | PUT    | /api/v1/admin/users/{id}/activate | 🔒ADMIN | Activate user           |


      
