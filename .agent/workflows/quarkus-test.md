---
description: Test-driven development for Quarkus applications using REST Assured, InjectMock, and Testcontainers. Invokes the tdd-guide agent.
---

# Quarkus TDD Workflow

This command invokes the **tdd-guide** agent to implement new Quarkus features using a test-driven approach.

## What This Command Does

1. **Understand Specs**: Reads the feature description and identifies required behaviors.
2. **Setup Mocks**: Identifies suitable mocks for external dependencies (e.g., using `@InjectMock`).
3. **Write Failing Test (RED)**: Creates a new `@QuarkusTest` using REST Assured that fails.
4. **Implement minimal Code (GREEN)**: Simplest possible Java code to pass the test.
5. **Verify Solution**: Run `mvn test` or `./gradlew test` to ensure stability.
6. **Refactor (BLUE)**: Clear technical debt and optimize reactive streams.

## When to Use

Use `/quarkus-test` (or `@quarkus-test`) when:

- Starting a new Quarkus resource, service, or repository.
- Bug fixing (reproduce with a test first).
- Implementing complex business logic with Panache/Mutiny.
- Migrating from synchronous to reactive flows.

## TDD Principles

### RED - Fail First
- Create `@QuarkusTest` matching the requested feature.
- Use `RestAssured.given()` for resource-level tests.
- Verify the test fails due to missing implementation (usually 404 or compilation error).

### GREEN - Pass Fast
- Create the JAX-RS Resource, Service, and Entity.
- Use `PanacheRepository` or `PanacheEntity` for data access.
- Write minimal code to satisfy the test requirements.

### BLUE - Refactor
- Remove duplication and magic values.
- Optimize Mutiny pipelines (`onItem().transform()`, etc.).
- Ensure proper CDI scopes (`@ApplicationScoped`, `@RequestScoped`).

## Example Usage

```text
User: /quarkus-test "Add a GET /users/me endpoint that returns current user"

Agent:
# Quarkus TDD Plan

1. Create `UserResourceTest` using REST Assured.
2. Verify it fails (404 Not Found).
3. Implement `UserResource.getMe()` with Panache.
4. Refactor and ensure proper scopes.

[Executing Test Plan...]
```

## Related

- Agent: `agents/tdd-guide.md`, `agents/java-reviewer.md`
- Skills: `quarkus-tdd`, `quarkus-patterns`, `java-coding-standards`.
