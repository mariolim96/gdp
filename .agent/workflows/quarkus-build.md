---
description: Build issue resolution and dev-mode initialization for Quarkus projects. Invokes the java-build-resolver agent.
---

# Quarkus Build & Initialize

This command invokes the **java-build-resolver** agent for deep analysis of project-level build, configuration, and native compilation issues.

## What This Command Does

1. **Inspect Maven/Gradle**: Analyzes `pom.xml` or `build.gradle` for missing dependencies.
2. **Quarkus Extensions Check**: Verifies if correct extensions are installed (e.g., `quarkus-hibernate-mutiny`).
3. **Analyze Error Logs**: Deep search of stack traces for specific ArC/JPA errors.
4. **Dev-Mode Troubleshooting**: Identifies why hot-reload might be failing.
5. **Native Build Issues**: Specialized analysis of GraalVM/Native image build failures.
6. **Apply and Verify Fix**: Changes configurations and confirms successful compilation.

## When to Use

Use `/quarkus-build` (or `@quarkus-build`) when:

- Your Quarkus project fails to compile or run.
- You have dependency conflicts (Enforcer/Dependency graph issues).
- Quarkus dev-mode (mvn quarkus:dev) is behaving unexpectedly.
- Dealing with tricky CDI `@Inject` (ArC) resolution errors.
- Native build/compilation is failing.

## Build Priorities

### Project Integrity
- Validating the Maven/Gradle structure.
- Checking for compatible versions of Quarkus vs Java (17/21).

### Extension Management
- Ensuring required extensions are present in `pom.xml`.
- Checking for duplicate or conflicting libraries.

### Configuration Analysis
- Reviewing `application.properties` and `application.yml` for correct format.
- Checking for missing required configuration properties.

### Native Readiness
- Analyzing reflections/serialization for GraalVM compatibility.
- Identifying non-native-friendly libraries.

## Example Usage

```text
User: /quarkus-build "My app fails to start with Ambassador/ArC errors"

Agent:
# Analyzing Build Errors

1. Searching pom.xml for conflicting dependencies.
2. Checking ArC bean discovery logs.
3. Found: Using @Inject on Ambiguous types without @Named.

Resolution: I will add @LookupIfProperty or @Default as appropriate.
[Applying changes...]
```

## Related

- Agent: `agents/java-build-resolver.md`
- Skills: `quarkus-verification`, `java-coding-standards`.
