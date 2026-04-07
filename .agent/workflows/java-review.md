---
description: Comprehensive Java and Quarkus code review for Java 17+ standards, CDI (ArC) best practices, Panache, Mutiny, and security. Invokes the java-reviewer agent.
---

# Java & Quarkus Code Review

This command invokes the **java-reviewer** agent for comprehensive Java-specific code review, with a strong focus on the Quarkus ecosystem.

## What This Command Does

1. **Identify Java Changes**: Find modified `.java` files via `git diff`
2. **Run Analysis**: Execute Maven/Gradle tasks if available (Checkstyle, PMD, SpotBugs)
3. **Quarkus Idioms Check**: Verify proper CDI (ArC) usage, Panache patterns, and Mutiny reactive flows
4. **Security Scan**: Check for SQL injection, hardcoded secrets in `application.properties`, and missing security annotations
5. **Modern Java Check**: Verify usage of Records, Sealed Classes, and Pattern Matching
6. **Generate Report**: Categorize issues by severity

## When to Use

Use `/java-review` when:

- After writing or modifying Java/Quarkus code
- Before committing Java changes
- Reviewing pull requests with Java code
- Optimizing reactive flows with Mutiny
- Ensuring GraalVM native image compatibility

## Review Categories

### CRITICAL (Must Fix)

- SQL Injection in JPQL/SQL (string concatenation)
- Hardcoded secrets in `application.properties` or code
- Blocking I/O on reactive Mutiny routes (Uni/Multi)
- Missing security annotations (`@RolesAllowed`, etc.) on public endpoints
- Connection leaks (missing try-with-resources)

### HIGH (Should Fix)

- Field injection (`@Inject` on field) — use constructor injection
- Legay classes (e.g., `java.util.Date`, `Vector`) — use `java.time` or modern alternatives
- Missing `@Valid` on resource/controller inputs
- Missing `@Transactional` on database-altering methods
- Excessive use of `@Singleton` when `@ApplicationScoped` is preferred

### MEDIUM (Consider)

- Redundant Lombok usage when Records can suffice
- Using `System.out.println` instead of proper logging
- Magic numbers without named constants
- Deeply nested code (> 3 levels)
- Methods > 40 lines

## Example Usage

```text
User: /java-review

Agent:
# Java Code Review Report

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

[MEDIUM] Legacy Date usage
File: src/main/java/com/example/UserResource.java:25
Issue: Using java.util.Date.
Fix: Use java.time.OffsetDateTime or LocalDate.
```

## Approval Criteria

| Status     | Condition                               |
| ---------- | --------------------------------------- |
| ✅ Approve | No CRITICAL or HIGH issues              |
| ⚠️ Warning | Only MEDIUM issues (merge with caution) |
| ❌ Block   | CRITICAL or HIGH issues found           |

## Related

- Agent: `agents/java-reviewer.md`
- Skills: `java-coding-standards`, `jpa-patterns`, `springboot-patterns`
