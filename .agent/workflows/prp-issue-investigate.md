---
description: Investigative root cause analysis and impact assessment for complex issues
argument-hint: <issue-description|issue-url> [--deep]
---

# Issue Investigation

**Input**: $ARGUMENTS

---

## Phase 1: REPRODUCTION - Prove the Issue

### 1.1 Gather Evidence
- Read the issue description carefully.
- Identify the reported behavior vs. expected behavior.
- Look for logs, stack traces, or screenshots in the report.

### 1.2 Create Minimal Reproduction
- Write a reproduction script or narrowing test case.
- **Crucial**: The test MUST fail with the reported issue.
- Document the exact steps/commands to reproduce.

**PHASE_1_CHECKPOINT:**
- [ ] Issue clearly understood
- [ ] Reproduction case created
- [ ] Failure confirmed

---

## Phase 2: ANALYSIS - Find the Root Cause (5 Whys)

### 2.1 Trace the Flow
- Follow the code execution from the entry point to the failure.
- Use debugger or targeted logging.
- Identify the exact line/condition where state deviates from expected.

### 2.2 The 5 Whys
1. Why did the failure happen? (Immediate cause)
2. Why did THAT happen?
3. ... (Repeat until systemic cause found)

### 2.3 Impact Assessment
- What other components are affected?
- Is there potential for data corruption?
- Is this a regression? (Check git history)

**PHASE_2_CHECKPOINT:**
- [ ] Root cause identified at the code level
- [ ] Systemic cause understood
- [ ] Impact scope defined

---

## Phase 3: SOLUTIONING - Propose Fixes

### 3.1 Evaluate Options
- Short-term fix vs. long-term architectural solution.
- Trade-offs: complexity, performance, maintainability.

### 3.2 Strategy Selection
- Choose the best path forward.
- Define a high-level fix strategy.

**PHASE_3_CHECKPOINT:**
- [ ] Multiple solutions considered
- [ ] Best strategy selected
- [ ] Next steps defined (usually move to `/prp-plan` or `/prp-debug`)
