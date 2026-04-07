---
description: Deep debugging and root cause analysis using 5 Whys and history analysis
argument-hint: <symptom-description> [--deep]
---

# Deep Debugging

**Input**: $ARGUMENTS

---

## High-Level Flow

1. **Symptom Definition**: Exactly what is happening?
2. **Reproduction**: Isolate the smallest possible case.
3. **History Analysis** (if --deep): When did this start? (`git bisect`)
4. **State Analysis**: Inspect variable states at each step.
5. **Root Cause**: Identify the fundamental flaw.
6. **Report**: Summarize findings and move to fix phase.

---

## The 5 Whys Worksheet

- **Why 1**: {Immediate Cause}
- **Why 2**: ...
- **Why 3**: ...
- **Why 4**: ...
- **Why 5**: {Systemic Root Cause}
