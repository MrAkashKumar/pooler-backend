# Pooler / HubHop backend

Spring Boot REST backend for HubHop’s identity, discovery, fair Common Point, invitation, short-lived chat, live meetup, arrival, and ride-history flows.

## Run locally

Requirements: Java 21+.

```bash
cp .env.example .env
./mvnw spring-boot:run
```

Default base URL: `http://localhost:8888/pooler-backend`

- Health: `/api/v1/public/health`
- Swagger: `/swagger-ui/index.html`
- OpenAPI: `/v3/api-docs`
- H2 console: `/h2-console`

The development profile uses an in-memory H2 database and seeded accounts. Do not enable the seeder or `ddl-auto=create-drop` in production.

## Architecture

Controllers return a consistent `ApiResponse<T>` envelope. Domain logic lives in service interfaces/implementations; repositories contain persistence queries; entities share audited timestamps through `BaseEntity`. Mobile authorization is stateless JWT plus an optional session token for sensitive actions.

Core modules:

- Auth, refresh/session tokens, Google ID-token exchange.
- Profile, contacts, saved locations, Telegram identity.
- Discovery pings, nearby rider search, distance/midpoint/route compatibility.
- Invitation and ride state machines.
- Two-hour invitation chat, messages, read receipts, reactions, search, cleanup.
- Participant-only live location and independent physical-arrival confirmation.

## Mobile headers

| Header | Purpose |
| --- | --- |
| `Authorization` | `Bearer <accessToken>` |
| `X-Session-Token` | Required by `@ValidSession` operations |
| `X-Device-Id` | Stable app-install identifier |
| `X-Platform` | `IOS`, `ANDROID`, or `WEB` |
| `X-App-Version` | Semantic app version |
| `X-Correlation-ID` | Optional request trace ID |

## Test

```bash
./mvnw test
```

API collections are under `doc/`, and the executable product wireframe is under `doc/productInfo/`. New endpoints are always available from generated OpenAPI even before a Postman collection refresh.
