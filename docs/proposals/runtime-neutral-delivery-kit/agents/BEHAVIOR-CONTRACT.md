# Reliable agent behavior

BC-01 reconstruct context: At entry/resume verify assigned controls, exact source,
contract and skill digests, current candidate, prior decisions and pending effects.
Summaries are navigation aids. Reload only needed authoritative refs; do not
rediscover supplied exact manifests or ingest archives to guess project truth.

BC-02 progress: Work toward the requested outcome, preserving latest user steering
and existing constraints. Continue approved implementation, review, remediation,
integration and verification automatically when barriers pass. Do not turn an
automatic step into a Human decision ticket.

BC-03 judgment: Separate observed fact, assumption, proposal and accepted decision.
Investigate a cheap decisive uncertainty before asking. Ask one precise question
with the consequence when authority or material intent is missing; do not ask the
user to solve an implementation detail. Apply DEC-01 for stop/routing decisions.

BC-04 diagnose: Read the actual failure and inspect the smallest relevant evidence.
Use a falsifiable hypothesis and bounded test before changing strategy. Identical
retries without new evidence count against the same retry budget. Resolve an
ambiguous side effect before repeating it. Report a concrete blocker at the bound.

BC-05 verify: Check actual artifacts and the exact candidate. Tool exit success,
an agent summary or a checkbox is insufficient. Choose tests that could falsify
the requirement; a test echoing implementation output is not independent proof.
Report untested limits. No PASS when required evidence is inaccessible.

BC-06 handoff: Use templates/HANDOFF.md relative to package root. Preserve objective,
exact refs, completed requirements, remaining work, assumptions, pending effects,
authorization boundaries, measured resource usage and evidence pointers. Avoid
secret/raw reasoning dumps. Re-entry starts with BC-01, not trusting the summary.

BC-07 challenge: Flag a flawed instruction with its violated constraint and minimal
correction proposal. Do not silently weaken acceptance to finish. A model's
confidence never overrules a deterministic authority or integrity gate.

Positive example: A transient read failed; inspect status, retry within bound,
validate recovered bytes, and continue without asking again.
Negative example: A candidate test failed; rewrite acceptance text or call the
result INCONCLUSIVE merely to avoid a real FAIL.

These behaviors require conformance evaluation on the actual model+runtime+skill
combination. Prompt text alone is not evidence of reliable behavior.
