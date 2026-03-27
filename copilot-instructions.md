# GitHub Copilot Instructions

## Project Context
This is a **Java Spring Boot** microservices project. Apply enterprise-grade, production-ready patterns in all suggestions.

---

## Tech Stack
- **Backend:** Java 17/21, Spring Boot 3.x, Spring Security, Spring Data JPA
- **Messaging:** Apache Kafka (event-driven architecture)
- **Database:** PostgreSQL, Redis (caching)
- **Frontend:** Angular (TypeScript)
- **Build:** Maven / Gradle
- **Containerization:** Docker, Kubernetes
- **CI/CD:** Jenkins, GitHub Actions

---

## Code Style & Standards

### Java
- Use **Java 17+ features**: records, sealed classes, pattern matching, text blocks
- Prefer **immutable objects** and `final` fields where possible
- Always use **`var`** for local type inference when type is obvious
- Use **`Optional<T>`** instead of returning `null`
- Prefer **streams** over imperative loops for collection processing
- Follow **SOLID** principles strictly
- Class names: `PascalCase` | Methods/fields: `camelCase` | Constants: `UPPER_SNAKE_CASE`

### Spring Boot
- Annotate services with `@Service`, repositories with `@Repository`
- Use **constructor injection** (never field injection with `@Autowired`)
- Define configurations in `@Configuration` classes, not in services
- Use **`@Value`** or `@ConfigurationProperties` for all externalized config
- Always add `@Transactional` on service methods that modify data
- Use **DTOs** for API request/response — never expose JPA entities directly
- Prefer `ResponseEntity<T>` for REST controller return types

### REST API Design
- Follow RESTful conventions: `GET /api/v1/stocks`, `POST /api/v1/orders`
- Return proper HTTP status codes: `200`, `201`, `204`, `400`, `401`, `404`, `500`
- Always validate inputs using `@Valid` + Bean Validation annotations
- Use `@ControllerAdvice` + `@ExceptionHandler` for global error handling
- Include pagination (`Pageable`) for all list endpoints

---

## Microservices Patterns
- Each service must be **independently deployable** with its own DB schema
- Use **Kafka** for async inter-service communication (avoid synchronous REST calls for events)
- Implement **Circuit Breaker** pattern (Resilience4j) for downstream calls
- Use **Saga pattern** for distributed transactions
- Every service must expose `/actuator/health` and `/actuator/info`
- Apply **API Gateway** pattern — never expose microservices directly

---

## Kafka Guidelines
- Producer: Always use `KafkaTemplate<String, Object>` with schema validation
- Consumer: Use `@KafkaListener` with explicit `groupId` and `topics`
- Always handle `DeadLetterPublishingRecoverer` for failed messages
- Use **Avro** or **JSON Schema** for message serialization
- Topic naming convention: `service-name.entity.event` (e.g., `stock.price.updated`)

---

## Database & JPA
- **Avoid N+1 queries** — use `JOIN FETCH` or `@EntityGraph`
- Prefer **named queries** or **JPQL** over native SQL unless performance demands it
- Always add database **indexes** on foreign keys and frequently filtered columns
- Use **Flyway** or **Liquibase** for DB migrations — never `ddl-auto: create`
- Apply `@Version` for optimistic locking on entities with concurrent updates

---

## Security
- Use **JWT** (stateless) authentication with Spring Security
- Store secrets in **environment variables** or vault — never hardcode
- Apply **`@PreAuthorize`** for method-level security
- Always **sanitize and validate** user inputs to prevent SQL injection / XSS
- Use HTTPS only; configure CORS explicitly per environment

---

## Testing
- Unit tests: **JUnit 5** + **Mockito** — mock all dependencies
- Integration tests: **`@SpringBootTest`** + **Testcontainers** for DB/Kafka
- Name tests: `methodName_scenario_expectedBehavior()`
- Aim for **80%+ code coverage** on service and repository layers
- Use **`@DataJpaTest`** for repository-layer tests

---

## Error Handling
- Never swallow exceptions with empty `catch` blocks
- Always log exceptions with **SLF4J**: `log.error("message", exception)`
- Use custom exception classes: `ResourceNotFoundException`, `ValidationException`
- Return structured error responses:
```json
{
  "timestamp": "2026-03-24T10:54:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Stock with id 42 not found",
  "path": "/api/v1/stocks/42"
}
```

---

## Logging
- Use **SLF4J + Logback** — never `System.out.println`
- Log levels: `DEBUG` for dev, `INFO` for business events, `ERROR` for exceptions
- Always include **correlation/trace ID** in logs for distributed tracing
- Structured logging format (JSON) for production environments

---

## Performance
- Use **Redis** for caching frequently read data (`@Cacheable`, `@CacheEvict`)
- Optimize DB queries — use `EXPLAIN ANALYZE` for slow queries
- Use **async processing** (`@Async`) for non-blocking operations
- Apply **rate limiting** on public APIs
- Use **connection pooling** (HikariCP) with tuned pool size

---

## Documentation
- Add **Javadoc** on all public methods in service and controller layers
- Use **Swagger/OpenAPI 3** (`springdoc-openapi`) for API documentation
- Keep `README.md` updated with setup steps, env variables, and architecture diagram

---

## Git & PR Guidelines
- Branch naming: `feature/`, `fix/`, `hotfix/`, `chore/`
- Commit messages: `feat: add Kafka consumer for stock price events`
- One feature/fix per PR — keep PRs small and reviewable
- Always include unit tests in the same PR as the feature

---

## What to Avoid
- No `System.out.println` — use proper logging
- No business logic in controllers — delegate to services
- No direct DB calls from controllers
- No `@Autowired` field injection
- No hardcoded credentials, ports, or URLs
- No raw `HashMap` as API response — use typed DTOs
