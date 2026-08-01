# Agentic URL Shortener

A production-oriented URL-shortening service and governed software-delivery
workflow prototype built with Java, Spring Boot, and PostgreSQL.

The project demonstrates:

- Reliable URL creation and redirection
- Durable request idempotency
- Redirect analytics and cache-aside lookup
- Observable production behavior
- Stateful SDLC workflow orchestration
- Human release approval
- Safe-stop controls
- Append-only audit history
- Container packaging
- GitHub Actions CI

For design details, see [Architecture](docs/architecture.md).

## Features

### URL shortener

- Secure eight-character Base62 short codes
- HTTP and HTTPS URL validation
- Optional expiration
- Redirect metadata
- Redirect analytics
- Atomic visit counters
- Caffeine cache-aside redirect lookup
- RFC 9457 Problem Details errors

### Idempotency

- Required `Idempotency-Key` header
- PostgreSQL-backed durable request state
- SHA-256 request fingerprints
- Atomic request reservation
- Completed-response replay
- Payload-conflict detection
- Failed-request recovery
- Protection across application restarts and instances

### Agentic SDLC orchestration

- Persisted workflows and nodes
- Explicit dependency graph
- Sequential and parallel stages
- Dependency synchronization
- Dynamic downstream re-planning
- Release-readiness approval gate
- Human safe-stop control
- Append-only audit events
- Optimistic locking

### Operations

- PostgreSQL and Flyway migrations
- Health, liveness, and readiness endpoints
- Prometheus metrics
- Correlation IDs
- Multi-stage non-root Docker image
- Docker Compose
- GitHub Actions test and container-build workflow

## Technology

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Caffeine
- Micrometer and Prometheus
- Maven
- JUnit 5 and Mockito
- Docker and Docker Compose
- GitHub Actions

## Project structure

```text
.
|-- .github/workflows/
|   `-- ci.yml
|-- docs/
|   `-- architecture.md
|-- src/main/java/com/yeshwanthk/agentic_url_shortener/
|   |-- config/
|   |-- exception/
|   |-- idempotency/
|   |   |-- domain/
|   |   |-- dto/
|   |   |-- exception/
|   |   |-- repository/
|   |   `-- service/
|   |-- observability/
|   |-- orchestration/
|   |   |-- controller/
|   |   |-- domain/
|   |   |-- dto/
|   |   |-- exception/
|   |   |-- repository/
|   |   `-- service/
|   `-- url/
|       |-- cache/
|       |-- controller/
|       |-- domain/
|       |-- dto/
|       |-- exception/
|       |-- repository/
|       `-- service/
|-- src/main/resources/db/migration/
|-- src/test/
|-- compose.yaml
|-- Dockerfile
`-- pom.xml
```

## API overview

### URL APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/urls` | Idempotently create a short URL |
| `GET` | `/api/v1/urls/{shortCode}` | Retrieve URL metadata |
| `GET` | `/api/v1/urls/{shortCode}/analytics` | Retrieve redirect analytics |
| `GET` | `/{shortCode}` | Redirect to the original URL |

### Workflow APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/workflows` | Create an SDLC workflow |
| `GET` | `/api/v1/workflows/{id}` | Retrieve workflow state |
| `POST` | `/api/v1/workflows/{id}/advance` | Re-evaluate dependency gates |
| `POST` | `/api/v1/workflows/{id}/nodes/{key}/complete` | Complete a workflow node |
| `POST` | `/api/v1/workflows/{id}/replan` | Invalidate and re-plan downstream work |

### Governance APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/workflows/{id}/governance/approvals/release-readiness` | Approve release readiness |
| `POST` | `/api/v1/workflows/{id}/governance/safe-stop` | Safe-stop a workflow |
| `GET` | `/api/v1/workflows/{id}/governance/audit-events` | Retrieve audit history |

## Prerequisites

Install:

- Java 21
- Docker Desktop
- Git

A separate Maven installation is unnecessary because the repository includes
the Maven wrapper.

## Run locally

### 1. Start PostgreSQL

```powershell
docker compose up -d postgres
docker compose ps
```

### 2. Configure the application

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/agentic_shortener"
$env:SPRING_DATASOURCE_USERNAME = "agentic_app"
$env:SPRING_DATASOURCE_PASSWORD = "agentic_local_password"
$env:PUBLIC_BASE_URL = "http://localhost:8082"
```

Do not commit production secrets.

### 3. Start the application

```powershell
.\mvnw.cmd spring-boot:run
```

Local URL:

```text
http://localhost:8082
```

## Run with Docker

Build the image:

```powershell
docker build -t agentic-url-shortener:local .
```

Start the full stack:

```powershell
docker compose --profile app up -d --build
docker compose --profile app ps
```

Container URL:

```text
http://localhost:8080
```

Check health:

```powershell
curl.exe -i http://localhost:8080/actuator/health
```

Stop the stack without deleting PostgreSQL data:

```powershell
docker compose --profile app down
```

## URL API examples

### Create a short URL

```powershell
$key = "create-url-" + [guid]::NewGuid()

curl.exe -i -X POST http://localhost:8082/api/v1/urls `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: $key" `
  -d '{\"url\":\"https://example.com/products/123\",\"expiresAt\":null}'
```

Expected first response:

```http
HTTP/1.1 201 Created
Idempotency-Replayed: false
Location: http://localhost:8082/Ab12Cd34
```

### Replay the request

Repeat the exact request with the same key:

```powershell
curl.exe -i -X POST http://localhost:8082/api/v1/urls `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: $key" `
  -d '{\"url\":\"https://example.com/products/123\",\"expiresAt\":null}'
```

Expected:

```http
HTTP/1.1 200 OK
Idempotency-Replayed: true
```

The stored resource ID and short code remain unchanged.

### Test conflict protection

Reuse the same key with a different URL:

```powershell
curl.exe -i -X POST http://localhost:8082/api/v1/urls `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: $key" `
  -d '{\"url\":\"https://different.example.com\",\"expiresAt\":null}'
```

Expected:

```http
HTTP/1.1 409 Conflict
```

### Redirect

```powershell
curl.exe -i http://localhost:8082/Ab12Cd34
```

Expected:

```http
HTTP/1.1 302 Found
Location: https://example.com/products/123
```

### Analytics

```powershell
curl.exe -i `
  http://localhost:8082/api/v1/urls/Ab12Cd34/analytics
```

Example:

```json
{
  "shortCode": "Ab12Cd34",
  "status": "ACTIVE",
  "visitCount": 3,
  "createdAt": "2026-07-31T12:00:00Z",
  "lastAccessedAt": "2026-07-31T12:05:00Z",
  "expiresAt": null
}
```

## Workflow example

Create a workflow:

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/v1/workflows" `
    -ContentType "application/json" `
    -Body '{
      "name": "URL analytics enhancement",
      "requirement": "Add production redirect analytics",
      "scenarioType": "BROWNFIELD"
    }'

$workflowId = $workflow.id

$workflow.nodes |
    Select-Object nodeKey, stage, status
```

The initial graph contains:

```text
requirements       READY
architecture       BLOCKED
implementation     BLOCKED
testing            BLOCKED
documentation      BLOCKED
release-readiness  BLOCKED
```

After implementation completes, testing and documentation become ready in
parallel. Release readiness remains blocked until both finish and a human
approval exists.

Approve release readiness:

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/v1/workflows/$workflowId/governance/approvals/release-readiness" `
    -ContentType "application/json" `
    -Body '{
      "actor": "engineering-reviewer",
      "reason": "Testing and documentation gates passed"
    }'
```

Retrieve the audit trail:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8082/api/v1/workflows/$workflowId/governance/audit-events"
```

Safe-stop execution:

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/v1/workflows/$workflowId/governance/safe-stop" `
    -ContentType "application/json" `
    -Body '{
      "actor": "engineering-reviewer",
      "reason": "Manual risk-control verification"
    }'
```

Subsequent execution attempts return `409 Conflict`.

## Idempotency contract

| Situation | Result |
|---|---|
| New key and request | Create resource; return `201` |
| Same key and payload | Replay response; return `200` |
| Same key and different payload | Return `409` |
| Same key while processing | Return retryable `409` |
| Failed or expired incomplete record | Allow controlled retry |

Idempotency records are persisted in PostgreSQL. Request fingerprints prevent
the same key from representing different operations.

## Observability

### Health

```text
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

### Prometheus

```powershell
curl.exe http://localhost:8082/actuator/prometheus
```

Application metrics include URL creation, redirect success/failure, and redirect
duration.

### Correlation IDs

Clients may send:

```http
X-Correlation-ID: request-123
```

Valid values are propagated to the response and logging context. Invalid values
are replaced with generated UUIDs.

## Testing

Run all tests:

```powershell
.\mvnw.cmd clean test
```

Run the complete verification lifecycle:

```powershell
.\mvnw.cmd clean verify
```

The current tests cover core URL behavior, validation, redirect caching,
analytics, observability, controller contracts, and application-context schema
validation.

## CI

The GitHub Actions workflow runs on pushes and pull requests to `main`.

It:

1. Starts PostgreSQL.
2. Configures Java 21.
3. Runs `clean verify`.
4. Uploads test reports and the application JAR.
5. Builds the production Docker image.

Workflow:

```text
.github/workflows/ci.yml
```

## Error format

Errors use RFC 9457 Problem Details:

```json
{
  "type": "urn:problem:idempotency-conflict",
  "title": "Idempotency key conflict",
  "status": 409,
  "detail": "The idempotency key was already used with a different request",
  "instance": "/api/v1/urls"
}
```

Stack traces and internal implementation details are not returned to clients.

## Security considerations

- Only HTTP and HTTPS URLs are accepted.
- URLs containing embedded credentials are rejected.
- Request sizes and identifier lengths are validated.
- Database credentials are environment-based.
- Runtime containers use a non-root user.
- Public endpoints should be protected by rate limiting and client quotas before
  internet exposure.
- Management endpoint exposure should be restricted at the network boundary.

## Current limitations

- Authentication and client authorization are not implemented.
- Rate limiting and quotas are not implemented.
- Expired idempotency-record cleanup is not automated.
- Processing lease and completed-response retention use the same expiration
  model.
- Automated workflow retries and compensating rollback execution are not
  implemented.
- Audit events currently cover key governance transitions, not every internal
  state mutation.
- Custom short aliases are not implemented.
- The prototype uses deterministic workflow execution rather than external AI
  model calls.

## Further documentation

- [Architecture and engineering decisions](docs/architecture.md)