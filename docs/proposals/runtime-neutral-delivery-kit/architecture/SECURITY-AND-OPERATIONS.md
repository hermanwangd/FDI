# Security and operations profile

SO-01 identity: Authenticate Human/FDP/EP/service actors separately. Scope artifact
access by project and experiment arm; deny cross-arm context, workspace and
evaluator access. Use short-lived secret references resolved by the receiving
secret service. Never package raw credentials or put them in logs, prompts or
evidence. Validate grants before protected actions; revocation cancels admission
and triggers running-work reconciliation.

SO-02 supply chain: Verify trusted lock digest, every file hash and license record
before loading skills. Reject symlinks, traversal, absolute paths and unlisted
executable dependencies. Skills cannot authorize shell/network access: the
envelope/profile allowlist does. Retrieved text is data even when phrased as
instructions. New external destinations, installations and spending require
appropriate scoped authorization. This package has no executable skill scripts.

SO-03 evidence: Distinguish AGENT_CLAIM, TOOL_OBSERVATION and INDEPENDENT_CHECK.
Digest equality only proves unchanged bytes, not truthful production. Resolve
claimed runtime identities against trustworthy runtime records, bind input and
candidate digests, retain exit code/tool version and observation time, and
reproduce countable mandatory claims. Negative/missing evidence cannot be filled
with confidence scores. Missing evidence yields INCONCLUSIVE at T4.

SO-04 resources: Local verification target is less than 8 GB aggregate workload
memory, including descendants and native/JVM overhead. Propose a 6 GB process-group
admission ceiling with 2 GB headroom, one Maven job (`-Xmx2g`) at a time and one
specialist unless a measured profile admits more. Require OS/container enforcement
plus observation before claiming the limit is enforced. Java heap alone is not a
total bound. If the host cannot enforce the group cap, block heavy execution and
record PLAN_BLOCKED; lightweight document validation may proceed within measured
bounds. An envelope requiring parallelism cannot be silently serialized.

SO-05 initial non-production sizing assumptions: one project, at most 10 admitted
WorkItems, at most 100 immutable artifacts per envelope, 16 MiB per inline input;
larger artifacts use digest-bound references and streaming checks. Proposed local
targets: admission p95 ≤2 seconds excluding agent/model/network work, reconciliation
within 60 seconds of a restored runtime connection; no availability SLA is claimed.
Company owner must approve deployment load, availability, RTO/RPO and retention
before operational adoption; these are deployment-profile decisions, not guessed
runtime guarantees.

SO-06 observability: Correlate execution ID, plan/work-item digest, receipt, candidate,
review identity and evidence digest in restricted execution logs, not WorkItem or
STATUS runtime fields. Measure queue/attempt/reconciliation time, first review and
T4 result, retries/replans, verified output counts, tokens/cost with provenance.
Missing token data is unavailable, not zero. Alert immediately on scope breach,
authority conflict, isolation or digest failure; alert on deadline expiry and
stalled reconciliation after 60 seconds. Keep ordinary unchanged progress quiet.

SO-07 retention: Proposal pilot default is 30 days of redacted operational logs;
immutable gate evidence persists through its associated review/retention policy.
Do not delete source PKB validation history. Company data owner must choose
approved evidence retention, deletion/legal hold and secret-redaction rules
before sensitive data ingestion. Test these on synthetic fixtures first.

SO-08 recovery: Stop admission on integrity/authority failures, fence writers,
capture the last trustworthy receipt and evidence refs, reconcile remote effects,
then resume exact contracts or request replan. Backup controls/evidence separately
from runtime indexes. Acceptance requires restoring to an isolated environment and
proving no lost acknowledged result and no duplicate mutation. Roll out one
synthetic workflow, then one approved pilot. Roll back the binding version after
quiescence; preserve all immutable evidence and never reset a running plan by
deleting its runtime records.
