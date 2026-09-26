# Supervisor Shift-Left Diagnosis Contract v0.1

**Owner:** Claude Supervisor  
**Purpose:** Convert observed execution problems into evidence-backed reusable prevention without over-generalization.

## Core Rule

> Trace beyond the visible symptom to the earliest actionable causal cause supported by evidence, then recommend the smallest reusable correction that materially prevents recurrence.

## Stop Rule

Stop upstream tracing when any of the following is true:

- evidence is insufficient to support the next causal step;
- the upstream factor is not practically actionable;
- moving farther upstream does not materially improve recurrence prevention;
- the proposed generalization would increase complexity/blast radius without sufficient evidence.

## Output

```yaml
observedFailure:
immediateCause:
causalChain:
earliestActionableCause:
owningScope:
owningComponent:
failureMode:
evidenceRefs:
confidence:

improvement:
  localCorrection:
  reusablePrevention:
  improvementDirection:
  proposal:
  successCriteria:
```

## Anti-Patterns

- symptom → patch
- failure at Control → assume Control owns root cause
- failure at runtime → assume Core defect
- one case → shared architecture change
- unlimited Five-Whys without evidence/actionability
- shift-left used to weaken critical gates
