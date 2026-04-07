---
description: Comprehensive PR code review - checks diff, patterns, runs validation, comments on PR
argument-hint: <pr-number|pr-url> [--approve|--request-changes]
---

# PR Code Review

**Process**:
1. **Context**: Fetch PR metadata and diff via `gh pr view` and `gh pr diff`.
2. **Deep Read**: Analyze every changed file in context of the whole codebase.
3. **Validation**: Run `npm run type-check`, `lint`, and `test` on the PR branch.
4. **Checklist**:
   - Type safety (explicit types, no `any`).
   - Pattern compliance (matching existing architectural styles).
   - Security (input validation, secret leakage).
   - Maintainability (readability, naming).
5. **Report**: Generate a detailed report in `.claude/PRPs/reviews/` and post a PR comment via `gh pr review`.
