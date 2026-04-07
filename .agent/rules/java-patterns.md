---
paths:
paths:
  - '**/*.java'
---

# Java Patterns

> This file extends [common/patterns.md](../common/patterns.md) with Java, Quarkus, and Spring Boot specific content.

## Architectural Layers

- **Service Layer**: Business logic in service classes; keep controllers and repositories thin.
- **Repository Pattern**: Encapsulate data access behind an interface.
- **Constructor Injection**: Always use constructor injection — never field injection (`@Inject` or `@Autowired`).

## Data Access (Quarkus & JPA)

- **Quarkus Panache**:
    - Prefer **Panache Entity** (Active Record) for simple CRUD.
    - Use **Panache Repository** for complex domain logic or when separation is required.
- **Spring Data JPA**: Use `@Repository` interfaces with standard method naming conventions.

```java
// Quarkus Panache Active Record
@Entity
public class Person extends PanacheEntity {
    public String name;
    public LocalDate birth;
}
```

## Reactive Programming (Mutiny)

- Use **SmallRye Mutiny** for reactive APIs in Quarkus.
- Prefer `Uni` for single results and `Multi` for streams.
- Ensure non-blocking execution in reactive routes.

## Domain Modeling

- **Modern Records**: Use `record` for DTOs and value types (Java 16+).
- **Sealed Types**: Use for closed domain models (Java 17+).

```java
public sealed interface PaymentResult permits PaymentSuccess, PaymentFailure {
    record PaymentSuccess(String transactionId, BigDecimal amount) implements PaymentResult {}
    record PaymentFailure(String errorCode, String message) implements PaymentResult {}
}

// Exhaustive switch (Java 21+)
String message = switch (result) {
    case PaymentSuccess s -> "Paid: " + s.transactionId();
    case PaymentFailure f -> "Failed: " + f.errorCode();
};
```

## Object Creation

- **Builder Pattern**: Use for objects with many optional parameters (manual or with `@Builder` only when necessary).
- **Static Factory Methods**: Use for descriptive object creation (`of()`, `from()`).

## API Consistency

- **Response Envelope**: Use a consistent API response format.

```java
public record ApiResponse<T>(boolean success, T data, String error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

## Observability (Health & Metrics)

### Quarkus (MicroProfile)
Use MicroProfile Health for readiness/liveness and Metrics for performance monitoring.

```java
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("Database connection");
    }
}

@Timed(name = "processTime", description = "Time to process request")
public Uni<Response> process() { ... }
```

### Spring Boot (Actuator)
Enable Actuator endpoints and use Micrometer for custom metrics.

```java
@Bean
public HealthIndicator customHealthIndicator() {
    return () -> Health.up().withDetail("External Service", "Connected").build();
}
```

## OpenAPI & Documentation

Always provide standardized API documentation.

- **Quarkus**: Use `quarkus-smallrye-openapi` extension. Annotate with `@Operation` and `@APIResponse`.
- **Spring Boot**: Use `springdoc-openapi-starter-webmvc-ui`. Annotate with `@Tag` and `@ApiResponse`.

## References

- See skill: `springboot-patterns` for Spring Boot architecture patterns.
- See skill: `quarkus-patterns` for Quarkus-specific best practices (Panache, CDI, Mutiny).
- See skill: `jpa-patterns` for entity design and query optimization.
- See examples: `examples/quarkus-api-CLAUDE.md`, `examples/springboot-api-CLAUDE.md`.
