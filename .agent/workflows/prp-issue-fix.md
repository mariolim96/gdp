---
description: Quick-fix workflow for well-understood, isolated bugs
argument-hint: <bug-description>
---

# Quick Issue Fix

**Input**: $ARGUMENTS

---

## Process

1. **Reproduction**: Create a failing test case that proves the bug.
2. **Investigation**: Locate the specific lines causing the failure.
3. **Draft Fix**: Apply the minimal change required to fix the bug.
4. **Validation**: Run the reproduction test again (should pass).
5. **Regression**: Run the full test suite to ensure no collateral damage.
6. **Cleanup**: Refactor the fix for clarity if necessary.

**Constraint**: If the fix requires architectural changes or touches more than 3 files, STOP and use `/prp-plan`.
