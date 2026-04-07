# Java Test Coverage (JaCoCo)

Measure and analyze code coverage for Java Quarkus applications using JaCoCo.

## 🚀 Coverage Workflow

1. **Run Tests**: Execute the Maven verify goal to run tests and generate JaCoCo execution data.
   ```bash
   mvn clean verify
   ```
2. **Generate Report**: The JaCoCo report is usually generated in `target/site/jacoco/index.html`.
3. **Verify Compliance**: Check if the project meets the required coverage threshold (default 80%).

## 🛠️ When to Use

- Before merging a feature to ensure logic is tested.
- When identifying "blind spots" in the codebase.
- During CI/CD pipelines to enforce quality gates.

## 📦 Best Practices

- **80% Baseline**: Aim for at least 80% line coverage for all new code.
- **Critical Paths**: Ensure 100% coverage for security, financial, and core business logic.
- **Exclude Boilerplate**: Exclude generated code (like DTOs or Panache entities if they only contain getters/setters) from coverage reports via `pom.xml` configuration.

## 📈 Quality Gates

A pull request should only be approved if:
- ✅ Total coverage does not decrease.
- ✅ New files have > 80% coverage.
- ✅ No critical business logic is untested.

## 🛠️ Related
- **Agent**: `code-reviewer`
- **Workflows**: `/quarkus-test`, `/verify`

### Test Generation Rules

- Place tests adjacent to source: `foo.ts` → `foo.test.ts` (or project convention)
- Use existing test patterns from the project (import style, assertion library, mocking approach)
- Mock external dependencies (database, APIs, file system)
- Each test should be independent — no shared mutable state between tests
- Name tests descriptively: `test_create_user_with_duplicate_email_returns_409`

## Step 4: Verify

1. Run the full test suite — all tests must pass
2. Re-run coverage — verify improvement
3. If still below 80%, repeat Step 3 for remaining gaps

## Step 5: Report

Show before/after comparison:

```
Coverage Report
──────────────────────────────
File                   Before  After
src/services/auth.ts   45%     88%
src/utils/validation.ts 32%    82%
──────────────────────────────
Overall:               67%     84%  PASS:
```

## Focus Areas

- Functions with complex branching (high cyclomatic complexity)
- Error handlers and catch blocks
- Utility functions used across the codebase
- API endpoint handlers (request → response flow)
- Edge cases: null, undefined, empty string, empty array, zero, negative numbers
