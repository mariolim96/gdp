---
description: Legacy slash-entry shim for the e2e-testing skill. Prefer the skill directly.
---

# Quarkus E2E Testing (Playwright)

End-to-end testing for Quarkus applications using Playwright, leveraging Quarkus Dev Services for seamless infrastructure management.

## 🚀 What This Workflow Does

1. **Setup Environment**: Leverages `mvn quarkus:dev` or `mvn test` to activate **Quarkus Dev Services** (automatic Docker containers for Postgres, Redis, etc.).
2. **Playwright Integration**: Connects Playwright tests to the running Quarkus dev instance.
3. **Resource Testing**: Focuses on JAX-RS (RestEasy) resources and their interaction with the database.
4. **Validation**: Verifies that the UI or REST API behaves correctly from the user's perspective.

## 🧪 When to Use

- When implementing a full user journey (e.g., "User creates a record and sees it in the list").
- When verifying integration between multiple Quarkus services.
- When validating front-end interactions with Quarkus backend.

## 🏗️ Example Playwright Test (JAX-RS)

```typescript
import { test, expect } from '@playwright/test';

test('create and retrieve user', async ({ page }) => {
  // Quarkus app usually runs on 8080 in dev
  await page.goto('http://localhost:8080/users');

  // Trigger creation
  await page.click('#create-user-btn');
  await page.fill('#user-name', 'Quarkus Engineer');
  await page.click('#submit-btn');

  // Verify result via JAX-RS endpoint or UI
  await expect(page.locator('#user-list')).toContainText('Quarkus Engineer');
});
```

## 📦 Best Practices

- **Dev Services**: Do not manually start databases. Let Quarkus manage them via Testcontainers/Dev Services.
- **Wait for Readiness**: Use `await page.waitForLoadState('networkidle')` to ensure the asynchronous Quarkus resource is fully loaded.
- **Clean Slate**: Use a fresh database state for each run (handled by Quarkus `@QuarkusTest` if using integration tests, or by Dev Services for full E2E).

## 🛠️ Related
- **Agent**: `e2e-runner`
- **Skill**: `e2e-testing`
- **Workflows**: `/quarkus-test`, `/verify`
 file lives at:
`agents/e2e-runner.md`

## Quick Commands

```bash
# Run all E2E tests
npx playwright test

# Run specific test file
npx playwright test tests/e2e/markets/search.spec.ts

# Run in headed mode (see browser)
npx playwright test --headed

# Debug test
npx playwright test --debug

# Generate test code
npx playwright codegen http://localhost:3000

# View report
npx playwright show-report
```
