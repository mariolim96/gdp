# Refactor Clean

Safely identify and remove dead code with test verification at every step.

## Step 1: Detect Dead Code

Run analysis tools based on project type:

| Tool | What It Finds | Command |
|------|--------------|---------|
| knip | Unused exports, files, dependencies | `npx knip` |
| depcheck | Unused npm dependencies | `npx depcheck` |
| ts-prune | Unused TypeScript exports | `npx ts-prune` |
| vulture | Unused Python code | `vulture src/` |
| deadcode | Unused Go code | `deadcode ./...` |
| cargo-udeps | Unused Rust dependencies | `cargo +nightly udeps` |

If no tool is available, use Grep to find exports with zero imports:
```
# Find exports, then check if they're imported anywhere
```

## Step 2: Categorize Findings
# Java Refactor and Clean

A specialized workflow for modernizing Java Quarkus codebases, focusing on Java 17+ standards and Quarkus best practices.

## 🚀 Refactor Workflow

1. **Modernize to Records**: Convert data-only classes (POJOs/DTOs) to **Java 17 Records**.
2. **Standardize CDI**: Remove field-level `@Inject` and convert to constructor-based injection.
3. **Reactive Migration**: Identify blocking calls and recommend `Uni` or `Multi` (Mutiny) conversions.
4. **Cleanup**:
   - Remove unused imports.
   - Swap legacy `java.util.Date` for `java.time.OffsetDateTime` or `LocalDateTime`.
   - Replace complex anonymous classes with Lambdas.
5. **Verify**: Run `mvn compile` and `mvn test` to ensure zero regressions.

## 🛠️ When to Use

- When onboarding to an older Java codebase.
- Before a major feature implementation to clear technical debt.
- During code reviews to enforce modern standards.

## 📦 Best Practices

- **Minimal Diffs**: Refactor in small, verifiable chunks.
- **Record Conversion**: Only use Records for immutable data; keep Entities as classes if JPA requires it.
- **JPA cleanup**: Ensure `@ManyToOne` and `@OneToMany` have proper fetch types (prefer `LAZY`).

## 🛠️ Related
- **Agent**: `code-simplifier`
- **Agent**: `java-reviewer`
- **Workflows**: `/java-review`, `/quarkus-review`
- **Skip if uncertain** — Better to keep dead code than break production
- **Don't refactor while cleaning** — Separate concerns (clean first, refactor later)
