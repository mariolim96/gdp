---
paths:
  - '**/*.java'
---

# Java Security

> This file extends [common/security.md](../common/security.md) with Java, Quarkus, and Spring Boot specific content.

## Secret Management

- **Environment Presence**: Use environment variables for all secrets (`QUARKUS_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`).
- **Framework Integration**:
  - **Quarkus**: Use **Quarkus Config** or **SmallRye Config** profiles.
  - **Spring**: Use **Spring Cloud Config** or `@Value("${...}")` with default values.
- **NEVER** hardcode credentials in `application.properties`, `application.yml`, or source code.
- Objects like `Password` or `Token` should be cleared from memory as soon as possible.

## Authentication & Authorization

- **Established Frameworks**: Use **Quarkus Security** (OIDC, JWT) or **Spring Security**.
- **Declarative Security**: Use standard annotations to secure resources:
  - `@RolesAllowed`, `@Authenticated`, `@PermitAll` (Common/Quarkus).
  - `@PreAuthorize`, `@Secured` (Spring).
- **Password Hashing**: Always use **BCrypt** or **Argon2**; never MD5/SHA1.
- **Centralized Handling**: Map authentication/authorization errors to generic 401/403 responses.

## SQL Injection Prevention

- **Parameterized Queries**: Always use bind parameters (`:param` or `?`).
- **Framework Support**:
  - **JPA/Hibernate**: Use `Query.setParameter()`.
  - **Panache**: Use `list("name = ?1", name)`.
  - **JdbcTemplate**: Use `jdbcTemplate.query(sql, mapper, params)`.
- **Validation**: Sanitize any input used in native or dynamic queries.

## Input Validation

- **Bean Validation**: Use `@NotNull`, `@NotBlank`, `@Email`, `@Size` on DTOs/Records.
- **System Boundaries**: Validate all user input at the Entry Point (Controller/Resource).

```java
public record CreateUserRequest(
    @NotBlank String name,
    @Email String email,
    @Size(min = 8) String password
) {}
```

## Dependency Security

- **Scanning**: Use **Snyk**, **OWASP Dependency-Check**, or GitHub Dependabot.
- **Audit**: Run `mvn dependency:tree` or `./gradlew dependencies` regularly.

## Error Handling & Logging

- **Safe Responses**: Never leak stack traces, internal paths, or database errors.
- **PII Scrubbing**: Ensure logs do not contain passwords, tokens, or personal identifiers.
- **Generic Messages**: Return generic "Internal Server Error" or "Resource Not Found" to clients.

```java
// GOOD — Generic error with log context
try {
    return userService.findById(id);
} catch (UserNotFoundException ex) {
    log.warn("User not found: id={}", id);
    return ApiResponse.error("User not found");
} catch (Exception ex) {
    log.error("Failed to fetch user: id={}", id, ex);
    return ApiResponse.error("An unexpected error occurred");
}
```

## References

- See skill: `springboot-security` for Spring Security specific patterns.
- See skill: `quarkus-security` for Quarkus Security/OIDC/JWT best practices.
- See skill: `security-review` for general auditing checklists.
