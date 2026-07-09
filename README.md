    Pooler-backend

    A brief description of what this Spring Boot service does.

    ### Prerequisites
    * **Java 21+** (or your specific version)
    * **Maven 3.6+** or **Gradle**
    * **Docker** (if using databases like H2/PostgreSQL/MySQL)
    * **IDE** (IntelliJ IDEA, VS Code, or Eclipse)

    ### Installation & Setup

    1. **Clone the repository:**
   
    git clone https://github.com/MrAkashKumar/pooler-backend.git
    cd pooler-backend

    2. **Configure Environment:**

    Update src/main/resources/application.properties (or .yml) with your local database credentials.
---

## 🚀 Quick Start

### Option 1 — Maven (Local)

```bash
# Dev profile (default)
./mvnw spring-boot:run

# Staging profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev //staging or prod 

# Prod profile
JWT_SECRET=<64-char-hex> DB_URL=<url> ./mvnw spring-boot:run -P prod
```

### Option 2 — Docker Compose

```bash
# Copy env file
cp .env.pooler .env

# Start dev stack (app + Mailhog mail catcher)
docker compose --profile dev up -d

# View logs
docker compose logs -f auth-service

# Stop
docker compose down
```

## 📖 API Documentation (Swagger & Postman)

This is the section you specifically asked for. It’s best to provide both for flexibility.

### 3. Swagger UI (OpenAPI)
Since Spring Boot usually uses **SpringDoc OpenAPI**, the documentation is generated automatically.

```markdown

### Interactive API Docs (Swagger)
Once the application is running, you can access the interactive Swagger UI to test the endpoints directly from your browser:

* **Swagger UI:** [http://localhost:8080/pooler-backend/swagger-ui/index.html](http://localhost:8080/pooler-backend/swagger-ui/index.html)

* **API Spec (JSON):** [http://localhost:8080/pooler-backend/v3/api-docs](http://localhost:8080/pooler-backend/v3/api-docs)
        


### Postman Collection
We have provided a pre-configured Postman collection for easy testing.

1. Locate the file in `/doc/Pooler-API.postman_collection.json`.
                      `/doc/Pooler-Local.postman_environment.json`.
2. Open **Postman**.
3. Click **Import** and drag the JSON file into the window.
4. (Optional) Set up a Postman **Environment** with a variable `base_url = http://localhost:8080`.



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

        -------------------------------- H2 Console -----------------------------------------
        http://localhost:<PORT>/api/v1/h2-console/ - connect database
