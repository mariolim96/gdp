---
paths:
paths:
  - '**/*.java'
---

# Java Coding Style

> This file extends [common/coding-style.md](../common/coding-style.md) with Java, Quarkus, and Spring Boot specific content.

## Formatting

- **google-java-format** or **Checkstyle** (Google or Sun style) for enforcement.
- Use **Spotless** for consistent styling across the project.
- One public top-level type per file.
- Consistent indent: 2 or 4 spaces (match project standard).
- Member order: constants, fields, constructors, public methods, protected, private.

## Immutability

- **Prefer `record`** for value types and DTOs (Java 16+).
- Mark fields `final` by default — use mutable state only when required.
- Return defensive copies from public APIs: `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()`.
- Copy-on-write: return new instances rather than mutating existing ones.

```java
// GOOD — immutable value type
public record OrderSummary(Long id, String customerName, BigDecimal total) {}

// GOOD — final fields, no setters
public class Order {
    private final Long id;
    private final List<LineItem> items;

    public List<LineItem> getItems() {
        return List.copyOf(items);
    }
}
```

## Dependency Injection (CDI & Spring)

- **Prefer constructor injection** over field injection (`@Inject` or `@Autowired`).
- **Quarkus (ArC)**: Use scope annotations correctly (`@ApplicationScoped`, `@RequestScoped`, `@Singleton`).
- **Spring**: Use `@Service`, `@Component`, `@Repository` with constructor injection.

## Naming

Follow standard Java conventions:
- `PascalCase` for classes, interfaces, records, enums.
- `camelCase` for methods, fields, parameters, local variables.
- `SCREAMING_SNAKE_CASE` for `static final` constants.
- Packages: all lowercase, reverse domain (`com.example.app.service`).

## Modern Java Features

Use modern language features where they improve clarity:
- **Records** for DTOs and value types (Java 16+).
- **Sealed classes** for closed type hierarchies (Java 17+).
- **Pattern matching** with `instanceof` — no explicit cast (Java 16+).
- **Text blocks** for multi-line strings — SQL, JSON templates (Java 15+).
- **Switch expressions** with arrow syntax (Java 14+).
- **Pattern matching in switch** — exhaustive sealed type handling (Java 21+).

## Optional Usage

- Return `Optional<T>` from finder methods that may have no result.
- Use `map()`, `flatMap()`, `orElseThrow()` — never call `get()` without `isPresent()`.
- Never use `Optional` as a field type or method parameter.

## Error Handling

- Prefer unchecked exceptions for domain errors.
- Create domain-specific exceptions extending `RuntimeException`.
- Avoid broad `catch (Exception e)` unless at top-level handlers.
- Include context in exception messages.

## Streams

- Use streams for transformations; keep pipelines short (3-4 operations max).
- Prefer method references when readable: `.map(Order::getTotal)`.
- Avoid side effects in stream operations.

## References

- See skill: `java-coding-standards` for full coding standards and Quarkus-specific ArC patterns.
- See skill: `jpa-patterns` for JPA/Hibernate entity design patterns.
- See skill: `springboot-patterns` for Spring Boot specific idioms.

>>>>>>> origin
