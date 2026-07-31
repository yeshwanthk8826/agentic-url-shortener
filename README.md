## Current implementation

- Java 21
- Spring Boot 4.1
- PostgreSQL 18
- Flyway migrations
- Environment-based configuration
- Docker Compose local database
- Actuator health endpoints

## Local database

Set the required environment variables:

```powershell
$env:DATABASE_NAME = "agentic_shortener"
$env:DATABASE_USERNAME = "agentic_app"
$env:DATABASE_PASSWORD = Read-Host "Database password" -MaskInput
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/agentic_shortener"
$env:SPRING_PROFILES_ACTIVE = "local"