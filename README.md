# Pooler / Hoppo backend

Spring Boot REST backend for Hoppo’s identity, discovery, fair Common Point, invitation, short-lived chat, live meetup, arrival, and ride-history flows.

## Run locally

Requirements: Java 21+.

```bash
./mvnw spring-boot:run
```

Default base URL: `http://localhost:8888/pooler-backend`

- Health: `/api/v1/public/health`
- Swagger: `/swagger-ui/index.html`
- OpenAPI: `/v3/api-docs`
- H2 console: `/h2-console`

The development profile uses an in-memory H2 database. Create local accounts through signup and email verification; do not commit seeded credentials or enable `ddl-auto=create-drop` in production.

## Architecture

Controllers return a consistent `ApiResponse<T>` envelope. Domain logic lives in service interfaces/implementations; repositories contain persistence queries; entities share audited timestamps through `BaseEntity`. Mobile authorization is stateless JWT plus an optional session token for sensitive actions.

Core modules:

- Auth, refresh/session tokens, Google ID-token exchange.
- Profile, rider gender, match preference, contacts, saved locations, Telegram identity.
- Discovery pings, nearby rider search with mutual `MALE`/`FEMALE`/`ANY` preference filtering, distance/midpoint/route compatibility.
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

## Rider preference contract

`GET/PUT /api/v1/users/me` exposes safety profile fields:

| Field | Values | Default | Purpose |
| --- | --- | --- | --- |
| `gender` | `UNKNOWN`, `MALE`, `FEMALE`, `OTHER` | `UNKNOWN` | The rider's own profile category for mutual filtering. First-time setup must use an explicit rider choice, not an auto-selected default. |
| `matchPreference` | `ANY`, `MALE`, `FEMALE` | `ANY` | Who the rider wants to see in discovery. |
| `emergencyContactName` | string, max 120 | `null` | Trusted family/contact label displayed in Safety centre. |
| `emergencyContactPhone` | string, max 32 | `null` | Phone number used by the mobile app for dialler/SMS handoff. |
| `emergencyMessage` | string, max 300 | `null` | Default message text used in the mobile SMS composer. |

`POST /api/v1/discovery/nearby` filters candidates before returning them. A candidate is shown only when the requester's preference allows the candidate's gender and the candidate's preference allows the requester's gender. This keeps the default broad while giving riders, especially women, a safety control for who appears in search.

Emergency contact fields are stored only as profile metadata. The backend does not call or text the contact; mobile clients compose the call/SMS locally and include device location at the moment the rider taps the safety action.

## Profile media contract

`POST /api/v1/users/me/media?purpose=PROFILE_PHOTO` and `POST /api/v1/users/me/media?purpose=PAYMENT_QR` accept `multipart/form-data` with a `file` part. Files must be JPEG, PNG, or WebP and no larger than `profile-media.max-size-mb` (default 5 MB in each profile properties file).

The backend uploads the object to S3 using AWS SDK default credentials and stores the resulting public URL on the user:

- `profilePictureUrl`: optional account/profile photo
- `paymentQrCodeUrl`: optional payment QR for manual sharing after a match

Configuration lives in the Spring profile properties files:

```properties
profile-media.s3-bucket=your-hoppo-profile-media-bucket
profile-media.s3-region=ap-southeast-1
profile-media.key-prefix=hoppo/profile-media/prod
profile-media.public-base-url=https://cdn.yourdomain.com
profile-media.max-size-mb=5
```

Development keeps `profile-media.s3-bucket` blank so accidental local uploads fail clearly. Staging and production can resolve the same keys from deployment-provided Spring placeholders. The S3 bucket or CloudFront distribution must allow read access for returned media URLs. Hoppo does not automatically expose payment QR during discovery; the mobile client shares it only when the owner taps **Share payment QR** in meetup chat.

## Fare split contract

Confirmed rides store two trip distances:

- `primaryTripDistanceKm`: longer rider, final drop-off, suggested booker
- `secondaryTripDistanceKm`: shorter rider, first drop-off

`POST /api/v1/rides/{rideId}/fare-split` accepts the real provider fare after the riders book in Grab, TADA, Gojek, or another cab app:

```json
{
  "totalFare": 30.0,
  "currency": "SGD",
  "provider": "Grab"
}
```

Validation requires `totalFare` from `0.01` to `99999.00`, a 3-letter currency code when supplied, and a non-blank provider name.

The backend stores the total fare, provider, currency, `primaryFareShare`, and `secondaryFareShare` on the ride. Shares are proportional to each rider's trip distance. Hoppo records the split for history only; it does not collect payment.

## Test

```bash
./mvnw test
```

API collections are under `doc/`, and the executable product wireframe is under `doc/productInfo/`. New endpoints are always available from generated OpenAPI even before a Postman collection refresh.

## Google OAuth note

The backend does not expose `/oauthredirect`. That route belongs to the Expo client and is registered in Google Cloud as the OAuth redirect URI. After Google redirects back to the client, the client sends the received ID token to `POST /api/v1/auth/google`, where this backend verifies the token audience against `app.google.client-ids`.
