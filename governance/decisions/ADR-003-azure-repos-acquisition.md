# ADR-003 — Azure Repos Acquisition

Azure Repos is supported as a source provider through exact read-only local Git acquisition. FDI binds full Git revisions after acquisition; provider URLs/branch names do not replace immutable source identity.
