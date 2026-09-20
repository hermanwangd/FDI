# Supporting-v1 controlled cell protocol

This is one fresh, independent validation cell. Read the exact package path in the dispatch description and use only its allow-listed inputs. Do not read any other supporting-v1 cell, initial-v1 results, evaluator/gold files, hidden expected verdict, other arm, or future candidate.

Do not modify repository files, frozen scenario definitions, skills, controls, Product Knowledge, Product Context, runtime, product code, fixtures, gold, thresholds, or denominators. Use a disposable workspace for repository inspection. Do not publish, merge, or update a canonical candidate.

The output is a bounded reasoning/design result, not an implementation. Freeze the result in one issue delivery comment with these headings:

1. `### RESULT`
2. `### EVIDENCE USED` — exact input paths, revisions, and commands/readbacks actually used
3. `### LIMITATIONS` — unavailable MCPs, ambiguity, or unverified claims
4. `### CONTROLLED CELL META` — profile, arm, scenario revision, and input package

Do not include evaluator answers or invented facts. If the frozen input cannot establish a point, state the gap and request clarification rather than deciding silently. The controller will capture the delivery as `OUTPUT.md` and evaluate it only after the output is frozen.
