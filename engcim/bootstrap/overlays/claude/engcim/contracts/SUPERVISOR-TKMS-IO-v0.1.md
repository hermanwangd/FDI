# Supervisor tKMS I/O Contract v0.1

Claude Supervisor owns direct tKMS I/O.

Supported knowledge domains:
- Platform Knowledge
- Product Knowledge, subject to Product Knowledge governance

Rules:
- ENGCIM Swarm MUST NOT directly read/write tKMS.
- Per-Mission automatic publication is not required.
- Product Knowledge write does not bypass Product authority/governance.
- If tKMS access is unavailable, report the operation unavailable/blocked; do not invent an API.
- tKMS is not a mandatory intermediary for WorkspaceKnowledge or Swarm Dev Team engineering changes.
