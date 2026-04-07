---
paths:
paths:
  - '**/*.java'
  - '**/pom.xml'
  - '**/build.gradle'
  - '**/build.gradle.kts'
  - '**/application.properties'
  - '**/application.yml'
---

# Java Hooks

> This file extends [common/hooks.md](../common/hooks.md) with Java, Quarkus, and Spring Boot specific content.

## PostToolUse Hooks

Configure in `~/.claude/settings.json`:

- **Formatting**: Auto-format `.java` files after edit (using `google-java-format` or `Spotless`).
- **Style Overlays**: Run `checkstyle` after editing Java files.
- **Verification**: Run `./mvnw compile` or `./gradlew compileJava` to verify changes.

## Warnings & Automations

- **Constructor Injection**: Warn about field injection (`@Inject` or `@Autowired` on fields) — suggest constructor injection.
- **Validation**: Warn about missing `@Valid` on controller/resource parameters.
- **Logging**: Warn about usage of `System.out.println` — suggest using SLF4J or JBoss Logging.
- **Quarkus Configuration**: Trigger `mvn quarkus:dev` if `application.properties` or `application.yml` is modified.
- **Dependency Audit**: Suggest `mvn dependency:analyze` (Maven) or `./gradlew dependencies` (Gradle) if `pom.xml` or `build.gradle` is modified.
- **Native Image Check**: Suggest `mvn verify -Pnative` in Quarkus projects if core JAX-RS or Panache logic is heavily modified.

