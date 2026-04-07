---
description: Comprehensive Quarkus code review for Java 17+ standards, CDI (ArC) best practices, Panache, Mutiny, and security. Invokes the java-reviewer agent.
---

# Quarkus Code Review

This command invokes the **java-reviewer** agent for a comprehensive Quarkus-specific code review, focusing on cloud-native patterns and reactive efficiency.

## What This Command Does

1. **Identify Quarkus Changes**: Find modified `.java` files, `application.properties`, and `pom.xml` via `git diff`.
2. **Run Analysis**: Execute `mvn compile` or `./gradlew compileJava` to catch early issues.
3. **Quarkus Idioms Check**: Verify proper CDI (ArC) usage, Panache patterns, and Mutiny reactive flows.
4. **Security Scan**: Check for SQL injection, hardcoded secrets in `application.properties`, and missing security annotations.
5. **Modern Java Check**: Verify usage of Records, Sealed Classes, and Pattern Matching.
6. **Generate Report**: Summarize findings with a focus on Quarkus-native efficiency.

## When to Use

Use `/quarkus-review` (or `@quarkus-review`) when:

- After writing or modifying Quarkus resources or services.
- Before committing Quarkus changes.
- Reviewing pull requests with Quarkus code.
- Optimizing reactive flows with Mutiny.
- Ensuring GraalVM native image compatibility.

## Review Categories

### CRITICAL (Must Fix)

- Blocking I/O on reactive Mutiny routes (Uni/Multi).
- SQL Injection in JPQL/SQL (string concatenation).
- Hardcoded secrets in `application.properties` or code.
- Missing security annotations (`@RolesAllowed`, etc.) on public endpoints.
- Connection leaks (missing try-with-resources).

### HIGH (Should Fix)

- Field injection (`@Inject` on field) — use constructor injection.
- Improper scope usage (excessive use of `@Singleton` instead of `@ApplicationScoped`).
- Missing `@Valid` on resource/controller inputs.
- Missing `@Transactional` on database-altering methods.
- Legacy classes (e.g., `java.util.Date`) — use `java.time`.

### MEDIUM (Consider)

- Redundant Lombok usage when Records can suffice.
- Using `System.out.println` instead of proper logging.
- Magic numbers without named constants.
- Deeply nested code (> 3 levels).
- Methods > 40 lines.

## Example Usage

```text
User: /quarkus-review

Agent:
# Quarkus Code Review Report

## Files Reviewed
- src/main/java/com/example/UserResource.java (modified)
- src/main/java/com/example/UserService.java (modified)

## Issues Found

[CRITICAL] Blocking call on Reactive Route
File: src/main/java/com/example/UserResource.java:42
Issue: Thread.sleep() or blocking I/O used inside a Uni pipeline.
Fix: Use non-blocking delays or offload to a worker thread.

[HIGH] Field Injection detected
File: src/main/java/com/example/UserService.java:18
Issue: Using @Inject on fields makes unit testing harder.
Fix: Use constructor-based injection.
```

## Approval Criteria

| Status     | Condition                               |
| ---------- | --------------------------------------- |
| ✅ Approve | No CRITICAL or HIGH issues              |
| ⚠️ Warning | Only MEDIUM issues (merge with caution) |
| ❌ Block   | CRITICAL or HIGH issues found           |

## Related

- Agent: `agents/java-reviewer.md`
- Skills: `java-coding-standards`, `jpa-patterns`, `quarkus-patterns`, `quarkus-security`, `quarkus-tdd`.
