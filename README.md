    Pooler-backend

    Feature
        1. Authentication
        2. Community
        3. Search NearBy Location
        4. Invitation - Accept/Decline/Pending
        5. Location - Near by and your home location
    
        security work Flow ---
        
        🚀 Quick Start
        Option 1 — Maven (Local)
            # Dev profile (default)
                ./mvnw spring-boot:run

            # Staging profile
                ./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

            # Prod profile
                JWT_SECRET=<64-char-hex> DB_URL=<url> ./mvnw spring-boot:run -P prod
        
        Option 2 — Docker Compose
            # Copy env file
                cp .env.example .env

            # Start dev stack (app + Mailhog mail catcher)
                docker compose --profile dev up -d

            # View logs
                docker compose logs -f auth-service

            # Stop
                docker compose down
    
        
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
      
