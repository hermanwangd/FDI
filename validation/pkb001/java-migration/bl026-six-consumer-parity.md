# BL-026 Six-Consumer Python→Java Byte-Parity Gate

- Date: 2026-09-06
- Candidate: `target/fdi-0.4.8.3.jar` (integration HEAD `691f3b8`) vs the six transitional Python consumers.
- Method: identical inputs on separate fixture copies under `/tmp/bl026/`; stdout, stderr, exit code, and output-file bytes compared with `cmp`. All fixtures are synthetic copies (`/tmp`) — no tracked repository file was modified; the only repository write is this report.

## 1. `pkb001_gate.py` → `phase0-readiness-validate` — PARITY (5 cases)

Cases: empty evidence (BLOCKED, exit 2); full READY evidence with `--output reports/phase0.json` (exit 0); missing evidence file (ERROR, exit 1); symlink `--output` inside root; dangling symlink `--output` escaping root.

- Commands: `python3 tooling/validation/pkb001_gate.py --root <root> [--evidence <e>] [--output <o>]` vs `java -jar target/fdi-0.4.8.3.jar phase0-readiness-validate --root <root> ...`.
- stdout byte-parity: Y (all 5 cases). Output-file byte-parity: Y (`--output` file identical). Exit-code parity: Y (2/0/1/1/1).
- Disclosed limit verified: dangling symlink whose (nonexistent) target escapes the root → Python reports `output path must remain inside repository root`, Java reports `output path must not be a symlink`; both exit 1 fail-closed. A symlink that stays inside the root yields the identical `output path must not be a symlink` on both (also verified).

## 2. `pkb001_evaluate.py` → `blinded-evaluate` — PARITY (5 cases)

The Python module has no CLI (it is a library); parity was assessed at the report level via a thin driver (`/tmp/bl026/c2/driver.py`) that calls `evaluate()`/`build_decision_report()` and renders `json.dumps(report, indent=2) + "\n"` exactly like the Java CLI. The Java `--output` write has no Python counterpart (disclosed: this CLI surface is new).

Cases: 30 R3-arm proposals with mixed outcomes (CONTINUE, exit 0); empty inputs (REVISE, exit 2); hard failure `GROUND_TRUTH_ACCESS` (STOP, exit 2); duplicate proposals with least-favorable judgment collapse (`--minimum-proposals 1 --minimum-gold 1`); malformed proposal (ERROR, exit 1).

- stdout byte-parity: Y (including all Wilson-interval floats and `median_review_seconds`). Output-file byte-parity: Y (Java `--output` equals stdout bytes, matching the driver's render). Exit-code parity: Y (0/2/2/2/1).

## 3. `graphify_live_verifier.py` → `graphify-live-verify` — FAILURE-BEHAVIOR PARITY; live exercise NOT RUN

Live stdio-MCP exercise: NOT RUN — no reachable Graphify MCP server. Checked: no `.fdi-work/graphify-venv312` in this worktree, the sibling worktree, or the main checkout; no `graphify` on PATH; no `~/.graphify`; `config/graphify.example.yaml` carries no server command. npx/uv exist but installing a server would fabricate evidence, so per instructions it was not attempted.

Failure-behavior comparison instead (3 cases): existing root without runtime (NOT_BOUND, exit 2); missing root directory (NOT_BOUND, exit 2); usage error without `--output` (exit 2).

- stdout byte-parity: Y when both run against the same root (the only difference on separate roots is the embedded root path in `server_error` — by design). Output-file byte-parity: Y. Exit-code parity: Y (2/2/2).
- stderr: both print nothing on NOT_BOUND; usage-error stderr differs only in program name (`graphify_live_verifier.py` vs `graphify-live-verify`) — inherent CLI-rename artifact.

## 4. `pkb001_scenario_review.py` → `scenario-review-render` — PARITY (4 cases)

Cases: happy-path render of a valid proposal (exit 0; json+md+`.claim.json` compared); technical identifier in behavior (`TECHNICAL_IDENTIFIER_IN_BEHAVIOR`, exit 1); output collision on second run (`OUTPUT_ALREADY_EXISTS`, exit 1); unresolved `/tmp` vs `/private/tmp` proposal path mismatch (`OUTPUT_PATH_INVALID`, exit 1).

- stdout byte-parity: Y except embedded absolute output paths, which contain the per-implementation fixture root (`rootA` vs `rootB`) by design; content identical modulo that prefix. Output-file byte-parity: Y (review JSON, Markdown, and claim file byte-identical). Exit-code parity: Y (0/1/1/1). stderr byte-parity: Y on all blocked cases.
- Disclosed limits recorded (not exercised as divergences): FormatChecker date-time no-op, micros-domain `fromisoformat` port, 10,000-digit JSON number cap, create-then-chmod window, `--root` CLI default.

## 5. `build_pkb001_human_review_packet.py` → `human-review-packet-build` — PARITY (2 cases)

Cases: full packet build from copied committed inputs (exit 0, empty stdout on both); missing `sealed-blind-key.json` (exit 1, stack trace on stderr).

- stdout byte-parity: Y (both empty). Output-file byte-parity: Y (`human-review-decision-packet.json` and `HUMAN-REVIEW-DECISION-PACKET.md` byte-identical). Exit-code parity: Y (0/1).
- Note: on the error case stderr is a Python traceback vs a Java stack trace — different bytes by language, same fail-closed exit code, same failing path identified (`.../sealed-blind-key.json`). Disclosed `--root` default difference (script-location vs cwd) is documented in the Java CLI; all runs here passed `--root` explicitly.

## 6. `pkb001_task7_evaluate.py` → `task7-evaluate` — PARITY (2 cases)

Cases: full evaluation over the committed `validation/pkb001` inputs (exit 0; stdout, `--report`, `--pending` compared); corrupted forward-run witness in a copied root (STOP fail-closed, exit 2, identical `failure_stage`/`detail`).

- stdout byte-parity: Y. Output-file byte-parity: Y (report and pending packet byte-identical in both cases). Exit-code parity: Y (0/2).

## New divergences

None beyond the already-disclosed limits above. No code was changed and no divergence was silenced.

## Verdict

**PASS** — all six consumers show stdout/output-file/exit-code byte parity on every exercised case; the only byte differences observed are (a) the pre-disclosed dangling-symlink error message on consumer 1, (b) embedded per-fixture root paths in consumer 3/4 outputs (test-harness artifact, content identical), and (c) Python-traceback vs Java-stack-trace stderr text on consumer 5 error propagation. Consumer 3's live stdio-MCP exercise was not run (no reachable Graphify MCP server) and remains a disclosed coverage gap for an environment that has the frozen runtime.
