# DropOnAir Demo Backend

A minimal Spring Boot backend demonstrating the **3 required server-side endpoints** for
integrating DropOnAir SDK into your application.

> **This is a demo only.** It uses an in-memory key store (restarts lose keys) and a
> mock authentication system (no real credentials). Do NOT use this in production as-is.

---

## What This Demonstrates

| Endpoint | Your Production Equivalent |
|----------|---------------------------|
| `POST /api/auth/login` | Your existing user auth (OAuth2, session, etc.) |
| `POST /api/droponair/token` | Token exchange proxy, signs requests with your server secret |
| `PUT /api/droponair/keys/me` | Store user's X25519 public key in your database |
| `GET /api/droponair/keys/{userId}` | Serve peer's public key for E2EE key exchange |

DropOnAir only requires the last three endpoints. The auth endpoint exists only for demo purposes.

---

## Quick Start

### 1. Get Your Credentials

1. Sign up at [panel.droponair.com](https://panel.droponair.com)
2. Create an app
3. Copy **App ID**, **Public API Key**, and **Server Secret Key**

### 2. Configure

Set environment variables:
```bash
export DROPONAIR_APP_ID=your-app-id
export DROPONAIR_PUBLIC_API_KEY=your-public-api-key
export DROPONAIR_SERVER_SECRET=your-server-secret
```

Or edit `src/main/resources/application.yml` directly (not recommended for secrets).

### 3. Run

```bash
# Java 17+
./mvnw spring-boot:run

# Or build and run the JAR
./mvnw clean package -DskipTests
java -jar target/droponair-demo-backend-*.jar
```

Server starts on **http://localhost:8180**

---

## API Reference

### POST /api/auth/login

Mock login. Replace with your real authentication in production.

**Request:**
```json
{ "userId": "alice", "displayName": "Alice" }
```

**Response:**
```json
{ "jwt": "<demo-jwt>", "userId": "alice", "displayName": "Alice" }
```

---

### POST /api/droponair/token

Token exchange proxy. The SDK calls this with the user's JWT and expects a DropOnAir token back.

**Headers:** `Authorization: Bearer <user-jwt>`

**Response:**
```json
{ "accessToken": "<droponair-jwt>", "expiresIn": 3600 }
```

**How it works:**
1. Validates the user's JWT
2. Signs a request to DropOnAir using `HMAC-SHA256(serverSecret, appId + timestamp + nonce + SHA256(body))`
3. POSTs to `https://sdk.droponair.com/api/token/exchange`
4. Returns the DropOnAir JWT to the client

The server secret **never** leaves the backend. DropOnAir trusts the HMAC signature.

---

### PUT /api/droponair/keys/me

Publish the user's X25519 public key.

**Headers:** `Authorization: Bearer <user-jwt>`

**Request:**
```json
{ "publicKey": "<base64-encoded-x25519-public-key>", "deviceId": "device-abc" }
```

**Response:**
```json
{ "status": "ok", "userId": "alice" }
```

---

### GET /api/droponair/keys/{userId}

Fetch a user's public key for E2EE key exchange.

**Headers:** `Authorization: Bearer <user-jwt>`

**Response:**
```json
{
  "publicKey": "<base64>",
  "deviceKeys": [
    { "deviceId": "device-abc", "publicKey": "<base64>" }
  ]
}
```

---

## SDK Client Configuration

Point the SDK at this backend:

### JavaScript / TypeScript
```typescript
const client = await initialize({
  appId: 'YOUR_APP_ID',
  publicApiKey: 'YOUR_PUBLIC_API_KEY',
  getUserJwt: async () => {
    const res = await fetch('http://localhost:8180/api/auth/me', {
      headers: { Authorization: `Bearer ${storedJwt}` }
    });
    return storedJwt;  // Return the user JWT
  },
  tokenExchangeEndpoint: 'http://localhost:8180/api/droponair/token',
  keyDirectoryEndpoint: 'http://localhost:8180/api/droponair/keys',
});
```

### Android (Kotlin)
```kotlin
val client = DropOnAir.initialize(DropOnAirConfig(
    appId = "YOUR_APP_ID",
    publicApiKey = "YOUR_PUBLIC_API_KEY",
    getUserJwt = { storedJwt },
    tokenExchangeEndpoint = "http://10.0.2.2:8180/api/droponair/token",   // 10.0.2.2 = localhost on Android emulator
    keyDirectoryEndpoint = "http://10.0.2.2:8180/api/droponair/keys",
))
```

### iOS (Swift)
```swift
let client = try await DropOnAir.initialize(config: DropOnAirConfig(
    appId: "YOUR_APP_ID",
    publicApiKey: "YOUR_PUBLIC_API_KEY",
    getUserJwt: { storedJwt },
    tokenExchangeEndpoint: "http://localhost:8180/api/droponair/token",
    keyDirectoryEndpoint: "http://localhost:8180/api/droponair/keys"
))
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DROPONAIR_APP_ID` | `YOUR_APP_ID` | DropOnAir App ID from dashboard |
| `DROPONAIR_PUBLIC_API_KEY` | `YOUR_PUBLIC_API_KEY` | Public API key |
| `DROPONAIR_SERVER_SECRET` | `YOUR_SERVER_SECRET` | **Secret**, server-side only |
| `DROPONAIR_SDK_URL` | `https://sdk.droponair.com` | DropOnAir SDK base URL |
| `DEMO_JWT_SECRET` | `demo-super-secret-...` | Demo JWT signing secret |
| `PORT` | `8180` | Server port |

---

## Production Checklist

When adapting this to your production backend:

- [ ] Replace mock auth with your real user authentication
- [ ] Store public keys in a persistent database (not in-memory)
- [ ] Support multiple devices per user in the key directory
- [ ] Load `DROPONAIR_SERVER_SECRET` from a secrets manager (not env vars)
- [ ] Add rate limiting to the token exchange endpoint
- [ ] Add HTTPS/TLS termination
- [ ] Implement proper error handling and logging
