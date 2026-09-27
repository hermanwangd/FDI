# S01 r3 chronology limitation

Downloaded r3 artifact digest: 57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba.
Provider delivery comment 01a0dd93-856f-70f1-b40e-e576ec4fc50f is timestamped 2026-09-26T11:57:07Z. Two semantics entries inside the immutable archive use updatedAt 2026-09-26T11:58:00Z, 53 seconds later. The payload timestamp must not be presented as the observed delivery/capture time. Preserve the original archive and use provider event timestamps and independently captured run receipts for chronology. This metadata discrepancy does not by itself contradict the pinned-source content or the explicitly separated conflict/verified slices, but it remains a reported limitation; it was not silently repaired in the reviewed archive.
