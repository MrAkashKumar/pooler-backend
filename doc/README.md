# Pooler Auth API — Postman Collection

This folder contains a ready-to-import Postman collection and environment for the Pooler backend.

## Files

- `Pooler-Auth-API.postman_collection.json` — full collection with all endpoints, headers, request bodies, query/path params, and example responses (success + common error cases).
- `Pooler-Auth-API.postman_environment.json` — environment with `baseUrl` and token variables.
- `Pooler-API.postman_environment.json` — full collection with all endpoints, headers, request bodies, query/path params, and example responses (success + common error cases).
- `Pooler-Local.postman_environment.json` — environment with `baseUrl` and token variables.

## Import

1. Open Postman → **Import** → drop both JSON files.
2. Select the **Pooler Local Dev** environment in the top-right dropdown.
3. Run **Authentication → Register** or **Login**. Tokens (`accessToken`, `refreshToken`, `sessionToken`, `userId`) are auto-saved by the collection's test scripts and reused by all protected requests.

## Base URL

The collection uses `{{baseUrl}}` (default `http://localhost:8888/pooler-backend`).

| Profile | URL |
| --- | --- |
| `dev` (default) | `http://localhost:8888/pooler-backend` |
| no-profile | `http://localhost:8888` |
| staging | `https://staging.pooler.com` |
| prod | `https://api.pooler.com` |

## Swagger UI

After the fix in `application.properties` and `SecurityConfig.java`:

- Swagger UI: `http://localhost:8888/pooler-backend/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8888/pooler-backend/v3/api-docs`

> The `server.servlet.contextPath=/api/v1` was removed because every controller already declares `@RequestMapping("/api/v1/...")`; the duplicated prefix was breaking Swagger and the public-route matchers.

## Endpoints (summary)

### Public — no auth
| Method | Path |
| --- | --- |
| GET | `/api/v1/public/health` |
| GET | `/api/v1/public/version` |

### Authentication
| Method | Path | Auth |
| --- | --- | --- |
| POST | `/api/v1/auth/register`         | none |
| POST | `/api/v1/auth/login`            | none |
| POST | `/api/v1/auth/refresh`          | none (uses refresh token in body) |
| POST | `/api/v1/auth/forgot-password`  | none |
| POST | `/api/v1/auth/reset-password`   | none |
| POST | `/api/v1/auth/logout`           | Bearer |
| POST | `/api/v1/auth/logout-all`       | Bearer |

### User Profile (Bearer)
| Method | Path |
| --- | --- |
| GET    | `/api/v1/users/me` |
| PUT    | `/api/v1/users/me` |
| PUT    | `/api/v1/users/me/change-password` |
| DELETE | `/api/v1/users/me` |

### Sessions (Bearer)
| Method | Path |
| --- | --- |
| GET    | `/api/v1/sessions` |
| DELETE | `/api/v1/sessions/{sessionId}` |
| GET    | `/api/v1/sessions/token-info` |

### Audit Logs (Bearer)
| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/v1/audit/me?page=0&size=20`              | Current user |
| GET | `/api/v1/audit/users/{entityId}?page=0&size=20` | Admin only |

### Admin (Bearer + ROLE_ADMIN)
| Method | Path |
| --- | --- |
| GET | `/api/v1/admin/users?page=0&size=20&sort=createdAt` |
| GET | `/api/v1/admin/users/{id}` |
| PUT | `/api/v1/admin/users/{id}/suspend` |
| PUT | `/api/v1/admin/users/{id}/activate` |

## Mobile-specific headers

These are picked up by `DeviceInfoArgumentResolver` / `RequestMetadataInterceptor`:

| Header | Description |
| --- | --- |
| `X-Device-Id`     | Unique device identifier (UUID / Android ID) |
| `X-Platform`      | `ANDROID` / `IOS` / `WEB` |
| `X-App-Version`   | semver (e.g. `1.0.0`) |
| `X-FCM-Token`     | Firebase push token |
| `X-Session-Token` | Current session token (required by `@ValidSession` endpoints) |
| `X-Correlation-ID`| Optional client-supplied correlation id |
