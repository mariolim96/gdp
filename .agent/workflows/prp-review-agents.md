---
description: Multi-agent coordinated review for high-stakes changes
---

# Multi-Agent Review

**Mission**: Coordinate multiple specialized agents to provide a multi-faceted critique of the code.

**Flow**:
1. **Assign**: Spawn specialized reviewer agents based on code changes (e.g., Security, Performance, UI/UX).
2. **Review**: Each agent performs a deep analysis of the PR based on its domain.
3. **Synthesis**: A lead agent summarizes all critiques into a single report.
4. **Conclusion**: Final recommendation (Approve, Request Changes, or Discuss).
